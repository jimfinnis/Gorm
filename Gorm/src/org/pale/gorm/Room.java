package org.pale.gorm;

import java.util.Random;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

/**
 * A room or corridor in the castle
 * 
 * @author white
 * 
 */
public class Room {
	public enum RoomType {
		HALL, CORRIDOR, ROOM, GARDEN
	}

	/**
	 * The basic 'type' of the room, providing hints for connections to other
	 * rooms
	 */
	private RoomType roomType;

	/**
	 * The extent of the entire room
	 */
	Extent extent;

	/**
	 * How much of the top of the room is 'roof'
	 */
	int roofDepth = 1;

	/**
	 * Construct the room - does not render it, because it might get moved
	 * around
	 * 
	 * @param e
	 */
	public Room(RoomType t, Extent e) {
		extent = new Extent(e);
		roomType = t;
	}

	/**
	 * Return the bounding box of the room, including the walls
	 * 
	 * @return
	 */
	public Extent getExtent() {
		return extent;
	}

	/**
	 * Return the 'type' of the room
	 * 
	 * @return
	 */
	public RoomType getType() {
		return roomType;
	}

	/**
	 * Draw into the world
	 */
	void render() {
		Castle c = Castle.getInstance();

		// basic room for now
		c.fillBrickWithCracksAndMoss(extent);
		c.fill(extent.expand(-1, Extent.ALL), Material.AIR, 0);

		lightWalls();
		floorLights();
		underfill();
		makeExit(true);
		makeExit(false);
		makeExit(true);
		makeExit(false);
		makeExit(true);
		makeExit(false);
	}

	void lightWalls() {
		// first some wall lights
		int y = extent.miny + 5;
		if (y >= extent.maxy)
			y = extent.maxy - 1;
		for (int x = extent.minx + 1; x < extent.maxx; x++) {
			if (requiresLight(x, y, extent.minz + 1))
				addLight(x, y, extent.minz + 1);
			if (requiresLight(x, y, extent.maxz - 1))
				addLight(x, y, extent.maxz - 1);
		}
		for (int z = extent.minz + 1; z < extent.maxz; z++) {
			if (requiresLight(extent.minx + 1, y, z))
				addLight(extent.minx + 1, y, z);
			if (requiresLight(extent.maxx - 1, y, z))
				addLight(extent.maxx - 1, y, z);
		}
	}

	void floorLights() {
		World w = Castle.getInstance().getWorld();
		int y = extent.miny + 2;

		for (int x = extent.minx + 3; x < extent.maxx - 3; x += 5) {
			for (int z = extent.minz + 3; z < extent.maxz - 3; z += 5) {
				Block b = w.getBlockAt(x, y, z);
				if (b.getLightLevel() < 7) {
					b.setType(Material.FENCE);
					b = w.getBlockAt(x, y - 1, z);
					b.setType(Material.FENCE);
					b = w.getBlockAt(x, y + 1, z);
					b.setType(Material.TORCH);
				}
			}
		}
	}

	/**
	 * Fill voids under the room in some way
	 */
	void underfill() {
		World w = Castle.getInstance().getWorld();
		for (int x = extent.minx; x <= extent.maxx; x++) {
			for (int z = extent.minz; z <= extent.maxz; z++) {
				for (int y = extent.miny - 1;; y--) {
					Block b = w.getBlockAt(x, y, z);
					if (b.isEmpty()) {
						b.setType(Material.COBBLESTONE);
					} else {
						break;
					}
				}
			}
		}

	}

	void addLight(int x, int y, int z) {
		Block b = Castle.getInstance().getWorld().getBlockAt(x, y, z);
		b.setType(Material.TORCH);
	}

	boolean requiresLight(int x, int y, int z) {
		Block b = Castle.getInstance().getWorld().getBlockAt(x, y, z);
		return b.getLightLevel() < 7;
	}

	public boolean contains(IntVector v) {
		return extent.contains(v);
	}

	boolean intersects(Extent e) {
		return extent.intersects(e);
	}

	public void setExtent(Extent e) {
		extent = new Extent(e);

	}

	/** Try to create an exit from this room into another one (if outside is false) or to the outside 
	 * (if it is true) Does not create vertical exits!
	 */
	public void makeExit(boolean outside) {
		Castle c = Castle.getInstance();
		Random rnd = new Random();
	
// SNARK only trying exits in the orientations which fail.
//		IntVector.Direction[] dirs = {IntVector.Direction.NORTH,IntVector.Direction.SOUTH,
//				IntVector.Direction.EAST, IntVector.Direction.WEST};
		IntVector.Direction[] dirs = {IntVector.Direction.NORTH,IntVector.Direction.SOUTH};
				//IntVector.Direction.EAST, IntVector.Direction.WEST};
		
		Extent innerSpace = extent.expand(-1, Extent.ALL);
		
		for(int tries=0;tries<20;tries++){  
			GormPlugin.log("try  "+Integer.toString(tries));
			// pick a wall
			IntVector.Direction dir = dirs[rnd.nextInt(dirs.length)];
			Extent wall = extent.getWall(dir);
			// find the centre of the wall, next to the floor
			IntVector wallCentre = wall.getCentre();
			wallCentre.y = innerSpace.miny;
			// pick which side of the centre we're going to walk down and
			//  get a vector 

			IntVector wallNormal = dir.vec.scale(2); // multiply by 2 so that the 'shadows' of the exit are shifted out enough
			GormPlugin.log("wall direction="+dir.toString()+", wall vector="+wallNormal.toString());
			IntVector walkVector = wallNormal.rotate(rnd.nextInt(2)*2+1); // 1 turn or 3
			// make the exit, actually embedded in the centre of the wall 
			Extent baseExit = new Extent(dir,rnd.nextInt(3)+1,rnd.nextInt(2)+3);
			baseExit = baseExit.addvec(wallCentre);
			// check along the wall until it runs out
			for(int i=0;i<100;i++){
				// get a vector to put the exit at
				IntVector v = walkVector.scale(i);
				// make the exit, at that position
				Extent exit = baseExit.addvec(v);
				GormPlugin.log(" testing exit at "+exit.toString());
				// make sure it's not near an exit already
				if(c.isNearExistingExit(exit)){
					GormPlugin.log("  aborting, is near exit");
					continue;
				}
				// find the exit's 'shadow' on this side of the wall
				Extent e1 = exit.subvec(wallNormal);
				// if that's NOT entirely inside the inner extent, we've run out of wall
				if(!innerSpace.inside(e1)){
					GormPlugin.log("  aborting, run out of wall");	
					break;
				}
				// and get the shadow on the far side
				Extent e2 = exit.addvec(wallNormal);
				if(testExitDestination(outside,e2)){
					// if it's good, use it
					c.fill(exit, Material.AIR, 0);
					c.addExit(exit);
					e2 = new Extent(exit);
					e2.miny-=5;
					e2.maxy-=2;
					c.postProcessExit(e2);
					return;
				}
			}
			
		}
		
	}

	private boolean testExitDestination(boolean outside, Extent e) {
		Castle c = Castle.getInstance();
		boolean isInRoom = c.intersects(e);
		if(outside == isInRoom) // look at the truth table :)
			return false;
		return !e.intersectsWorld(); // if it passed that, it'll be OK provided it doesn't intersect the world
	}

}
