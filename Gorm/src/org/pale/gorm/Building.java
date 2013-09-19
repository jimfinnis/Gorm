package org.pale.gorm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.pale.gorm.roomutils.PitchedRoofBuilder;

/**
 * A building in the castle, consisting of rooms
 * 
 * @author white
 * 
 */
public class Building {
	public enum BuildingType {
		HALL, CORRIDOR, ROOM, GARDEN
	}

	public class Room {
		Extent e;
		Building b;

		/**
		 * This is a map of exits by the room they go to
		 */
		Map<Room, Exit> exitMap = new HashMap<Room, Exit>();

		/**
		 * Our exits
		 */
		Collection<Exit> exits = new ArrayList<Exit>();

		Room(Extent e, Building b) {
			this.b = b;
			this.e = new Extent(e);
		}
	}

	private static int idCounter = 0;
	private int id = idCounter++;

	/**
	 * List of the rooms - vertical sections within the building
	 */
	private List<Room> rooms = new ArrayList<Room>();

	/**
	 * The basic 'type' of the building
	 * 
	 */
	private BuildingType building;

	/**
	 * The extent of the entire building
	 */
	Extent extent;

	/**
	 * How much of the top of the building is 'roof'
	 */
	int roofDepth = 1;

	/**
	 * Construct the building - does not render it, because it might get moved
	 * around
	 * 
	 * @param e
	 */
	public Building(BuildingType t, Extent e) {
		extent = new Extent(e);
		building = t;
	}

	/**
	 * Return the bounding box of the building, including the walls
	 * 
	 * @return
	 */
	public Extent getExtent() {
		return extent;
	}
	
	/** The standard operation for single-room buildings
	 *
	 */
	public void makeSingleRoom(){
		rooms = new ArrayList<Room>(); // make sure any old ones are gone
		rooms.add(new Room(extent,this));
	}

	/**
	 * Return the 'type' of the building
	 * 
	 * @return
	 */
	public BuildingType getType() {
		return building;
	}

	/**
	 * Draw into the world - call this *before* adding the building!
	 */
	void render() {
		Castle c = Castle.getInstance();

		// basic building for now
		c.fillBrickWithCracksAndMoss(extent, true);

		c.fill(extent.expand(-1, Extent.ALL), Material.AIR, 1); // 'inside' air

		underfill();

		makeRooms();

		// is the bit above the building free?
		Extent e = new Extent(extent);
		e.miny = e.maxy;
		if (e.xsize() > e.zsize())
			e.setHeight(e.xsize() / 2);
		else
			e.setHeight(e.zsize() / 2);
		if (c.intersects(e)) {
			// only building for a small roof
		} else {
			// room for a bigger roof
			new PitchedRoofBuilder().buildRoof(extent);
		}
	}

	/**
	 * Attempt to build a number of internal floors in tall buildings,
	 */
	private void makeRooms() {
		Castle c = Castle.getInstance();

		// start placing floors until we run out
		for (int h = extent.miny + 1; h < extent.maxy - 4;) {
			int nexth = h + c.r.nextInt(3) + 4;
			placeFloorAt(h, nexth);

			h = nexth + 2; // because h and nexth delineate the internal space -
							// the air space - of the building
		}
	}

	/**
	 * Place a floor at height h above the building base. Floors
	 * are one deep.
	 * 
	 * @param yAboveFloor
	 *            Y of the block just above the floor
	 * @param yBelowCeiling
	 *            Y of the block just below the ceiling
	 */
	private void placeFloorAt(int yAboveFloor, int yBelowCeiling) {
		Castle c = Castle.getInstance();

		// work out the extent of this room
		Extent roomExt = new Extent(extent);
		roomExt.miny = yAboveFloor - 1;
		roomExt.maxy = yBelowCeiling + 1;

		// make the actual floor - first layer
		Extent floor = extent.expand(-1, Extent.X | Extent.Z);
		floor.miny = yAboveFloor - 1;
		floor.maxy = floor.miny;
		c.fillBrickWithCracksAndMoss(floor, false);

		// now make floor match the inner space of the building
		floor.miny = yAboveFloor;
		floor.maxy = yBelowCeiling;

		carpet(floor, c.r.nextInt(14));
		lightWalls(floor);
		floorLights(floor);

		Room r = new Room(roomExt, this);
		rooms.add(r);

	}

	/**
	 * Add carpeting to this extent - this is the interior space of the
	 * building; the air space.
	 * 
	 * @param floor
	 */
	private void carpet(Extent floor, int col) {
		Castle c = Castle.getInstance();
		floor = new Extent(floor);
		floor.maxy = floor.miny;
		c.fill(floor, Material.CARPET, col);
	}

	/**
	 * Lights around the walls if we need them
	 * 
	 * @param e
	 *            the internal air space of the building
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
	 * 
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
	 * Fill voids under the building in some way
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
	 * Tell this building to make a random exit to an adjacent building - this
	 * is for horizontal exits
	 */
	public boolean makeRandomExit() {
		Castle c = Castle.getInstance();

		GormPlugin.log("Attempting to make an exit from building "
				+ Integer.toString(id));

		// first we need to find a suitable nearby building. This version of the
		// code will only
		// link with buildings with colinear walls.

		List<Building> buildings = new ArrayList<Building>();
		for (Building r : c.getBuildings()) {
			if (r != this) {
				GormPlugin.log("  Checking building " + Integer.toString(r.id)
						+ " for validity");
				if (extent.intersects(r.extent)) {

					Extent e = extent.intersect(r.extent); // get the
															// intersecting
															// wall
					if (e.xsize() != 1 && e.zsize() != 1) {
						throw new RuntimeException(
								"two buildings intersect >1 deep");
					}

					// check it intersects enough
					if (Math.max(e.xsize(), e.zsize()) >= 5) {
						buildings.add(r);
					} else
						GormPlugin
								.log("     building intersection is too small");
				} else
					GormPlugin.log("     buildings do not intersect");
			}
		}

		if (buildings.size() != 0) {
			Building b = buildings.get(c.r.nextInt(buildings.size()));
			GormPlugin.log(String.format("Trying to make an exit between %d and %d!!!",
					id,b.id));
			return makeRandomExit(b);
		}
		return false;
	}
	
	/**
	 *  Having found a building nearby, we need to find a pair of candidate rooms, one here
	 *  and one over there.
	 */
	private boolean makeRandomExit(Building other){
		// we don't want to always do one floor.
		Collections.shuffle(rooms);
		
		for(Room r: rooms){
			for(Room r2: other.rooms){
				if(Math.abs(r2.e.miny - r.e.miny) < 4){
					// that'll do!
					if(r2.e.miny<r.e.miny) // destination is lower than src. 
						return makeExitBetweenRooms(r,r2);
					else
						return makeExitBetweenRooms(r2,r);
					
				} 
			}
		}
		
		GormPlugin.log("Make exit failed; level mismatch");
		return false;
	}
	
	

	/**
	 * This is used to make a random horizontal exit to a building we already
	 * know intersects enough with us
	 * 
	 * @param r
	 */
	private boolean makeExitBetweenRooms(Room thisRoom, Room remoteRoom) {
		Extent intersection = thisRoom.e.intersect(remoteRoom.e);
		Direction dir;
		
		if(thisRoom.exitMap.containsKey(remoteRoom)){
			GormPlugin.log(String.format("exit already exists between %d and %d",thisRoom.b.id,remoteRoom.b.id));
			return false;
		}
			
		// work out the orientation of the exit
		if (intersection.xsize() > intersection.zsize()) {
			dir = remoteRoom.e.minz == thisRoom.e.maxz ? Direction.SOUTH
					: Direction.NORTH;
		} else {
			dir = remoteRoom.e.minx == thisRoom.e.maxx ? Direction.EAST
					: Direction.WEST;
		}

		int height = 3;

		// create the actual hole
		IntVector centreOfIntersection = intersection.getCentre();
		Extent e = new Extent(centreOfIntersection.x, thisRoom.e.miny + 1,
				centreOfIntersection.z, centreOfIntersection.x, thisRoom.e.miny
						+ height, centreOfIntersection.z);

		// create the two exit structures
		Exit src = new Exit(e, dir, thisRoom, remoteRoom);
		Exit dest = new Exit(e, dir.opposite(), remoteRoom, thisRoom);
		thisRoom.exits.add(src);
		thisRoom.exitMap.put(remoteRoom, src); // exit 'src' leads to room 'r'
		remoteRoom.exits.add(dest);
		remoteRoom.exitMap.put(thisRoom, dest); // exit 'dest' leads back here
		// blow the hole
		Castle.getInstance().fill(src.getExtent(), Material.AIR, 1);
		GormPlugin.log("hole blown: "+src.getExtent().toString());
		Castle.getInstance().postProcessExit(src);
		return true;
	}
}
