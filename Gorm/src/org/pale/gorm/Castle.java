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
	public Random r = new Random();

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
	 * returns true if e intersects any room or exit at all
	 * 
	 * @param e
	 * @return
	 */
	public boolean intersects(Extent e) {
		for (Room x : rooms) {
			if (x.getExtent().intersects(e))
				return true;
		}
		return overlapsExit(e);
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

	public void checkFill(Extent e, Material mat, int data) {
		GormPlugin.log("filling " + e.toString());
		int t = 0;
		for (int x = e.minx; x <= e.maxx; x++) {
			for (int y = e.miny; y <= e.maxy; y++) {
				for (int z = e.minz; z <= e.maxz; z++) {
					Block b = world.getBlockAt(x, y, z);
					if (canOverwrite(b)) {
						b.setType(mat);
						b.setData((byte) data);
						t++;
					}
				}
			}
		}
		GormPlugin.log("blocks filled: " + Integer.toString(t));
	}

	/**
	 * Fill an extent with a material and data DOES NOT CHECK that the stuff
	 * we're writing will overwrite anything
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
				}
			}
		}
		GormPlugin.log("blocks filled: " + Integer.toString(t));
	}

	/**
	 * We should use this when we might overwrite a room or exit, to prevent
	 * that.
	 * 
	 * @param b
	 * @return
	 */
	private boolean canOverwrite(Block b) {
		Material m = b.getType();
		// materials walls can be made of
		if (m == Material.SMOOTH_BRICK || m == Material.WOOD || m == Material.BRICK ||
				m == Material.LAPIS_BLOCK || m==Material.GOLD_BLOCK)
			return false;
		
		// check for 'inside' air which we can't overwrite
		if (m==Material.AIR && b.getData()==1) 
			return false;
		return true;
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
					if (canOverwrite(b)) {
						b.setType(Material.SMOOTH_BRICK);
						b.setData((byte) dataVals[r.nextInt(dataVals.length)]);
						t++;
					}
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
	 * Returns true if an extent overlaps an exit
	 * 
	 * @param exit
	 * @return
	 */
	public boolean overlapsExit(Extent x) {
		for (Extent e : exits) {
			if (x.intersects(e))
				return true;
		}
		return false;
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

	public void postProcessExit(Extent e, IntVector.Direction d) {
		IntVector v = d.vec;

		e = e.growDirection(d, 100); // stretch out the extent for scanning
										// purposes
		e.miny -= 100;

		if (v.x == 0) {
			// north/south case
			// for each x..
			for (int x = e.minx; x <= e.maxx; x++) {
				// keep building steps down until we hit floor. First get the
				// initial height.
				int z = (e.maxz + e.minz) / 2; // we start in the middle of the
												// exit, in the gap itself
				int y = e.getHeightWithin(x, z);
				GormPlugin.log(String.format("%d,%d in %s has height %d", x, z,
						e.toString(), y));
				for (;;) {
					z += v.z; // move one step out the exit
					int y2 = e.getHeightWithin(x, z); // get that height
					GormPlugin.log(String.format("%d,%d in %s has height %d",
							x, z, e.toString(), y2));
					// if it is lower than where we were, build it up with
					// stairs
					if (y2 != Extent.BADHEIGHT && y2 < y) {
						// get the direction
						IntVector dir = new IntVector(-v.x, 0, -v.z);
						// make stair data and set the material
						setStairs(x, y, z, dir.toBlockFace());
						y--; // and go down one
					} else
						break; // otherwise get out of the loop
				}
			}
		} else {                //THIS CODE IS THE SAME AS ABOVE apart from the direction. Yes, it's bad.
			// east/west case 
			// for each z..
			for (int z = e.minz; z <= e.maxz; z++) {
				// keep building steps down until we hit floor. First get the
				// initial height.
				int x = (e.maxx + e.minx) / 2; // we start in the middle of the
												// exit, in the gap itself
				int y = e.getHeightWithin(x, z);
				GormPlugin.log(String.format("%d,%d in %s has height %d", x, z,
						e.toString(), y));
				for (;;) {
					x += v.x; // move one step out the exit
					int y2 = e.getHeightWithin(x, z); // get that height
					GormPlugin.log(String.format("%d,%d in %s has height %d",
							x, z, e.toString(), y2));
					// if it is lower than where we were, build it up with
					// stairs
					if (y2 != Extent.BADHEIGHT && y2 < y) {
						// get the direction
						IntVector dir = new IntVector(-v.x, 0, -v.z);
						// make stair data and set the material
						setStairs(x, y, z, dir.toBlockFace());
						y--; // and go down one
					} else
						break; // otherwise get out of the loop
				}
			}
		}
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
