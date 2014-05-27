package org.pale.gorm;

import java.util.LinkedList;
import java.util.Random;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.material.Vine;
import org.pale.gorm.Extent.LocationRunner;
import org.pale.gorm.rooms.BlankRoom;
import org.pale.gorm.rooms.EmptyRoom;
import org.pale.gorm.rooms.PlainRoom;
import org.pale.gorm.rooms.SpawnerRoom;
import org.pale.gorm.roomutils.WindowMaker;

/**
 * A building in the castle, consisting of rooms
 * 
 * @author white
 * 
 */
public abstract class Building {
	/**
	 * Given a parent building and a size, produce an extent for a building
	 * which can be slid around by the Builder.
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
	int id = idCounter++;

	/**
	 * If a roof has been added (NOT a roof garden - roofs are separate entities
	 * which can be overwritten) this is its height.
	 */
	protected int roofHeight = 0;

	/**
	 * List of the rooms - vertical sections within the building. DO NOT add
	 * rooms directly, use addRoom()
	 */
	public LinkedList<Room> rooms = new LinkedList<Room>();

	protected void addRoomTop(Room r) {
		rooms.addFirst(r);
		Castle.getInstance().addRoom(r);
	}

	protected void addRoomBasement(Room r) {
		rooms.addLast(r);
		Castle.getInstance().addRoom(r);
	}

	/**
	 * The extent of the entire building
	 */
	public Extent extent;

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
		addRoomTop(r);
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
	 * Furnish rooms after building
	 */
	public void furnish(MaterialManager mgr) {
		for (Room r : rooms) {
			r.furnish(mgr);
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

		Room r = roomByGrade(mgr, roomExt, this);
		addRoomAndBuildExitDown(r, false);
		WindowMaker.buildWindows(mgr, r);
	}

	/**
	 * Get the noise determined grade value for this building, in order to
	 * determine the 'level of upkeep' of this section of the castle.
	 * 
	 * @return grade level of the current building, from 0 to 1
	 */

	public double grade() {
		IntVector centre = this.extent.getCentre();
		double grade = Noise.noise2Dfractal(centre.x, centre.z, 3, 3, 3, 0.8);
		// rebalance such that non-dungeon castles are friendlier and
		// higher-grade
		if ((GormPlugin.getInstance().getIsDungeon() == false)
				&& (grade < 0.35)) {
			grade += 0.5;
		}
		return grade;
	}

	/**
	 * Get the grade level for this building, in order to determine the 'level
	 * of upkeep' of this section of the castle.
	 * 
	 * @return grade level of the current building, from 1 to 4
	 */

	public int gradeInt() {
		double grade = this.grade();
		if (grade <= 0.35) {
			return 1;
		} else if (grade <= 0.5) {
			return 2;
		} else if (grade <= 0.65) {
			return 3;
		} else {
			return 4;
		}
	}

	/**
	 * Delegate generation of new room to 1 of 4 grade sets
	 * 
	 * @param mgr
	 * @param roomExt
	 * @param bld
	 * @return Room
	 */

	private Room roomByGrade(MaterialManager mgr, Extent roomExt, Building bld) {
		double grade = bld.grade();
		if (grade <= 0.35) {
			return gradeLow(mgr, roomExt, bld);
		} else if (grade <= 0.5) {
			return gradeMidLow(mgr, roomExt, bld);
		} else {
			return gradeHigh(mgr, roomExt, bld);
		}
	}

	private Room gradeLow(MaterialManager mgr, Extent roomExt, Building bld) {
		Random rnd = new Random();
		switch (rnd.nextInt(3)) {
		case 0:
			return new EmptyRoom(mgr, roomExt, bld);
		default:
			return new SpawnerRoom(mgr, roomExt, bld);
		}
	}

	private Room gradeMidLow(MaterialManager mgr, Extent roomExt, Building bld) {
		Random rnd = new Random();
		switch (rnd.nextInt(3)) {
		default:
			return new PlainRoom(mgr, roomExt, bld);
		}
	}

	private Room gradeHigh(MaterialManager mgr, Extent roomExt, Building bld) {
		Random rnd = new Random();
		switch (rnd.nextInt(5)) {
		default:
			return new PlainRoom(mgr, roomExt, bld);
		}
	}

	/**
	 * add a new room, attempting to build an exit down from this room to the
	 * one below. Assumes the room list is ordered such new rooms added are
	 * higher up.
	 * 
	 * @param newRoomExtent
	 * @param outside
	 *            is the room outside or inside
	 */
	protected void addRoomAndBuildExitDown(Room r, boolean outside) {
		Room lowerFloor = rooms.peekFirst(); // any prior floor will be the
		// first item
		addRoomTop(r); // adds to head
		if (lowerFloor != null) {
			// there is a floor below - try to build some kind of link down
			lowerFloor.buildVerticalExitUpTo(r);
		}
	}

	/**
	 * Add a new room, attempting to build an exit up from it to the next floor.
	 */
	protected void addRoomAndBuildExitUp(Room r, boolean outside) {
		Room upperFloor = rooms.peekLast(); // any prior floor will be the last
											// item
		addRoomBasement(r); // adds to head
		if (upperFloor != null) {
			// there is a floor below - try to build some kind of link down
			r.buildVerticalExitUpTo(upperFloor);
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
			if (Castle.requiresLight(x, y, e.minz))
				addLight(x, y, e.minz);
			if (Castle.requiresLight(x, y, e.maxz))
				addLight(x, y, e.maxz);
		}
		for (int z = e.minz; z <= e.maxz; z++) {
			if (Castle.requiresLight(e.minx, y, z))
				addLight(e.minx, y, z);
			if (Castle.requiresLight(e.maxx, y, z))
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
	protected void underfill(MaterialManager mgr, boolean complete) {
		Castle c = Castle.getInstance();
		World w = c.getWorld();
		int dx, dz;

		// try to build a basement down here!
		if (attemptNewRoomUnder(mgr))
			return;

		MaterialDataPair mat = mgr.getSecondary();

		if (complete) {
			dx = 1;
			dz = 1;
		} else {
			dx = extent.xsize() <= 8 ? extent.xsize() - 1
					: calcUnderfill(extent.xsize() - 1);
			dz = extent.zsize() <= 8 ? extent.zsize() - 1
					: calcUnderfill(extent.zsize() - 1);
		}

		for (int x = extent.minx; x <= extent.maxx; x += dx) {
			for (int z = extent.minz; z <= extent.maxz; z += dz) {
				for (int y = extent.miny - 1;; y--) {
					Block b = w.getBlockAt(x, y, z);
					if (!b.getType().isSolid() && Castle.canOverwrite(b)) { // also
																			// avoid
																			// "unwritable air"
						b.setType(mat.m);
						b.setData((byte) mat.d);
					} else {
						break;
					}
				}
			}
		}

	}

	/**
	 * Rather than underfilling, try to make a basement room. This won't work if
	 * there is another building in the way.
	 * 
	 * @param mgr
	 * @return
	 */
	private boolean attemptNewRoomUnder(MaterialManager mgr) {

		GormPlugin.log("attempting basement");
		Castle c = Castle.getInstance();
		// get the floor, without the walls
		Extent e = extent.getWall(Direction.DOWN).expand(-1,
				Extent.X | Extent.Z);
		// decrease the floor y until we either hit a room or we're completely
		// underground

		for (;;) {
			e = e.subvec(0, 1, 0);
			if (c.intersects(e)) {
				GormPlugin.log("basement failed, castle in the way");
				return false; // no can do - there's a building down there
			}
			for (int x = e.minx; x <= e.maxx; x++) {
				// no y, we assume y=miny=maxy
				for (int z = e.minz; z <= e.maxz; z++) {
					Block b = c.getWorld().getBlockAt(x, e.miny, z);
					if (!b.getType().isSolid())
						continue;
				}
			}
			// but we need to go a bit deeper than that to make sure of
			// headroom!
			// Also put the walls back on
			e = e.subvec(0, 5, 0).expand(1, Extent.X | Extent.Z);

			// this is the total extent of the new room
			e = extent.getWall(Direction.DOWN).union(e);
			c.fill(e, mgr.getPrimary()); // make the walls
			c.fill(e.expand(-1, Extent.ALL), Material.AIR, 0); // clear the
																// contents

			// we'll succeed eventually unless the world has gone very strange
			// now we add a new room onto the bottom of the building (i.e. at
			// the start)
			Room r = new PlainRoom(mgr, e, this);
			addRoomAndBuildExitUp(r, false);
			GormPlugin.log("basement done! " + e.toString());
			extent = extent.union(e);
			return true;
		}
	}

	int calcUnderfill(int size) {
		int dPillar = 4;
		int count = 2;
		int finalCount = 0;
		while (count < 4) {
			if ((count * (Math.ceil(size / count))) == (size)) {
				finalCount = count;
			}
			count += 1;
		}
		if (finalCount > 0) {
			dPillar = (int) (Math.ceil(size / finalCount));
		}
		return dPillar;
	}

	void addLight(int x, int y, int z) {
		Block b = Castle.getInstance().getWorld().getBlockAt(x, y, z);
		b.setData((byte) 0);
		b.setType(Material.TORCH);
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
	 * Force update packet sending
	 */
	public void update() {
		for (Room r : rooms) {
			r.update();
		}
	}

	/**
	 * Tell this building to make a random exit to an adjacent building - this
	 * is for horizontal exits
	 */
	public boolean makeRandomExit() {
		Castle c = Castle.getInstance();

		// find a room with few exits. Or not many. Just iterate until an exit
		// has been made.

		for (Room r : rooms) {
			if (r.attemptMakeExit())
				return true;
		}
		return false;
	}

	public void ruin() {
		final Castle c = Castle.getInstance();
		if (gradeInt() > 1)
			return;

		// we also want to ruin the roof!
		Extent ruinExtent = new Extent(extent);
		ruinExtent.maxy += roofHeight;

		GormPlugin.log("RUINING " + ruinExtent.toString());

		final double chance = grade() * 1.5;
		final World w = c.getWorld();
		final Random rnd = c.r;
		final int minx = ruinExtent.minx;
		final int miny = ruinExtent.miny;
		final int minz = ruinExtent.minz;
		final double xsize = ruinExtent.xsize();
		final double ysize = ruinExtent.ysize();
		final double zsize = ruinExtent.zsize();

		// one side will be badly hit.
		Direction rd;
		final Direction ruinDir;
		do {
			rd = Direction.values()[rnd.nextInt(Direction.values().length)];
		} while (rd.vec.y != 0); // but not UP or DOWN!
		ruinDir = rd;

		ruinExtent.runOnAllLocations(new LocationRunner() {
			@Override
			public void run(int x, int y, int z) {
				Block b = w.getBlockAt(x, y, z);
				if (b.getType() != Material.AIR
						&& b.getType() != Material.WATER) {
					// less chance of ruinage further down
					double yInExt = ((double) (y - miny)) / ysize;
					double heightFactor = yInExt * yInExt;

					if (y >= extent.maxy)
						heightFactor *= 2; // make roofs even worse

					// and we also work out some kind of factor across the
					// building
					double edgeFactor;
					double xInExt = ((double) (x - minx)) / xsize;
					double zInExt = ((double) (z - minz)) / zsize;
					switch (ruinDir) {
					case SOUTH:
						edgeFactor = 1.0 - (zInExt + 1.0) * 0.5;
						break;
					case NORTH:
						edgeFactor = (zInExt + 1.0) * 0.5;
						break;
					case EAST:
						edgeFactor = 1.0 - (xInExt + 1.0) * 0.5;
						break;
					case WEST:
						edgeFactor = (xInExt + 1.0) * 0.5;
						break;
					default:
						edgeFactor = 0;
					}

					if (rnd.nextDouble() < heightFactor * chance * edgeFactor
							* edgeFactor) {
						int holeh = rnd.nextInt(5) + 1;
						Extent e = new Extent(x, y, z, x, y + holeh, z)
								.intersect(extent);
						if (e != null)
							c.fill(e, Material.AIR, 0);
					} else if (rnd.nextDouble() < 0.1 && b.getType().isSolid()) {
						// if that didn't work, what about vines? The list above
						// is a list
						// of things we can slap vines onto.
						// really these should be in a random order.
						for (Direction d : Direction.values()) {
							if (d.vec.y == 0) {
								IntVector v = new IntVector(x, y, z).add(d.vec);
								Block b2 = c.getBlockAt(v);
								if (b2.getType() == Material.AIR) {
									// that'll do.
									b2.setType(Material.VINE);
									// if this is right, this is *horrible*
									BlockState bs = b2.getState();
									Vine vine = (Vine) bs.getData();
									vine.removeFromFace(BlockFace.NORTH);
									vine.removeFromFace(BlockFace.SOUTH);
									vine.removeFromFace(BlockFace.EAST);
									vine.removeFromFace(BlockFace.WEST);
									vine.putOnFace(d.opposite().vec
											.toBlockFace());
									bs.setData(vine);
									bs.update();
								}
							}
						}
					}
				}
			}
		});
	}

}
