package org.pale.gorm;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.material.Stairs;

/**
 * Singleton class for the whole castle.
 * 
 * @author white
 * 
 */
public class Castle {
	private static Castle instance;
	private Extent allRoomsExtent = new Extent();
	private World world;
	private Random r = new Random();

	private List<Extent> exits = new LinkedList<Extent>();

	/**
	 * Private ctor so this is a singleton
	 */
	private Castle() {
	}

	/**
	 * MUST call this before building
	 * 
	 */
	public void setWorld(World w) {
		world = w;
	}

	public World getWorld() {
		return world;
	}

	public static Castle getInstance() {
		if (instance == null) {
			instance = new Castle();
		}
		return instance;
	}

	private List<Room> rooms = new LinkedList<Room>();

	public int getRoomCount() {
		return rooms.size();
	}

	/**
	 * Add a room to the internal lists.
	 * 
	 * @param r
	 */
	void addRoom(Room r) {
		rooms.add(r);
		allRoomsExtent = allRoomsExtent.union(r.getExtent());
	}

	/**
	 * returns true if e intersects any room at all
	 * 
	 * @param e
	 * @return
	 */
	public boolean intersects(Extent e) {
		for (Room x : rooms) {
			if (x.getExtent().intersects(e))
				return true;
		}

		return false;
	}

	/**
	 * Return the list of exits
	 */
	public List<Extent> getExits() {
		return exits;
	}

	/**
	 * Add an exit
	 */
	public void addExit(Extent e) {
		exits.add(new Extent(e));
		/*
		 * for(Extent x:exits){ fill(x,Material.AIR,0); x = new Extent(x);
		 * x.miny-=10; x.maxy-=2; ensureSteps(x); }
		 */
	}

	/**
	 * Fill an extent with a material and data
	 * 
	 * @param e
	 * @param mat
	 * @param data
	 */

	public void fill(Extent e, Material mat, int data) {
		GormPlugin.log("filling " + e.toString());
		int t = 0;
		for (int x = e.minx; x <= e.maxx; x++) {
			for (int y = e.miny; y <= e.maxy; y++) {
				for (int z = e.minz; z <= e.maxz; z++) {
					Block b = world.getBlockAt(x, y, z);
					b.setType(mat);
					b.setData((byte) data);
					t++;
				}
			}
		}
		GormPlugin.log("blocks filled: " + Integer.toString(t));
	}

	public void fillBrickWithCracksAndMoss(Extent e) {
		GormPlugin.log("filling " + e.toString());
		int t = 0;
		int dataVals[] = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 2, 2, 1, 2 };
		for (int x = e.minx; x <= e.maxx; x++) {
			for (int y = e.miny; y <= e.maxy; y++) {
				for (int z = e.minz; z <= e.maxz; z++) {
					Block b = world.getBlockAt(x, y, z);
					b.setType(Material.SMOOTH_BRICK);
					b.setData((byte) dataVals[r.nextInt(dataVals.length)]);
					t++;
				}
			}
		}
		GormPlugin.log("blocks filled: " + Integer.toString(t));
		spreadMoss(e);
		// spreadMoss(e);
	}

	private void doSpreadMoss(int cx, int cy, int cz) {
		for (int x = cx - 1; x <= cx + 1; x++) {
			for (int y = cy - 1; y <= cy + 1; y++) {
				for (int z = cz - 1; z <= cz + 1; z++) {
					Block b = world.getBlockAt(x, y, z);
					if (b.getType() == Material.SMOOTH_BRICK
							&& b.getData() == 0 && r.nextInt(10) < 1) {
						b.setData((byte) 1);
					}
				}
			}
		}
	}

	public void spreadMoss(Extent e) {
		for (int x = e.minx; x <= e.maxx; x++) {
			for (int y = e.miny; y <= e.maxy; y++) {
				for (int z = e.minz; z <= e.maxz; z++) {
					Block b = world.getBlockAt(x, y, z);
					if (b.getType() == Material.SMOOTH_BRICK
							&& b.getData() == 1) {
						doSpreadMoss(x, y, z);
					}
				}
			}
		}
	}

	public Material getRandomWallMaterial() {
		Material[] m = { Material.STONE, Material.SMOOTH_BRICK, Material.WOOD,
				Material.BRICK };
		return m[r.nextInt(m.length)];
	}

	public Collection<Room> getRooms() {
		return rooms;
	}

	public boolean contains(IntVector p) {
		for (Room x : rooms) {
			if (x.contains(p)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns true if an exit is near an existing exit
	 * 
	 * @param exit
	 * @return
	 */
	public boolean isNearExistingExit(Extent exit) {
		exit = exit.expand(1, Extent.ALL);
		for (Extent e : exits) {
			if (exit.intersects(e))
				return true;
		}
		return false;
	}

	/**
	 * A version of getHighestBlockAt that works only within an extent
	 * 
	 * @param e
	 * @param x
	 * @param z
	 * @return
	 */
	private Block getHighestBlockInExtent(Extent e, int x, int z) {
		for (int y = e.maxy; y >= e.miny; y--) {
			Block b = world.getBlockAt(x, y, z);
			if (!b.isEmpty())
				return b;
		}
		return null;
	}

	private void setStairs(int x, int y, int z, BlockFace dir) {
		Block b = world.getBlockAt(x, y, z);
		Stairs s = new Stairs(Material.SMOOTH_STAIRS);
		s.setFacingDirection(dir);
		b.setData(s.getData());
		b.setType(s.getItemType());
	}

	static boolean isStairs(Material m) {
		return (m == Material.COBBLESTONE_STAIRS || m == Material.WOOD_STAIRS
				|| m == Material.BIRCH_WOOD_STAIRS
				|| m == Material.SMOOTH_STAIRS || m == Material.BRICK_STAIRS);
	}

	/**
	 * If there is a step on the ground here, try to put stairs on it.
	 * 
	 * @param e
	 */
	public void postProcessExit1(Extent e) {
		// scan south->north first.
		for (int x = e.minx; x <= e.maxx; x++) {
			int prevh = -1000;
			for (int z = e.minz; z <= e.maxz; z++) {
				Block b = getHighestBlockInExtent(e, x, z);
				if (b != null) {
					int h = b.getY();

					if (!isStairs(b.getType()) && prevh > -1000) {
						if (h - prevh == 1) { // we have gone UP - build up the
												// previous block to match me
							setStairs(x, h, z - 1, BlockFace.SOUTH);
							prevh = -1000;
						} else if (h - prevh == -1) { // we have gone DOWN -
														// build this block up
														// to match prevh
							setStairs(x, h + 1, z, BlockFace.NORTH);
							prevh = -1000;
						}
					}
					prevh = h;
				}
			}
		}
	}

	/**
	 * Here, we try to ensure that the exit has stairs to cover it.
	 * 
	 * @param e
	 *            an extent, containing the 'floor zone' of the exit.
	 */
	public void postProcessExit(Extent e) {

		// look for blocks where the surrounding blocks are higher by >1. If you
		// find one, build it up. Keep doing that until we can't do it any more

		for (int i = 0; i < 100; i++) { // for safety we try 100 times only
			boolean builtUp = false;
			for (int x = e.minx; x <= e.maxx; x++) {
				for (int z = e.minz; z <= e.maxz; z++) {
					builtUp = buildUpLowBlock(e, x, z);
				}
			}
			if (!builtUp)
				break;
		}

		// then, try to build steps which go from a block 1 below to a block 1
		// higher.

		for (int i = 0; i < 100; i++) { // for safety we try 100 times only
			boolean builtUp = false;
			for (int x = e.minx; x <= e.maxx; x++) {
				for (int z = e.minz; z <= e.maxz; z++) {
					builtUp = singleStepUpBlock(e, x, z);
				}
			}
			if (!builtUp)
				break;
		}
	}

	/**
	 * If this block has a block next to it which is one higher, build a step up
	 * on top of it
	 * 
	 * @param c
	 * @param e
	 * @param x
	 * @param z
	 * @return
	 */
	private boolean singleStepUpBlock(Extent e, int x, int z) {
		int y = e.getHeightWithin(x, z);
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if (dx == 0 || dz == 0) {
					int y2 = e.getHeightWithin(x + dx, z + dz);
					// don't make stairs which step up to stairs at the same
					// level
					Material m = world.getBlockAt(dx + x, y2, dz + z).getType();
					if (m != Material.SMOOTH_STAIRS
							&& m != Material.COBBLESTONE_STAIRS
							&& m != Material.WOOD_STAIRS
							&& m != Material.BIRCH_WOOD_STAIRS) {
						if ((y2 - y) == 1) { // place stairs!
							Block b = world.getBlockAt(x, y + 1, z);
							// get the direction
							IntVector dir = new IntVector(dx, 0, dz);
							// make stair data and set the material
							Stairs stair = new Stairs(Material.SMOOTH_STAIRS);
							stair.setFacingDirection(dir.toBlockFace());
							b.setType(stair.getItemType());
							b.setData(stair.getData());
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	/**
	 * If this block is more than 1 block lower than any of its neighbours,
	 * build it up.
	 * 
	 * @param x
	 * @param z
	 */
	private boolean buildUpLowBlock(Extent e, int x, int z) {
		int y = e.getHeightWithin(x, z);
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if (dx == 0 || dz == 0) {
					int y2 = e.getHeightWithin(x + dx, z + dz);
					if ((y2 - y) > 1) { // built it up!
						Block b = world.getBlockAt(x, y + 1, z);
						b.setType(Material.DIAMOND_BLOCK); // with diamond for
															// now
															// so I can see it
															// work!
						return true;
					}
				}
			}
		}
		return false;
	}

	public void raze() {
		for (Room r : rooms) {
			fill(r.getExtent(), Material.AIR, 0);
			fill(r.getExtent().getWall(IntVector.Direction.DOWN),
					Material.GRASS, 0);
		}
		rooms.clear();

		for (Extent x : exits) {
			fill(x, Material.AIR, 0);
		}
		exits.clear();

	}
}
