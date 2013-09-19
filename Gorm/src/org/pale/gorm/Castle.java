package org.pale.gorm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
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

	private List<Room> rooms = new ArrayList<Room>();

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
		if (m == Material.SMOOTH_BRICK || m == Material.WOOD
				|| m == Material.BRICK || m == Material.LAPIS_BLOCK
				|| m == Material.GOLD_BLOCK)
			return false;

		// check for 'inside' air which we can't overwrite
		if (m == Material.AIR && b.getData() == 1)
			return false;
		return true;
	}

	/**
	 * Fill an area with bricks, adding moss and cracks.
	 * 
	 * @param e
	 * @param checkOverwrite
	 */
	public void fillBrickWithCracksAndMoss(Extent e, boolean checkOverwrite) {
		GormPlugin.log("filling " + e.toString());
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

	/**
	 * Return a list of rooms within a given extent
	 * 
	 * @return
	 */
	public Collection<Room> getRoomsIntersecting(Extent e) {
		ArrayList<Room> out = new ArrayList<Room>();

		for (Room r : rooms) {
			if (r.intersects(e))
				out.add(r);
		}
		return out;
	}

	/**
	 * Returns true if an extent overlaps an exit
	 * 
	 * @param exit
	 * @return
	 */
	public boolean overlapsExit(Extent x) {
		for (Room r : rooms) {
			if (r.overlapsExit(x))
				return true;
		}
		return false;
	}

	private void setStairs(int x, int y, int z, BlockFace dir, Material mat) {
		Block b = world.getBlockAt(x, y, z);
		Stairs s = new Stairs(mat);
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
	 * Attempt to run out from an extent representing a hole in a wall, dropping
	 * stairs until we hit the floor. We start one step out from the floor block
	 * of the exit.
	 * 
	 * @param e
	 * @param d
	 * @return
	 */
	private boolean dropExitStairs(Extent e, Direction d) {
		IntVector v = d.vec;
		int floory = e.miny; // get the floor of the exit

		e = e.growDirection(d, 2); // stretch out the extent for scanning
									// purposes
		e = e.growDirection(d.opposite(), 1); // and in a bit as well.
		e.miny -= 100; // so that getHeightWithin() will read heights lower than
						// the current floor

		boolean done = false;
		// we need to iterate over the 'stripes' within a multi-block-wide exit
		if (v.x == 0) {
			// the north-south case
			for (int x = e.minx; x <= e.maxx; x++) {
				int startz = v.z > 0 ? e.minz : e.maxz;
				IntVector start = new IntVector(x, floory, startz);
				done |= stairScan(start, e, v);
			}
		} else {
			// the north-south case
			for (int z = e.minz; z <= e.maxz; z++) {
				int startx = v.x > 0 ? e.minx : e.maxx;
				IntVector start = new IntVector(startx, floory, z);
				done |= stairScan(start, e, v);
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
	private boolean stairScan(IntVector start, Extent e, IntVector v) {
		boolean done = false;
		BlockFace bf = new IntVector(-v.x, 0, -v.z).toBlockFace();
		Collection<BlockState> undoBuffer = new ArrayList<BlockState>();
		
		int y = e.getHeightWithin(start.x, start.z);
		Material mat = Material.WOOD_STAIRS;
		
		// steps starting from blind walls can happen.. but are stupid.
		IntVector startWall = start.subtract(v).add(0,1,0);
		if(world.getBlockAt(startWall.x,startWall.y,startWall.z).getType().isSolid())
			return false;
		
		for (IntVector pt = new IntVector(start);
				e.contains(pt.x, e.miny, pt.z); pt = pt.add(v)) {
			int newy = e.getHeightWithin(pt.x, pt.z);
			if (isStairs(world.getBlockAt(pt.x, newy, pt.z).getType()))
				break;
			if (newy > y) {
				// if we've gone UP, exit the loop. We only work down.
				break;
			} else if (newy < y) {
				GormPlugin.log(String.format(
						"height was %d, is now %d - filling", y, newy));
				// we've gone down, so build up, saving the old block state
				undoBuffer.add(world.getBlockAt(pt.x,y,pt.z).getState());
				setStairs(pt.x, y, pt.z, bf, mat);
				
				Block b = world.getBlockAt(pt.x,y-1,pt.z); // check we landed!
				if(b.getType().isSolid())
					done = true;
				mat = Material.SMOOTH_STAIRS;
				y--; // and go down again!
			}
		}
		
		if(!done){
			// didn't achieve what we wanted, so restore all blocks
			for(BlockState s: undoBuffer){
				// for some reason update() isn't working for me.
				world.getBlockAt(s.getLocation()).setType(s.getType());
			}
		}
		return done;
	}

	/**
	 * Attempt to build stairs down and connect to nearby rooms. Can be run on
	 * any exit several times.
	 * 
	 * @param e
	 * @param d
	 */

	public void postProcessExit(Exit e) {
		
		replaceSolidWithAir(e.getExtendedExtent());
		
		Extent extent = e.getExtent();
		Direction d = e.getDirection();
		if (!dropExitStairs(extent, d)) {
			// if we didn't do any stairs magic one way, try the other way.
			dropExitStairs(extent, d.opposite());
		}
	}

	private void replaceSolidWithAir(Extent e) {
		for (int x = e.minx; x <= e.maxx; x++) {
			for (int y = e.miny; y <= e.maxy; y++) {
				for (int z = e.minz; z <= e.maxz; z++) {
					Block b = world.getBlockAt(x, y, z);
					b.setType(Material.AIR);
					b.setData((byte) 0);
				}
			}
		}		
	}

	/**
	 * Post process the exits whose rooms intersect a given extent, try to
	 * make exits link up with steps or bridges and clear any dross.
	 */
	public void postProcessExitsForRoomsIntersecting(Extent e) {
		for (Room r : getRoomsIntersecting(e)) {
			for (Exit exit : r.getExits()) {
				GormPlugin.log("processing exit");
				postProcessExit(exit);
			}
		}

	}

	/**
	 * This will decorate an exit - does nowt as yet
	 * 
	 * @param e
	 */
	public void decorateExit(Exit e) {
		GormPlugin.log("decorate exit does nothing");
		int qq = e.getDirection().vec.x == 0 ? Extent.Y | Extent.X : Extent.Y
				| Extent.Z;
		Extent x = e.getExtent().expand(1, qq);
		fill(x, Material.DIAMOND_BLOCK, 0);
		fill(e.getExtent(), Material.AIR, 1);
	}

	public void raze() {
		for (Room r : rooms) {
			fill(r.getExtent(), Material.AIR, 0);
			fill(r.getExtent().getWall(Direction.DOWN), Material.GRASS, 0);
			for (Exit x : r.getExits()) {
				fill(x.getExtent(), Material.AIR, 0);
			}
		}
		rooms.clear();
	}
}
