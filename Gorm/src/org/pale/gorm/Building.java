package org.pale.gorm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.material.Ladder;
import org.pale.gorm.rooms.BlankRoom;
import org.pale.gorm.rooms.PlainRoom;
import org.pale.gorm.roomutils.WindowMaker;

/**
 * A building in the castle, consisting of rooms
 * 
 * @author white
 * 
 */
public abstract class Building {
	/**
	 * Given a parent room and a size, produce an extent for a room which can be
	 * slid around by the Builder.
	 * 
	 * @param parent
	 * @param e
	 */
	public void setInitialExtent(Building parent, int x, int y, int z) {
		Extent e = new Extent(parent.getExtent().getCentre(), x, 1, z); // y
																		// unused
		e.miny = parent.getExtent().miny; // make floors align
		extent = e.setHeight(y);

	}

	private static int idCounter = 0;
	private int id = idCounter++;

	/**
	 * List of the rooms - vertical sections within the building
	 */
	protected LinkedList<Room> rooms = new LinkedList<Room>();

	/**
	 * The extent of the entire building
	 */
	Extent extent;

	/**
	 * How much of the top of the building is 'roof'
	 */
	int roofDepth = 1;

	/**
	 * Return the bounding box of the building, including the walls
	 * 
	 * @return
	 */
	public Extent getExtent() {
		return extent;
	}

	/**
	 * The standard operation for single-room buildings
	 * 
	 */
	public Room makeSingleRoom(MaterialManager mgr) {
		rooms = new LinkedList<Room>(); // make sure any old ones are gone
		Room r = new BlankRoom(mgr, extent, this);
		rooms.add(r);
		return r;
	}

	/**
	 * Fill this in - it's what actually makes the building
	 */
	public abstract void build(MaterialManager mgr);

	/**
	 * Attempt to build a number of internal floors in tall buildings. Floors
	 * are built in ascending order.
	 */
	protected void makeRooms(MaterialManager mgr) {
		Castle c = Castle.getInstance();

		// start placing floors until we run out
		for (int h = extent.miny + 1; h < extent.maxy - 4;) {
			int nexth = h + c.r.nextInt(3) + 4;
			placeFloorAt(mgr, h, nexth);

			h = nexth + 2; // because h and nexth delineate the internal space -
							// the air space - of the building
		}
	}

	/**
	 * Place a floor at height h above the building base. Floors are one deep,
	 * and are built in ascending order (i.e. the start of the 'rooms' list will
	 * have the lower floor in it, if there is one, since they are added to the
	 * head)
	 * 
	 * @param yAboveFloor
	 *            Y of the block just above the floor
	 * @param yBelowCeiling
	 *            Y of the block just below the ceiling
	 */
	private void placeFloorAt(MaterialManager mgr, int yAboveFloor,
			int yBelowCeiling) {

		// work out the extent of this room
		Extent roomExt = new Extent(extent);
		roomExt.miny = yAboveFloor - 1;
		roomExt.maxy = yBelowCeiling + 1;

		Room r = new PlainRoom(mgr, roomExt, this);
		addRoomAndBuildExitDown(r, false);
		WindowMaker.buildWindows(mgr, r);
	}

	/**
	 * Create and add a new room, attempting to build an exit down from this
	 * room to the one below. Assumes the room list is ordered such new rooms
	 * added are higher up.
	 * 
	 * @param newRoomExtent
	 * @param outside
	 *            is the room outside or inside
	 */
	protected void addRoomAndBuildExitDown(Room r, boolean outside) {
		Room lowerFloor = rooms.peekFirst(); // any prior floor will be the
		// first item
		rooms.addFirst(r); // add new room to head of list

		if (lowerFloor != null) {
			// there is a floor below - try to build some kind of link down
			buildVerticalExit(lowerFloor, r);
		}

	}

	/**
	 * Build a vertical exit between two rooms
	 * 
	 * @param lower
	 *            lower room
	 * @param upper
	 *            upper room
	 */
	private void buildVerticalExit(Room lower, Room upper) {
		World w = Castle.getInstance().getWorld();

		// get the inner extent of the lower room
		Extent innerLower = lower.e.expand(-1, Extent.ALL);

		// get one corner of that room (but go up one so we don't overwrite the
		// carpet)
		IntVector ladderPos = new IntVector(innerLower.minx,
				innerLower.miny + 1, innerLower.minz);

		Ladder ladder = new Ladder();
		ladder.setFacingDirection(BlockFace.NORTH);

		// build up, placing a ladder until we get to the floor above, and go
		// one square into the room
		// to clear the carpet too.
		for (int y = ladderPos.y; y <= upper.e.miny + 1; y++) {
			Block b = w.getBlockAt(ladderPos.x, y, ladderPos.z);
			b.setType(Material.LADDER);
			b.setData(ladder.getData());
		}
	}

	/**
	 * Add carpeting to this extent - this is the interior space of the
	 * building; the air space.
	 * 
	 * @param floor
	 */
	public void carpet(Extent floor, int col) {
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
	public void lightWalls(Extent e) {
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
	public void floorLights(Extent e) {
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
	 * Fill voids under the building in some way. If complete is true, make the
	 * block solid.
	 * 
	 * @param mgr
	 */
	void underfill(MaterialManager mgr, boolean complete) {
		Castle c = Castle.getInstance();
		World w = c.getWorld();
		int dx, dz;

		MaterialManager.MaterialDataPair mat = mgr.getSecondary();

		if (complete) {
			dx = 1;
			dz = 1;
		} else {
			dx = extent.xsize() < 8 ? extent.xsize() - 1 : 4;
			dz = extent.zsize() < 8 ? extent.zsize() - 1 : 4;
		}

		for (int x = extent.minx; x <= extent.maxx; x += dx) {
			for (int z = extent.minz; z <= extent.maxz; z += dz) {
				for (int y = extent.miny - 1;; y--) {
					Block b = w.getBlockAt(x, y, z);
					if (!b.getType().isSolid()) {
						b.setType(mat.m);
						b.setData((byte) mat.d);
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
	public boolean makeRandomExit(MaterialManager mgr) {
		Castle c = Castle.getInstance();

		GormPlugin.log("Attempting to make an exit from building "
				+ Integer.toString(id));

		// first we need to find a suitable nearby building. This version of the
		// code will only
		// link with buildings with colinear walls.

		List<Building> buildings = new ArrayList<Building>();
		for (Building r : c.getBuildings()) {
			if (r != this) {
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
					}
				}
			}
		}

		if (buildings.size() != 0) {
			Building b = buildings.get(c.r.nextInt(buildings.size()));
			GormPlugin.log(String.format(
					"Trying to make an exit between %d and %d!!!", id, b.id));
			return makeRandomExit(mgr, b);
		}
		return false;
	}

	/**
	 * Having found a building nearby, we need to find a pair of candidate
	 * rooms, one here and one over there.
	 */
	private boolean makeRandomExit(MaterialManager mgr, Building other) {
		// we don't want to always do one floor.
		Collections.shuffle(rooms);

		for (Room r : rooms) {
			for (Room r2 : other.rooms) {
				if (Math.abs(r2.e.miny - r.e.miny) < 4) {
					// that'll do!
					if (r2.e.miny < r.e.miny) // destination is lower than src.
						return makeExitBetweenRooms(mgr, r, r2);
					else
						return makeExitBetweenRooms(mgr, r2, r);

				}
			}
		}

		GormPlugin.log("Make exit failed; level mismatch");
		return false;
	}
	
	static final int[] offsets={0,1,-1,2,-2,3,-3,4,-4,5,-5};

	/**
	 * This is used to make a random horizontal exit to a building we already
	 * know intersects enough with us
	 * 
	 * @param r
	 */
	private boolean makeExitBetweenRooms(MaterialManager mgr, Room thisRoom,
			Room remoteRoom) {
		Extent intersection = thisRoom.e.intersect(remoteRoom.e);
		Direction dir;

		GormPlugin.log(String.format(
				"attempting to create exit between %d and %d", thisRoom.b.id,
				remoteRoom.b.id));

		if (thisRoom.exitMap.containsKey(remoteRoom)) {
			GormPlugin.log(String.format("exit already exists"));
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
		
		// shrink the intersection along its longest axis, so we don't end
		// up sliding the exit into a wall to avoid a window.
		intersection=intersection.expand(-1,Extent.LONGESTXZ);

		// try to find somewhere in the intersection which doesn't collide with
		// a window or existing exit

		IntVector offsetVec = dir.vec.rotate(1); // get a perpendicular to slide along
		for (int tries = 0; tries < offsets.length; tries++) {
			int offset = offsets[tries];

			// create the actual hole
			IntVector centreOfIntersection = intersection.getCentre().add(offsetVec.scale(offset));
			
			if(!intersection.contains(centreOfIntersection))
				continue; // slid out of intersection

			Extent e = new Extent(centreOfIntersection.x, thisRoom.e.miny + 1,
					centreOfIntersection.z, centreOfIntersection.x,
					thisRoom.e.miny + height, centreOfIntersection.z);

			// don't allow an exit if there's a window in the way!
			Extent wideexit = e.expand(2, Extent.X | Extent.Z).expand(1,Extent.Y);
			if (thisRoom.windowIntersects(wideexit)
					|| remoteRoom.windowIntersects(wideexit))
				continue;

			// create the two exit structures
			Exit src = new Exit(e, dir, thisRoom, remoteRoom);
			Exit dest = new Exit(e, dir.opposite(), remoteRoom, thisRoom);
			thisRoom.exits.add(src);
			thisRoom.exitMap.put(remoteRoom, src); // exit 'src' leads to room
													// 'r'
			remoteRoom.exits.add(dest);
			remoteRoom.exitMap.put(thisRoom, dest); // exit 'dest' leads back
													// here
			// blow the hole
			Castle.getInstance().fill(src.getExtent(), Material.AIR, 1);
			GormPlugin.log("hole blown: " + src.getExtent().toString());
			Castle.getInstance().postProcessExit(mgr, src);
			return true;
		}
		return false;
	}
}
