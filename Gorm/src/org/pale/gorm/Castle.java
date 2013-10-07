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

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.material.Stairs;
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
	
	/**
	 * Each chunk has a list of rooms which intersect it. Naturally, a room is likely
	 * to belong to more than one chunk
	 */
	private Map<Integer,Collection<Room>> roomsByChunk = new HashMap<Integer,Collection<Room>>();
	
	/**
	 * Rooms so we can sort by number of exits.
	 */
	private ArrayList<Room> rooms =  new ArrayList<Room>();

	/**
	 * Private ctor so this is a singleton
	 */
	private Castle() {}

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
	void addBuilding(Building r) {
		buildings.add(r);
	}

	/**
	 * returns true if e intersects any room at all. This works
	 * by iterating over the rooms in the chunks for that extent
	 * 
	 * @param e
	 * @return
	 */
	public boolean intersects(Extent e) {
		Set<Integer> chunks = e.getChunks();
		for(int c: chunks){
			Collection<Room> list = getRoomsByChunk(c);
			if(list!=null){
				for(Room r: list){
					if(r.getExtent().intersects(e))
						return true;
				}
			}
		}
		return false;
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
		if (mat == Material.SMOOTH_BRICK && data==0) {// if this is plain brick, flash it up!
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
	
	public void checkFill(Extent e, MaterialDataPair mp){
		checkFill(e,mp.m,mp.d);
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

	public void replace(Extent e, Material mat, int data, Material matReplace, int dataReplace) {
		if (mat == Material.SMOOTH_BRICK && data==0) {// if this is plain brick, flash it up!
			fillBrickWithCracksAndMoss(e, true);
		} else {
			for (int x = e.minx; x <= e.maxx; x++) {
				for (int y = e.miny; y <= e.maxy; y++) {
					for (int z = e.minz; z <= e.maxz; z++) {
						Block b = world.getBlockAt(x, y, z);
						if (b.getType() == matReplace){
							if (b.getData() == (byte) dataReplace){
								b.setType(mat);
								b.setData((byte) data);
							}
						}
					}
				}
			}
		}
	}
	
	public void replace(Extent e, MaterialDataPair mp, MaterialDataPair mpReplace){
		replace(e,mp.m,mp.d,mpReplace.m,mpReplace.d);
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
		if (mat == Material.SMOOTH_BRICK && data==0) // if this is plain brick, flash it up!
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
	
	public void fill(Extent e, MaterialDataPair mp){
		fill(e,mp.m,mp.d);
	}

	/**
	 * We should use this when we might overwrite a blg or exit, to prevent
	 * that.
	 * 
	 * @param b
	 * @return
	 */
	public static boolean canOverwrite(Block b) {
		Material m = b.getType();
		// this is going to be hideous if they remove getData().
		byte data = b.getData();
		// materials walls can be made of
		if (m == Material.SMOOTH_BRICK || m == Material.WOOD
				|| m == Material.BRICK || m==Material.COBBLESTONE || (m==Material.SANDSTONE && data!=0)
				|| isStairs(m))
			return false;

		// check for 'inside' air which we can't overwrite
		if (m == Material.AIR && data == 1)
			return false;
		return true;
	}

	/**
	 * Fill an area with bricks, adding moss and cracks.
	 * 
	 * @param e
	 * @param checkOverwrite
	 */
	private void fillBrickWithCracksAndMoss(Extent e, boolean checkOverwrite) {
		int t = 0;
		int dataVals[] = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 2, 2, 1, 2 };
		for (int x = e.minx; x <= e.maxx; x++) {
			for (int y = e.miny; y <= e.maxy; y++) {
				for (int z = e.minz; z <= e.maxz; z++) {
					Block b = world.getBlockAt(x, y, z);
					if (!checkOverwrite || canOverwrite(b)) {
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

	public void setStairs(int x, int y, int z, BlockFace dir, Material mat) {
		Block b = world.getBlockAt(x, y, z);
		Stairs s = new Stairs(mat);
		s.setFacingDirection(dir);
		b.setData(s.getData());
		b.setType(s.getItemType());
	}

	static boolean isStairs(Material m) {
		return (m == Material.COBBLESTONE_STAIRS || m == Material.WOOD_STAIRS
				|| m == Material.BIRCH_WOOD_STAIRS || m == Material.SANDSTONE_STAIRS
				|| m == Material.SMOOTH_STAIRS || m == Material.BRICK_STAIRS || m==Material.QUARTZ_STAIRS
				|| m == Material.NETHER_BRICK_STAIRS || m == Material.JUNGLE_WOOD_STAIRS || m==Material.SPRUCE_WOOD_STAIRS
				);
	}

	/**
	 * Attempt to run out from an extent representing a hole in a wall, dropping
	 * stairs until we hit the floor. We start one step out from the floor block
	 * of the exit.
	 * 
	 * @param e
	 * @param d
	 * @return
	 */
	private boolean dropExitStairs(MaterialManager mgr,Extent e, Direction d) {

		IntVector v = d.vec;
		int floory = e.miny; // get the floor of the exit

		e = e.growDirection(d, 5); // stretch out the extent for scanning
									// purposes

		e.miny -= 100; // so that getHeightWithin() will read heights lower than
						// the current floor

		boolean done = false;
		// we need to iterate over the 'stripes' within a multi-block-wide exit
		if (v.x == 0) {
			// the north-south case
			for (int x = e.minx; x <= e.maxx; x++) {
				int startz = v.z > 0 ? e.minz : e.maxz;
				IntVector start = new IntVector(x, floory, startz);
				done |= stairScan(mgr,start, e, v);
			}
		} else {
			// the north-south case
			for (int z = e.minz; z <= e.maxz; z++) {
				int startx = v.x > 0 ? e.minx : e.maxx;
				IntVector start = new IntVector(startx, floory, z);
				done |= stairScan(mgr,start, e, v);
			}
		}

		return done;
	}

	/**
	 * Starting at a point, scan along a given vector within the given extent,
	 * filling in drops with stairs as necessary. Return true if we did
	 * anything.
	 * 
	 * @param start
	 * @param e
	 * @param v
	 * @return
	 */
	private boolean stairScan(MaterialManager mgr, IntVector start, Extent e, IntVector v) {
		boolean done = false;
		BlockFace bf = new IntVector(-v.x, 0, -v.z).toBlockFace();
		Collection<BlockState> undoBuffer = new ArrayList<BlockState>();

		int y = e.getHeightWithin(start.x, start.z);
		Material mat = mgr.getStair();

		// steps starting from blind walls can happen.. but are stupid.
		IntVector startWall = start.subtract(v).add(0, 1, 0);
		if (world.getBlockAt(startWall.x, startWall.y, startWall.z).getType()
				.isSolid())
			return false;

		for (IntVector pt = new IntVector(start); e
				.contains(pt.x, e.miny, pt.z); pt = pt.add(v)) {
			int newy = e.getHeightWithin(pt.x, pt.z);
			if (isStairs(world.getBlockAt(pt.x, newy, pt.z).getType()))
				break;
			if (newy > y) {
				// if we've gone UP, exit the loop. We only work down.
				break;
			} else if (newy < y) {
				// we've gone down, so build up, saving the old block state
				undoBuffer.add(world.getBlockAt(pt.x, y, pt.z).getState());
				setStairs(pt.x, y, pt.z, bf, mat);

				Block b = world.getBlockAt(pt.x, y - 1, pt.z); // check we
																// landed!
				if (b.getType().isSolid())
					done = true;
				y--; // and go down again!
			}
		}

		if (!done) {
			// didn't achieve what we wanted, so restore all blocks
			for (BlockState s : undoBuffer) {
				// for some reason update() isn't working for me.
				world.getBlockAt(s.getLocation()).setType(s.getType());
			}
		}
		return done;
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


	public void postProcessExit(MaterialManager mgr,Exit e) {
		dropExitStairs(mgr,e.getExtent(), e.getDirection());
		ExitDecorator.decorate(mgr,e);
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
	 * @param r
	 */
	public void addRoom(Room r) {
		GormPlugin.log("Adding room "+Integer.toString(r.id)+" with extent "+r.getExtent().toString());
		rooms.add(r);
		for(int c: r.getChunks()){
			Collection<Room> list;
			if(!roomsByChunk.containsKey(c)) {
				list = new ArrayList<Room>();
				roomsByChunk.put(c, list);
			} else
				list = roomsByChunk.get(c);
			list.add(r);
		}
	}
	
	/**
	 * This will return null if a chunk isn't in the map yet
	 * @return
	 */
	public Collection<Room> getRoomsByChunk(int key){
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
	 * @param pos
	 * @return
	 */
	public Block getBlockAt(IntVector pos) {
		return world.getBlockAt(pos.x,pos.y,pos.z);
	}
}
