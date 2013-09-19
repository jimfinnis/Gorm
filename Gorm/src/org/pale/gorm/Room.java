package org.pale.gorm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.pale.gorm.roomutils.PitchedRoofBuilder;

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
	 * Our exits
	 */
	private Collection<Exit> exits = new ArrayList<Exit>();

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
	 * Return the list of exits
	 */
	public Collection<Exit> getExits() {
		return exits;
	}

	/**
	 * Draw into the world - call this *before* adding the room!
	 */
	void render() {
		Castle c = Castle.getInstance();

		// basic room for now
		c.fillBrickWithCracksAndMoss(extent, true);

		c.fill(extent.expand(-1, Extent.ALL), Material.AIR, 1); // 'inside' air

		underfill();

		internalFloorsAndExits();

		// is the bit above the room free?
		Extent e = new Extent(extent);
		e.miny = e.maxy;
		if (e.xsize() > e.zsize())
			e.setHeight(e.xsize() / 2);
		else
			e.setHeight(e.zsize() / 2);
		if (c.intersects(e)) {
			// only room for a small roof
		} else {
			// room for a bigger roof
			new PitchedRoofBuilder().buildRoof(extent);
		}
	}

	/**
	 * Attempt to build a number of internal floors in tall buildings, and exits
	 */
	private void internalFloorsAndExits() {
		Castle c = Castle.getInstance();

		// start placing floors until we run out
		for (int h = extent.miny+1; h < extent.maxy-4;){
			int nexth = h + c.r.nextInt(3) + 4;
			placeFloorAt(h,nexth);
			
			h=nexth+2; // because h and nexth delineate the internal space - the air space - of the room
		}
	}

	/**
	 * Place a floor and some exits at height h above the building base. Floors are one deep.
	 * 
	 * @param yAboveFloor		Y of the block just above the floor
	 * @param yBelowCeiling		Y of the block just below the ceiling
	 */
	private void placeFloorAt(int yAboveFloor,int yBelowCeiling) {
		Castle c = Castle.getInstance();

		// make the actual floor - first layer
		Extent floor = extent.expand(-1, Extent.X | Extent.Z);
		floor.miny = yAboveFloor-1;
		floor.maxy = floor.miny;
		c.fillBrickWithCracksAndMoss(floor, false);

		
		// now make floor match the inner space of the room
		floor.miny = yAboveFloor;
		floor.maxy = yBelowCeiling;
		
		carpet(floor,c.r.nextInt(14));
		lightWalls(floor);
		floorLights(floor);

		makeExit(yAboveFloor, true);
		makeExit(yAboveFloor, false);
		makeExit(yAboveFloor, true);
		makeExit(yAboveFloor, false);

	}

	/**
	 * Add carpeting to this extent - this is the interior space of the room; the air space.
	 * @param floor
	 */
	private void carpet(Extent floor, int col) {
		Castle c = Castle.getInstance();
		floor = new Extent(floor);
		floor.maxy = floor.miny;
		c.fill(floor,Material.CARPET,col);
	}

	/**
	 * Lights around the walls if we need them
	 * @param e the internal air space of the room
	 */
	void lightWalls(Extent e) {
		int y = e.miny + 4;
		if (y > e.maxy)
			y = e.maxy;
		for (int x = e.minx; x <= e.maxx; x++) {
			if (requiresLight(x, y, e.minz))
				addLight(x, y, e.minz);
			if (requiresLight(x, y, e.maxz))
				addLight(x, y, e.maxz);
		}
		for (int z = e.minz; z <= e.maxz; z++) {
			if (requiresLight(e.minx, y, z))
				addLight(e.minx, y, z);
			if (requiresLight(e.maxx, y, z))
				addLight(e.maxx, y, z);
		}
	}

	/**
	 * Lights on the floor if needed
	 * @param e 
	 */
	void floorLights(Extent e) {
		World w = Castle.getInstance().getWorld();
		int y = e.miny;

		for (int x = e.minx + 2; x < e.maxx - 2; x += 5) {
			for (int z = e.minz + 2; z < e.maxz - 2; z += 5) {
				Block b = w.getBlockAt(x, y, z);
				if (b.getLightLevel() < 7) {
					b.setData((byte) 0);
					b.setType(Material.FENCE);
					b = w.getBlockAt(x, y + 1, z);
					b.setData((byte) 0);
					b.setType(Material.FENCE);
					b = w.getBlockAt(x, y + 2, z);
					b.setData((byte) 0);
					b.setType(Material.TORCH);
				}
			}
		}
	}

	/**
	 * Fill voids under the room in some way
	 */
	void underfill() {
		Castle c = Castle.getInstance();
		World w = c.getWorld();

		int dx = extent.xsize() < 8 ? extent.xsize() - 1 : 4;
		int dz = extent.zsize() < 8 ? extent.zsize() - 1 : 4;

		for (int x = extent.minx; x <= extent.maxx; x += dx) {
			for (int z = extent.minz; z <= extent.maxz; z += dz) {
				for (int y = extent.miny - 1;; y--) {
					Block b = w.getBlockAt(x, y, z);
					if (b.isEmpty() || b.getType() == Material.CARPET) {
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
		b.setData((byte) 0);
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

	/**
	 * Try to create an exit from this room into another one (if outside is
	 * false) or to the outside (if it is true) Does not create vertical exits!
	 * 
	 * @param y  Y of base of exit
	 */
	public void makeExit(int y, boolean outside) {
		Castle c = Castle.getInstance();
		Random rnd = new Random();

		Direction[] dirs = { Direction.NORTH,
				Direction.SOUTH, Direction.EAST,
				Direction.WEST };

		// this is the 'inside' of the room.
		Extent innerSpace = extent.expand(-1, Extent.ALL);

		for (int tries = 0; tries < 20; tries++) {
			GormPlugin.log("try  " + Integer.toString(tries));
			// pick a wall
			Direction dir = dirs[rnd.nextInt(dirs.length)];
			Extent wall = extent.getWall(dir);

			// find the centre of the wall, next to the floor
			IntVector wallCentre = wall.getCentre();
			wallCentre.y = y;
			
			// pick which side of the centre we're going to walk down and
			// get a vector

			IntVector wallNormal = dir.vec.scale(2); // multiply by 2 so that
														// the 'shadows' of the
														// exit are shifted out
														// enough
			GormPlugin.log("wall direction=" + dir.toString()
					+ ", wall vector=" + wallNormal.toString());
			IntVector walkVector = wallNormal.rotate(rnd.nextInt(2) * 2 + 1); // 1
																				// turn
																				// or
																				// 3
			// make the exit, actually embedded in the centre of the wall
			
			int width = rnd.nextFloat()<0.2 ? 2:1;
			int height = rnd.nextFloat()<0.3 ? 3:2;
			Extent baseExtent = new Extent(dir, width, height, 0);
			
			baseExtent = baseExtent.addvec(wallCentre);
			// check along the wall until it runs out
			for (int i = 0; i < 100; i++) {
				// get a vector to put the exit at
				IntVector v = walkVector.scale(i);
				// make the exit, at that position
				Extent exitExtent = baseExtent.addvec(v);
				GormPlugin.log(" testing exit at " + exitExtent.toString());
				// make sure it's not near an exit already
				Extent tempBig = exitExtent.expand(3, Extent.ALL);
				if (overlapsExit(tempBig) || c.overlapsExit(tempBig)) {
					GormPlugin.log("  aborting, is near exit");
					continue;
				}

				// find the exit's 'shadow' on this side of the wall
				Extent e1 = exitExtent.subvec(wallNormal);
				// if that's NOT entirely inside the inner extent, we've run out
				// of wall
				if (!innerSpace.inside(e1)) {
					GormPlugin.log("  aborting, run out of wall");
					break;
				}
				// and get the shadow on the far side
				Extent e2 = exitExtent.addvec(wallNormal);
				if (testExitDestination(outside, e2)) {
					// if it's good, use it
					c.fill(exitExtent, Material.AIR, 1); // special 'inside' air
					Exit exit = new Exit(exitExtent,wallNormal.toDirection());
					exits.add(exit);
					// this will attempt to build stairs down, decorate etc.
					c.postProcessExit(exit);
					c.decorateExit(exit);
					c.postProcessExitsForRoomsIntersecting(extent.expand(10, Extent.ALL));
					return;
				}
			}
		}
	}

	public boolean overlapsExit(Extent x) {
		for (Exit e : getExits()) {
			if (x.intersects(e.getExtent()))
				return true;
		}
		return false;
	}
	

	private boolean testExitDestination(boolean outside, Extent e) {
		Castle c = Castle.getInstance();
		boolean isInRoom = c.intersects(e);
		if (outside == isInRoom) // look at the truth table :)
			return false;
		return !e.intersectsWorld(); // if it passed that, it'll be OK provided
										// it doesn't intersect the world
	}

}
