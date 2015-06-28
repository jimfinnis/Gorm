package org.pale.gorm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

//SNARK import net.minecraft.server.v1_7_R3.TileEntity;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.EntityType;
import org.bukkit.material.Bed;
import org.bukkit.material.Sign;
import org.bukkit.material.Stairs;
import org.bukkit.material.MaterialData;
import org.bukkit.material.FlowerPot;
import org.pale.gorm.roomutils.DungeonObjects;
import org.pale.gorm.GormPlugin;

/**
 * An attempt to write a class for building things independent of orientation,
 * with concise command strings.
 * 
 * @author white
 * 
 */
public class Turtle {
	/**
	 * This interface is used for custom writers, which are invoked up with the
	 * Mn command where n is a number. There can be 10 of them.
	 * 
	 * @author white
	 * 
	 */
	public interface TurtleWriter {
		abstract void write(Turtle t, IntVector loc, IntVector dir);
	}

	private Castle castle;
	private Map<Character, TurtleWriter> customWriters = new HashMap<Character, TurtleWriter>();
	private World world;
	private IntVector pos;
	private IntVector dir;
	private Material mat = Material.STAINED_CLAY; // default is purple clay
	private int data = 11;
	private Random r = new Random();
	private int mode = CHECKWRITE; // by default, we abort if we're going
									// overwrite solid

	/**
	 * The custom writer we are currently using, or null if we're using a
	 * standard material
	 */
	private TurtleWriter currentCustomWriter = null;

	int cur = 0;
	private String string;

	private boolean aborted;
	private int loopPoint = -1;
	/**
	 * Stack of values for the internal maths/condition system
	 */
	private boolean status; // status variable for conditions
	private boolean lastMoveCausedTurn; // true if the last follow we did caused
										// a turn
	private MaterialManager mgr;
	private int testScore = 0; // score used in 'testing' squares, returned from
								// getTestScore()
	private Room containingRoom = null;

	/**
	 * Set a custom writer which can later be invoked with 'Mn' where n is a
	 * number
	 */
	public Turtle setWriter(char c, TurtleWriter w) {
		customWriters.put(c, w);
		return this;
	}

	public boolean isModeFlag(int f) {
		return (mode & f) != 0;
	}

	public Turtle setModeFlag(int f) {
		mode |= f;
		return this;
	}

	public Turtle clrModeFlag(int f) {
		mode &= ~f;
		return this;
	}

	public Turtle setPosition(IntVector p) {
		pos = new IntVector(p);
		return this;
	}

	public Turtle setDirection(IntVector d) {
		dir = d;
		return this;
	}

	// flags
	public static final int FOLLOW = 1; // follow walls if we hit them
	public static final int SUPPORTFOLLOW = 2; // edge following on (build
												// direction
	// will turn to ensure support under
	// built blocks)
	public static final int LEFT = 4; // following will try left turn first
	public static final int RANDOM = 8; // following will try either left or
										// right
	// (overrides LEFT)
	public static final int HUGEDGE = 16; // we must always keep a wall to our
											// left or
	// right, and follow them round
	public static final int WRITEONMOVE = 32; // write at end of every move
	public static final int CHECKWRITE = 64; // abort if write would cause
												// overwrite of
	// non-air block (on by default)
	public static final int BACKSTAIRS = 128; // write stairs as facing away
												// from us

	public static final int TEST = 256; // test mode only - do not write, just
										// check we can and abort if not
	public static final int NOTINDOORS = 512; // only write if we're not in a
												// room - but don't abort on
												// failure
	public static final int ABORTINDOORS = 1024; // we abort if we try to write
													// in a room
	// ensure that all blocks placed at the starting height have something under
	// them
	// in test mode
	public static final int ENSUREFLOORSUPPORT = 2048;
	
	// debugging
	public static final int DEBUG = 4096;

	/**
	 * An extent containing all the blocks written by this turtle
	 */
	private Extent written = new Extent();
	/**
	 * Where this turtle started, used for ENSUREFLOORSUPPORT
	 */
	private IntVector startingPos;

	/**
	 * Get the extent of all the blocks written
	 * 
	 * @return
	 */
	public Extent getWritten() {
		return new Extent(written);
	}

	/**
	 * Clear the extent of all blocks written
	 * 
	 * @return
	 */
	public void clearWritten() {
		written = new Extent();
	}

	char getNext() {
		if (cur >= string.length())
			return 0;
		else {
			char c = string.charAt(cur++);
			return c;
		}
	}

	/**
	 * Get the test score, generated by 'T' commands
	 */
	public int getTestScore() {
		return testScore;
	}

	/**
	 * Reset the test score
	 */
	public void resetTestScore() {
		testScore = 0;
	}

	public Turtle(MaterialManager mgr, World w, IntVector initpos,
			Direction initdir) {
		world = w;
		castle = Castle.getInstance();
		dir = initdir.vec;
		pos = initpos;
		this.startingPos = new IntVector(pos);
		this.mgr = mgr;

		// here we set up the custom 'C' materials - e.g. Cc is the chest.
		setWriter('c', new TurtleWriter() {
			@Override
			public void write(Turtle t, IntVector v, IntVector dir) {
				DungeonObjects.chest(v, dir);
			}
		});
		setWriter('s', new TurtleWriter() {
			@Override
			public void write(Turtle t, IntVector loc, IntVector dir) {
				EntityType[] ents = { EntityType.CAVE_SPIDER, EntityType.WITCH,
						EntityType.SKELETON, EntityType.SPIDER,
						EntityType.ZOMBIE };
				DungeonObjects.spawner(loc,
						ents[Castle.getInstance().r.nextInt(ents.length)]);
			}
		});
		setWriter('p', new TurtleWriter() {
			@Override
			public void write(Turtle t, IntVector loc, IntVector dir) {
                          Block b = t.castle.getBlockAt(loc);
                          b.setType(Material.FLOWER_POT);
                          
                          // this doesn't work at the moment.
                          FlowerPot p = new FlowerPot(Material.FLOWER_POT);
                          p.setContents(new MaterialData(Material.RED_ROSE));
                          b.setData(p.getData());
                      }
                          
                      });
	}

	/**
	 * Clone a turtle - useful for effectively doing a save/restore!
	 * 
	 * @param t
	 */
	public Turtle(Turtle t) {
		castle = t.castle;
		world = t.world;
		dir = t.dir;
		pos = t.pos;
		mat = t.mat;
		mode = t.mode;
		this.mgr = t.mgr;
	}

	/**
	 * Run a string
	 * 
	 * @param s
	 * @return true if completed, false if aborted
	 */
	public boolean run(String s) {
		char c;
		int maxCount = 100; // only allow this number of instructions!
		string = s;
		aborted = false;
		// dumpMap();
		for (;;) {
			c = getNext();
			if (c == 0) {
				if (loopPoint >= 0) {
					cur = loopPoint;
					c = getNext();
				} else
					return true;
			}
			if (aborted)
				return false;
			execute(c);
			if (--maxCount == 0)
				return false;
		}
	}

	/**
	 * Set a room which the turtle must not write outside. This is done by
	 * considering blocks outside the room to be solid, so CHECKWRITE being off
	 * will override this. Blocked areas within the room are also considered.
	 * 
	 * @param e
	 */
	public Turtle setRoom(Room r) {
		containingRoom = r;
		return this;
	}

	public void forwards() {
		pos = pos.add(dir);
		IntVector tmpDir = dir;
		if (isModeFlag(FOLLOW))
			follow();
		if (isModeFlag(HUGEDGE))
			hugEdge();
		if (isModeFlag(SUPPORTFOLLOW))
			supportFollow();
		if (isModeFlag(WRITEONMOVE))
			write();
		lastMoveCausedTurn = !tmpDir.equals(dir);
	}

	/**
	 * Move, not turn. Follow rules will not apply.
	 */
	public void right() {
		pos = pos.add(dir.rotate(1));
	}

	/**
	 * Move, not turn. Follow rules will not apply.
	 */
	public void left() {
		pos = pos.add(dir.rotate(3));

	}

	public void moveAbsolute(IntVector v) {
		pos = v;
	}

	public IntVector getPos() {
		return pos;
	}

	public void rotate(int turns) {
		dir = dir.rotate(turns);
	}

	public Turtle setMaterial(Material m) {
		mat = m;
		data = 0;
		return this;
	}

	public Turtle setMaterial(Material m, int d) {
		mat = m;
		data = d;
		return this;
	}

	public Turtle setMaterial(MaterialDataPair mp) {
		setMaterial(mp.m, mp.d);
		return this;
	}

	public Block get() {
		return world.getBlockAt(pos.x, pos.y, pos.z);
	}

	public boolean write() {
		boolean rv;
		
		if (!isModeFlag(CHECKWRITE) || isEmpty(0, 0, 0)) {
			if (isModeFlag(ENSUREFLOORSUPPORT) && !isSupportedByFloor())
				rv = false;
			else if (isModeFlag(TEST))
				rv = true;
			else if (containingRoom != null && containingRoom.isBlocked(pos))
				rv = false;
			else if (isModeFlag(NOTINDOORS) && isIndoors())
				rv = false;
			else if (isModeFlag(ABORTINDOORS) && isIndoors())
				rv = false;
			else {
				Block b = get();

				if (currentCustomWriter != null) {
					currentCustomWriter.write(this, pos, dir);
					return true;
				}

				if (Castle.isStairs(mat)) { // if we're writing stairs, they are
											// facing
											// upwards away from us
					b.setType(mat);
					Stairs s = new Stairs(mat);
					IntVector d = isModeFlag(BACKSTAIRS) ? dir.negate() : dir;
					s.setFacingDirection(d.toBlockFace());
					b.setData(s.getData());
				} else if (mat == Material.WALL_SIGN) {
					b.setType(mat);
					BlockState bs = b.getState();
					Sign s = (Sign) bs.getData();
					s.setFacingDirection(dir.negate().toBlockFace());
					bs.update(true);
				} else if (mat == Material.BED_BLOCK) {
					b.setType(mat);
					BlockState bs = b.getState();
					Bed bed = (Bed) bs.getData();
					bed.setFacingDirection(dir.toBlockFace());
					bed.setHeadOfBed(data == 0);
					bs.update();
				} else {
					b.setData((byte) data);
					b.setType(mat);
				}

				written = written.union(pos);

				rv = true;
			}
		} else {
			rv = false;
		}
		
		if(!rv)
			abort();
		return rv;
	}

	/**
	 * return true if the block at the current position is inside a room
	 * 
	 * @return
	 */
	private boolean isIndoors() {
		int chunk = Extent.getChunkCode(pos.x, pos.z);
		for (Room x : castle.getRoomsByChunk(chunk)) {
			if (x.e.contains(pos)) {
				return true;
			}
		}
		return false;
	}

	public Material getMaterial() {
		return mat;
	}

	public void up() {
		pos = pos.add(0, 1, 0);
		if (isModeFlag(WRITEONMOVE))
			write();
	}

	public void down() {
		pos = pos.add(0, -1, 0);
		if (isModeFlag(WRITEONMOVE))
			write();

	}

	private void execute(char c) {
		switch (c) {
		case 'f': // forward
			forwards();
			break;
		case 'L': // move right (caps to distinguish from rotating!)
			dir = dir.rotate(3);
			forwards();
			dir = dir.rotate(1);
			break;
		case 'R':// move left (caps to distinguish from rotating!)
			dir = dir.rotate(1);
			forwards();
			dir = dir.rotate(3);
			break;
		case 'b': // move backwards
			dir = dir.rotate(2);
			forwards();
			dir = dir.rotate(2);
			break;
		case 'r': // turn right
			dir = dir.rotate(1);
			break;
		case 'l': // turn left
			dir = dir.rotate(3);
			break;
		case 'u':// go down
			up();
			break;
		case 'd':// go up
			down();
			break;
		case 't': // test the block, adding 1 to the score if it is empty
			if (isEmpty(0, 0, 0)) {
				testScore++;
			}
			break;
		case 'T':
			if (!isEmpty(0, 0, 0)) {
				abort();
			}
			break;
		case 'B':
			if (containingRoom != null
					&& (!isModeFlag(CHECKWRITE) || isEmpty(0, 0, 0))) {
				if (!isModeFlag(TEST)) {
					containingRoom.addBlock(new Extent(pos.x, pos.y, pos.z));
					// GormPlugin.log("adding block at "+pos.toString());
				}
			}
			break;
		case 'w': // write the material to the current block - but abort if we
					// are going to overwrite!
			write();
			break;
		case '+':// set mode
			mode |= getNextMode();
			break;
		case '-':// clear mode
			mode &= ~getNextMode();
			break;
		case 'm':
			setMaterial();
			break;
		case 'M':
			setMaterialDirect();
			break;
		case 'C':
			setCustomWriter();
			break;
		case ':': // mark the repeat point - we loop back here until we abort
			loopPoint = cur;
			break;
		case '?': // condition - set the flag
			checkCondition(getNext());
			break;
		case '(': // if flag is false, jump forwards to ')'
			if (!status) {
				while (getNext() != ')')
					;
			}
			break;
		default:
			break;
		}
	}

	private void checkCondition(char c) {
		switch (c) {
		case 't':
			status = lastMoveCausedTurn;
			break;
		default:
			throw new RuntimeException("unknown condition "
					+ Character.toString(c));
		}
	}

	private int getNextMode() {
		switch (getNext()) {
		case 'f':
			return FOLLOW;
		case 's':
			return SUPPORTFOLLOW;
		case 'l':
			return LEFT;
		case '?':
			return RANDOM;
		case 'h':
			return HUGEDGE;
		case 'w':
			return WRITEONMOVE;
		case 'c':
			return CHECKWRITE;
		case 'S':
			return BACKSTAIRS;
		case 't':
			return TEST;
		case 'i':
			return NOTINDOORS;
		case 'I':
			return ABORTINDOORS;
		case 'F':
			return ENSUREFLOORSUPPORT;
		}
		return 0;
	}

	private void setCustomWriter() {
		char c = getNext();
		if (customWriters.containsKey(c))
			currentCustomWriter = customWriters.get(c);
		else
			GormPlugin.log("cannot find custom writer for code " + c);

	}

	/**
	 * set material via material manager
	 */
	private void setMaterial() {
		currentCustomWriter = null;
		switch (getNext()) {
		case '1':
			setMaterial(mgr.getPrimary());
			break;
		case '2':
			setMaterial(mgr.getSecondary());
			break;
		case '3':
			setMaterial(mgr.getSupSecondary());
			break;
		case 'f':
			setMaterial(mgr.getFence());
			break;
		case 's':
			setMaterial(mgr.getStair(), 0);
			break;
		case 'S':
			setMaterial(mgr.getPrimary().toSteps());
			break;
		case 'o':
			setMaterial(mgr.getOrnament());
			break;
		case 'g':
			setMaterial(mgr.getGround());
			break;
		case 'p':
			setMaterial(mgr.getPole());
			break;
		default:
			setMaterial(Material.STAINED_CLAY, 14);// red clay error code!
		}
	}

	/**
	 * set material via a direct abbreviation (try to avoid)
	 */
	private void setMaterialDirect() {
		currentCustomWriter = null;
		char c = getNext();
		switch (c) {
		case 'w':
			mat = Material.WOOD;
			data = 0;
			break;
		case 'f':
			mat = Material.FENCE;
			data = 0;
			break;
		case 'W':
			mat = Material.COBBLE_WALL;
			data = 0;
			break;
		case 'l':
			mat = Material.LOG;
			data = 0;
			break;
		case 'b':
			mat = Material.SMOOTH_BRICK;
			data = 0;
			break;
		case 'c':
			mat = Material.SMOOTH_BRICK;
			data = 3;
			break;
		case 's':
			mat = Material.SMOOTH_STAIRS;
			data = 0;
			break;
		case 'j':
			mat = Material.WOOD_STAIRS;
			data = 0;
			break;
		case 'q':
			mat = Material.WALL_SIGN;
			data = 0;
			break;
		case 'L':
			mat = Material.LAPIS_BLOCK;
			data = 0; // for fun
			break;
		case 't':
			mat = Material.TORCH;
			data = 0;
			break;
		case 'a':
			mat = Material.AIR;
			data = 0;
			break;
		case 'g':
			mat = Material.THIN_GLASS;
			data = 0;
			break;
		case 'G':
			mat = Material.GLASS;
			data = 0;
			break;
		case 'i':
			mat = Material.IRON_FENCE;
			data = 0;
			break;
		case 'B':
			mat = Material.BOOKSHELF;
			data = 0;
			break;
		case 'C':
			mat = Material.CHEST; // EMPTY chest. Use Cc for chests with loot.
			data = 0;
			break;
		case 'D':
			char qq = getNext();
			mat = Material.BED_BLOCK;
			if (qq == '1')
				data = 1;
			else
				data = 0;
			break;
		case 'S':// specials
			switch (getNext()) {
			case 'b':
				setMaterial(Material.BREWING_STAND);
				break;
			case 'a':
				setMaterial(Material.ANVIL);
				break;
			case 'f':
				setMaterial(Material.FURNACE);
				break;
			case 'c':
				setMaterial(Material.CAULDRON);
				break;
			case 't':
				setMaterial(Material.WORKBENCH);
				break;
			default:
				setMaterial(Material.STAINED_CLAY, 14);// red clay error code!
			}
			break;

		default: {
			int n = Character.getNumericValue(c);
			if (n >= 10 || n < 0) {
				GormPlugin.log("cannot decipher turtle direct material code "
						+ c);
				setMaterial(Material.STAINED_CLAY, 14);// red clay error code!
			} else {
				GormPlugin.log("unknown material code " + c);
				setMaterial(Material.STAINED_CLAY, 14);// red clay error code!

			}
		}
		}
	}

	/**
	 * Return true if we try to turn left first when automatically turning
	 * 
	 * @return
	 */
	private boolean tryLeftFirst() {
		// if we are in random mode, it depends on a random number which way we
		// try first,
		// otherwise it depends on the LEFT flag.
		return (!isModeFlag(RANDOM) && isModeFlag(LEFT) || isModeFlag(RANDOM)
				&& r.nextBoolean());

	}

	/**
	 * If we're about to hit a wall, turn to avoid it
	 */
	private void follow() {
		if (!isEmpty(0, 0, -1)) { // ahead is full, we're going to hit a wall -
									// attempt INNER FOLLOW
			if (tryLeftFirst()) {
				// attempt follow left first
				if (!followInnerLeft())
					if (!followInnerRight())
						abort();
			} else {
				// attempt follow left first
				if (!followInnerRight())
					if (!followInnerLeft())
						abort();
			}
		}
	}

	/**
	 * If the left and right of this square are both empty, turn to follow the
	 * wall around. This follow mode allows us to follow outside edges.
	 */
	private void hugEdge() {
		if (isEmpty(-1, 0, 0) && isEmpty(1, 0, 0)) { // left and right are empty
														// - attempt OUTER
														// FOLLOW
														// (i.e. following the
														// outer edge)
			if (tryLeftFirst()) {
				// attempt follow left first
				if (!followOuterLeft())
					if (!followOuterRight())
						abort();
			} else {
				// attempt follow left first
				if (!followOuterRight())
					if (!followOuterLeft())
						abort();
			}
		}
	}

	/**
	 * With nothing on the left or the right, attempt to follow the left hand
	 * wall we just left.
	 * 
	 * @return
	 */
	private boolean followOuterLeft() {
		if (isEmpty(-1, 0, 1)) { // if the spot behind and to the left isn't a
									// wall, fail
			return false;
		} else {
			dir = dir.rotate(3); // or turn left
			return true;
		}
	}

	/**
	 * With nothing on the left or the right, attempt to follow the right hand
	 * wall we just left.
	 * 
	 * @return
	 */
	private boolean followOuterRight() {
		if (isEmpty(1, 0, 1)) { // if the spot behind and to the right isn't a
								// wall, fail
			return false;
		} else {
			dir = dir.rotate(1); // or turn right
			return true;
		}
	}

	/**
	 * We're one step in front of a wall - we need to turn to avoid it
	 */
	private boolean followInnerLeft() {
		if (isEmpty(-1, 0, 0)) {
			dir = dir.rotate(3);
			return true;
		} else {
			// dumpMap();
			return false;
		}
	}

	/**
	 * We're one step in front of a wall - we need to turn to avoid it
	 */
	private boolean followInnerRight() {
		if (isEmpty(1, 0, 0)) {
			dir = dir.rotate(1);
			return true;
		} else {
			// dumpMap();
			return false;
		}
	}

	/**
	 * If the step in front will take us onto an unsupported block, we need to
	 * turn. By default we'll go clockwise.
	 */
	private void supportFollow() {
		if (isEmpty(0, -1, -1)) {
			if (!isEmpty(1, -1, 0))
				dir = dir.rotate(1);
			else if (!isEmpty(-1, -1, 0))
				dir = dir.rotate(3);
			else
				abort();
		}

	}

	/**
	 * Check a location is OK to write
	 * 
	 * @param pos
	 * @return
	 */
	private boolean isLocationEmpty(IntVector p) {
		// check for a containing room
		if (containingRoom != null) {
			if (containingRoom.isBlocked(p)) {
				return false;
			}
		}
		Block b = world.getBlockAt(p.x, p.y, p.z);
		return !b.getType().isSolid();
	}

	/**
	 * Look at the block relative to the turtle - in turtle space - and tell
	 * whether it's empty (i.e. not solid)
	 * 
	 * @param x
	 * @param z
	 * @return
	 */
	public boolean isEmpty(int x, int y, int z) {
		IntVector v = new IntVector(x, y, z).rotateToSpace(dir);
		IntVector p = pos.add(v);
		return isLocationEmpty(p);
	}

	private boolean isSupportedByFloor() {
		IntVector p = new IntVector(pos);
		p.y = startingPos.y - 1;
		Block b = world.getBlockAt(p.x, p.y, p.z);
		return b.getType().isSolid();
	}

	/**
	 * End the run
	 */
	private void abort() {
		// GormPlugin.log("aborted");
		// dumpMap();
		aborted = true;
	}

	/**
	 * Useful debugging; print 5x5 map relative to the turtle = X is solid, _ is
	 * empty.
	 */
	@SuppressWarnings("unused")
	private void dumpMap() {
		for (int z = -2; z <= 2; z++) {
			String foo = String.format("%3d", z);
			for (int x = -2; x <= 2; x++) {
				IntVector v = new IntVector(x, 0, z).rotateToSpace(dir);
				IntVector p = pos.add(v);
				Block b = world.getBlockAt(p.x, p.y, p.z);
				if (b.getType() == Material.LAPIS_BLOCK)
					foo += "L";
				else if (isEmpty(x, 0, z))
					foo += "_";
				else
					foo += "X";
			}
			GormPlugin.log("MAP: " + foo);
		}
	}

}
