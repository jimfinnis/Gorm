package org.pale.gorm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.HashSet;

//SNARK import net.minecraft.server.v1_7_R3.TileEntity;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
//SNARK import org.bukkit.craftbukkit.v1_7_R3.CraftWorld;
import org.bukkit.entity.Villager.Profession;
import org.bukkit.material.Stairs;
import org.bukkit.material.Torch;
import org.pale.gorm.roomutils.ExitDecorator;

/**
 * Singleton class for the whole castle.
 * 
 * @author white
 * 
 */
public class Castle {
	private static Castle instance;
	private World world;
	public Random r = new Random();
	private HashMap<Profession, Integer> denizenCounts = new HashMap<Profession, Integer>();

	/**
	 * Each chunk has a list of rooms which intersect it. Naturally, a room is
	 * likely to belong to more than one chunk
	 */
	private Map<Integer, Collection<Room>> roomsByChunk = new HashMap<Integer, Collection<Room>>();
	/**
	 * This is a similar map to roomsByChunk, but for buildings.
	 */
	private Map<Integer, Collection<Building>> buildingsByChunk = new HashMap<Integer, Collection<Building>>();

	/**
	 * Rooms so we can sort by number of exits.
	 */
	private ArrayList<Room> rooms = new ArrayList<Room>();

	/**
	 * Private ctor so this is a singleton
	 */
	private Castle() {
		for (Profession p : Profession.values()) {
			denizenCounts.put(p, 0);
		}
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

	private List<Building> buildings = new ArrayList<Building>();

	public int getBuildingCount() {
		return buildings.size();
	}

	/**
	 * Add a building to the internal lists, along with its rooms
	 * 
	 * @param r
	 */
	void addBuilding(Building b) {
		for (int c : b.getExtent().getChunks()) {
			Collection<Building> list;
			if (!buildingsByChunk.containsKey(c)) {
				list = new ArrayList<Building>();
				buildingsByChunk.put(c, list);
			} else
				list = buildingsByChunk.get(c);
			list.add(b);
		}
		buildings.add(b);
	}

	/**
	 * returns true if e intersects any room at all. This works by iterating
	 * over the rooms in the chunks for that extent
	 * 
	 * @param e
	 * @return
	 */
	public boolean intersects(Extent e) {
		for (int c : e.getChunks()) {
			Collection<Building> list = buildingsByChunk.get(c);
			if (list != null) {
				for (Building b : list) {
					if (b.getExtent().intersects(e))
						return true;
				}
			}
		}
		return false;
	}

	public Collection<Building> getIntersectingBuildings(Extent e) {
		Collection<Building> blist = new ArrayList<Building>();
		for (int c : e.getChunks()) {
			Collection<Building> list = buildingsByChunk.get(c);
			if (list != null) {
				for (Building b : blist) {
					if (b.getExtent().intersects(e))
						list.add(b);
				}
			}
		}
		return blist;

	}

	public double grade(IntVector v) {
		double grade = Noise.noise2Dfractal(v.x, v.z, 3, 3, 3, 0.8);
		// rebalance such that non-dungeon castles are friendlier and
		// higher-grade
		if ((GormPlugin.getInstance().getIsDungeon() == false)
				&& (grade < 0.35)) {
			grade += 0.5;
		}
		return grade;

	}

	/**
	 * Get the grade level for the given extent, in order to determine the
	 * 'level of upkeep' of this section of the castle. Use this when we do not
	 * have access to the current building.
	 * 
	 * @return grade level of the given extent, from 1 to 4
	 */

	public int gradeInt(IntVector v) {
		Castle c = Castle.getInstance();
		double grade = c.grade(v);
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
	 * Fill an extent with a material and data, checking we don't overwrite
	 * certain things
	 * 
	 * @param e
	 * @param mat
	 * @param data
	 */

	public void checkFill(Extent e, Material mat, int data) {
		if (mat == Material.SMOOTH_BRICK && data == 0) {// if this is plain
														// brick, flash it up!
			fillBrickWithCracksAndMoss(e, true);
		} else {
			for (int x = e.minx; x <= e.maxx; x++) {
				for (int y = e.miny; y <= e.maxy; y++) {
					for (int z = e.minz; z <= e.maxz; z++) {
						Block b = world.getBlockAt(x, y, z);
						if (canOverwrite(b)) {
							b.setType(mat);
							b.setData((byte) data);
						}
					}
				}
			}
		}
	}

   public void addLightToWall(int x, int y, int z,BlockFace dir) {
        World w = Castle.getInstance().getWorld();
        Block b = w.getBlockAt(x, y, z);
        
        // a hack. Creating a torch will cause it
        // to drop, unless there's something under it.
        // Even when it's attached to a wall.
        // Madness.
       
       Block bunder = w.getBlockAt(x, y-1, z);
       if(canOverwrite(bunder) && canOverwrite(b)){
           bunder.setType(Material.DIRT);
        
           Torch t = new Torch(Material.TORCH);
           t.setFacingDirection(dir);
           b.setType(Material.TORCH);
           b.setData(t.getData());
        
           bunder.setType(Material.AIR);
       }
    }
	public void checkFill(Extent e, MaterialDataPair mp) {
		checkFill(e, mp.m, mp.d);
	}


	/**
	 * Fill a wall extent (i.e. one of the dimensions must be of zero width)
	 * with a pattern of materials. There's an assumption that it's not a floor
	 * or ceiling but I'm sure that could be fixed.
	 * 
	 * @param wallExtent
	 * @param mats
	 *            array of material pairs
	 * @param n
	 *            number of elements in array (might want to use a subset)
	 */
	public void patternFill(Extent wallExtent, MaterialDataPair[] mats, int n,
			Random rnd) {
		if (rnd == null)
			rnd = r;
		// work out the zero axis
		int shortAxis = wallExtent.getShortestAxis();
		// work out the long axis
		int longAxis = wallExtent.getLongestAxis();
		int xdim, ydim; // dimensions of the 2D array we'll make the pattern in
		switch (shortAxis) {
		case Extent.Y:
			if (longAxis == Extent.X) {
				xdim = wallExtent.xsize();
				ydim = wallExtent.zsize();
			} else {
				xdim = wallExtent.zsize();
				ydim = wallExtent.xsize();
			}
			break;
		case Extent.X:
			if (longAxis == Extent.Z) {
				xdim = wallExtent.zsize();
				ydim = wallExtent.ysize();
			} else {
				xdim = wallExtent.ysize();
				ydim = wallExtent.zsize();
			}
			break;
		default:
		case Extent.Z:
			if (longAxis == Extent.X) {
				xdim = wallExtent.xsize();
				ydim = wallExtent.ysize();
			} else {
				xdim = wallExtent.ysize();
				ydim = wallExtent.xsize();
			}
			break;
		}

		// doing it like this so I can replace with other pattern generators
		// later.

		int[][] array = new int[xdim][ydim];
		if (xdim == 1 || ydim == 1) {
			// simple version without symmetry, for tall windows.
			for (int x = 0; x < xdim; x++) {
				for (int y = 0; y < ydim; y++) {
					array[x][y] = rnd.nextInt(n);
				}
			}
		} else {
			for (int x = 0; x < xdim / 2; x++) {
				for (int y = 0; y < ydim / 2; y++) {
					int m = rnd.nextInt(n);
					array[x][y] = m;
					array[(xdim - 1) - x][y] = m;
					array[x][(ydim - 1) - y] = m;
					array[(xdim - 1) - x][(ydim - 1) - y] = m;
				}
			}
		}

		// now slap it into the extent
		for (int x = 0; x < xdim; x++) {
			for (int y = 0; y < ydim; y++) {
				int wx, wz, wy;
				switch (shortAxis) {
				case Extent.Y:
					wy = wallExtent.miny;
					if (longAxis == Extent.X) {
						wx = x + wallExtent.minx;
						wz = y + wallExtent.minz;
					} else {
						wz = x + wallExtent.minz;
						wx = y + wallExtent.minx;
					}
					break;
				case Extent.X:
					wx = wallExtent.minx;
					if (longAxis == Extent.Y) {
						wy = x + wallExtent.miny;
						wz = y + wallExtent.minz;
					} else {
						wz = x + wallExtent.minz;
						wy = y + wallExtent.miny;
					}
					break;
				default:
				case Extent.Z:
					wz = wallExtent.minz;
					if (longAxis == Extent.X) {
						wx = x + wallExtent.minx;
						wy = y + wallExtent.miny;
					} else {
						wy = x + wallExtent.miny;
						wx = y + wallExtent.minx;
					}
					break;
				}
				Block b = world.getBlockAt(wx, wy, wz);
				MaterialDataPair mat = mats[array[x][y]];
				b.setType(mat.m);
				b.setData((byte) mat.d);

			}
		}

	}

	/**
	 * Replace certain blocks within an Extent
	 * 
	 * @param e
	 * @param mat
	 * @param data
	 * @param matReplace
	 * @param dataReplace
	 */

	public void replace(Extent e, Material mat, int data, Material matReplace,
			int dataReplace) {
		for (int x = e.minx; x <= e.maxx; x++) {
			for (int y = e.miny; y <= e.maxy; y++) {
				for (int z = e.minz; z <= e.maxz; z++) {
					Block b = world.getBlockAt(x, y, z);
					if (b.getType() == matReplace) {
						if (b.getData() == (byte) dataReplace) {
							b.setType(mat);
							b.setData((byte) data);
						}
					}
				}
			}
		}
	}

	public void replace(Extent e, MaterialDataPair mp,
			MaterialDataPair mpReplace) {
		replace(e, mp.m, mp.d, mpReplace.m, mpReplace.d);
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
		if (mat == Material.SMOOTH_BRICK && data == 0) // if this is plain
														// brick, flash it up!
			fillBrickWithCracksAndMoss(e, false);
		else {
			for (int x = e.minx; x <= e.maxx; x++) {
				for (int y = e.miny; y <= e.maxy; y++) {
					for (int z = e.minz; z <= e.maxz; z++) {
						Block b = world.getBlockAt(x, y, z);
						b.setType(mat);
						b.setData((byte) data);
					}
				}
			}
		}
	}

	public void fill(Extent e, MaterialDataPair mp) {
		fill(e, mp.m, mp.d);
	}

	/**
	 * We should use this when we might overwrite a blg or exit, to prevent
	 * that.
	 * 
	 * @param b
	 * @param mode
	 * @return
	 */
	public static boolean canOverwrite(Block b) {
		Material m = b.getType();
		// this is going to be hideous if they remove getData().
		byte data = b.getData();

		if(m == Material.SMOOTH_BRICK || m == Material.WOOD
				|| m == Material.BRICK || m == Material.COBBLESTONE 
				|| (m == Material.SANDSTONE && data != 0) || isStairs(m))
			return false;
		
		return true;
	}

	/**
	 * Fill an area with bricks, adding moss and cracks.
	 * 
	 * @param e
	 * @param checkOverwrite
	 */
	private void fillBrickWithCracksAndMoss(Extent e, boolean checkFill) {
		int t = 0;
		int dataVals[] = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 2, 2, 1, 2 };
		for (int x = e.minx; x <= e.maxx; x++) {
			for (int y = e.miny; y <= e.maxy; y++) {
				for (int z = e.minz; z <= e.maxz; z++) {
					Block b = world.getBlockAt(x, y, z);
					if (!checkFill || canOverwrite(b)) {
						b.setType(Material.SMOOTH_BRICK);
						b.setData((byte) dataVals[r.nextInt(dataVals.length)]);
					}
				}
			}
		}
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

	public Collection<Building> getBuildings() {
		return buildings;
	}

	/**
	 * Return a list of buildings within a given extent
	 * 
	 * @return
	 */
	public Collection<Building> getBuildingsIntersecting(Extent e) {
		ArrayList<Building> out = new ArrayList<Building>();

		for (Building r : buildings) {
			if (r.intersects(e))
				out.add(r);
		}
		return out;
	}

	public static boolean isStairs(Material m) {
		return (m == Material.COBBLESTONE_STAIRS || m == Material.WOOD_STAIRS
				|| m == Material.BIRCH_WOOD_STAIRS
				|| m == Material.SANDSTONE_STAIRS
				|| m == Material.SMOOTH_STAIRS || m == Material.BRICK_STAIRS
				|| m == Material.QUARTZ_STAIRS
				|| m == Material.NETHER_BRICK_STAIRS
				|| m == Material.JUNGLE_WOOD_STAIRS || m == Material.SPRUCE_WOOD_STAIRS);
	}

    public void setStairs(int x, int y, int z, BlockFace dir, Material mat) {
        Block b = world.getBlockAt(x, y, z);
        b.setType(mat);
        Stairs matStairs = new Stairs(mat);
        matStairs.setFacingDirection(dir);
        b.setData(matStairs.getData());
    }
    

	@SuppressWarnings("unused")
	private void replaceSolidWithAir(Extent e) {
		for (int x = e.minx; x <= e.maxx; x++) {
			for (int y = e.miny; y <= e.maxy; y++) {
				for (int z = e.minz; z <= e.maxz; z++) {
					Block b = world.getBlockAt(x, y, z);
					if (b.getType().isSolid()) {
						b.setType(Material.AIR);
						b.setData((byte) 0);
					}
				}
			}
		}
	}

	public static boolean requiresLight(int x, int y, int z) {
		Block b = Castle.getInstance().getWorld().getBlockAt(x, y, z);
		return b.getLightLevel() < 7;
	}

	public void raze() {
		for (Building r : buildings) {
			fill(r.getExtent(), Material.AIR, 0);
			fill(r.getExtent().getWall(Direction.DOWN), Material.GRASS, 0);
		}
		buildings.clear();
	}

	public Building getRandomBuilding() {
		if (buildings.size() == 0)
			return null;
		else
			return buildings.get(r.nextInt(buildings.size()));
	}

	/**
	 * add a room to both the room list and the rooms-by-chunk map
	 * 
	 * @param r
	 */
	public void addRoom(Room r) {
		GormPlugin.log("Adding room " + Integer.toString(r.id)
				+ " with extent " + r.getExtent().toString());
		rooms.add(r);
		for (int c : r.getChunks()) {
			Collection<Room> list;
			if (!roomsByChunk.containsKey(c)) {
				list = new ArrayList<Room>();
				roomsByChunk.put(c, list);
			} else
				list = roomsByChunk.get(c);
			list.add(r);
		}
	}

	/**
	 * This will return null if a chunk isn't in the map yet
	 * 
	 * @return
	 */
	public Collection<Room> getRoomsByChunk(int key) {
		return roomsByChunk.get(key);
	}

	public Collection<Room> getRooms() {
		return rooms;
	}

	/**
	 * make sure the rooms list is sorted by number of exits
	 */
	public void sortRooms() {
		Collections.sort(rooms);
	}

	/**
	 * Useful shortcut for getting a block in the world.
	 * 
	 * @param pos
	 * @return
	 */
	public Block getBlockAt(IntVector pos) {
		return world.getBlockAt(pos.x, pos.y, pos.z);
	}

	public void roomSanityCheck() {
		for (int i = 0; i < Room.idCounter; i++) {
			// GormPlugin.log("looking for room "+Integer.toString(i));
			boolean found = false;
			for (Room r : rooms) {
				if (r.id == i) {
					found = true;
					break;
				}
			}
			if (!found)
				GormPlugin.log("ROOM NOT FOUND IN MAIN LIST: "
						+ Integer.toString(i));

			found = false;
			outer: for (Collection<Room> col : roomsByChunk.values()) {
				for (Room r : rooms) {
					if (r.id == i) {
						found = true;
						break outer;
					}
				}
			}
			if (!found)
				GormPlugin.log("ROOM NOT FOUND IN CHUNK LISTS: "
						+ Integer.toString(i));

		}
	}

	public Map<Profession, Integer> getDenizenCounts() {
		return denizenCounts;
	}

	public void incDenizenCount(Profession p) {
		denizenCounts.put(p, denizenCounts.get(p) + 1);

	}

}
