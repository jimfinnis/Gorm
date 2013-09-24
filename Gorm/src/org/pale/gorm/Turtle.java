package org.pale.gorm;

import java.util.Random;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.material.Stairs;

/**
 * An attempt to write a class for building things independent of orientation,
 * with concise command strings.
 * 
 * @author white
 * 
 */
public class Turtle {
	private World world;
	private IntVector pos;
	private IntVector dir;
	private Material mat = Material.STAINED_CLAY; // default is purple clay
	private int data = 11;
	private Random r = new Random();
	private int mode = CHECKWRITE; // by default, we abort if we're going
									// overwrite solid

	int cur = 0;
	private String string;

	private boolean aborted;
	private int loopPoint = -1;
	/**
	 * Stack of values for the internal maths/condition system
	 */
	private boolean status; // status variable for conditions
	private boolean lastMoveCausedTurn; // true if the last follow we did caused
	private MaterialManager mgr;

	// a turn

	public boolean isModeFlag(int f) {
		return (mode & f) != 0;
	}

	public void setModeFlag(int f) {
		mode |= f;
	}

	public void clrModeFlag(int f) {
		mode &= ~f;
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

	char getNext() {
		if (cur >= string.length())
			return 0;
		else {
			char c = string.charAt(cur++);
			GormPlugin.log("Next char: " + Character.toString(c));
			return c;
		}
	}

	public Turtle(MaterialManager mgr, World w, IntVector initpos,
			Direction initdir) {
		world = w;
		dir = initdir.vec;
		pos = initpos;
		this.mgr = mgr;
	}

	/**
	 * Clone a turtle - useful for effectively doing a save/restore!
	 * 
	 * @param t
	 */
	public Turtle(Turtle t) {
		world = t.world;
		dir = t.dir;
		pos = t.pos;
		mat = t.mat;
		mode = t.mode;
		this.mgr = t.mgr;
	}

	public void run(String s) {
		char c;
		string = s;
		aborted = false;
		dumpMap();
		for (;;) {
			c = getNext();
			if (c == 0) {
				if (loopPoint >= 0) {
					cur = loopPoint;
					c = getNext();
				} else
					return;
			}
			if (aborted)
				return;
			execute(c);
		}
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
		dir.rotate(turns);
	}

	public void setMaterial(Material m) {
		mat = m;
		data = 0;
	}

	public void setMaterial(Material m, int d) {
		mat = m;
		data = d;
	}

	public void setMaterial(MaterialManager.MaterialDataPair mp) {
		setMaterial(mp.m, mp.d);
	}

	public Block get() {
		return world.getBlockAt(pos.x, pos.y, pos.z);
	}

	public boolean write() {
		if (!isModeFlag(CHECKWRITE) || isEmpty(0, 0, 0)) {
			Block b = get();
			if (Castle.isStairs(mat)) { // if we're writing stairs, they are
										// facing
										// upwards away from us
				Stairs s = new Stairs(mat);
				IntVector d = isModeFlag(BACKSTAIRS) ? dir.negate() : dir;
				s.setFacingDirection(d.toBlockFace());
				b.setData(s.getData());
			}
			else
				b.setData((byte) data);

			b.setType(mat);

			return true;
		} else
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
			setMaterial(getNext());
			break;
		case 'M':
			setMaterialDirect(getNext());
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
		default:break;
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
		}
		return 0;
	}

	/**
	 * set material via material manager
	 * 
	 * @param next
	 */
	private void setMaterial(char next) {
		switch (next) {
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
	 * 
	 * @param next
	 */
	private void setMaterialDirect(char next) {
		switch (next) {
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
		default:
			setMaterial(Material.STAINED_CLAY, 14);// red clay error code!
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
		GormPlugin.log("following: dir " + dir.toString());

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
			GormPlugin.log("follow left failed");
			return false;
		} else {
			dir = dir.rotate(3); // or turn left
			GormPlugin.log("follow left OK");
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
			GormPlugin.log("follow right failed");
			return false;
		} else {
			dir = dir.rotate(1); // or turn right
			GormPlugin.log("follow right OK");
			return true;
		}
	}

	/**
	 * We're one step in front of a wall - we need to turn to avoid it
	 */
	private boolean followInnerLeft() {
		if (isEmpty(-1, 0, 0)) {
			GormPlugin.log("inner follow left OK");
			dir = dir.rotate(3);
			return true;
		} else {
			GormPlugin.log("inner follow left failed");
			dumpMap();
			return false;
		}
	}

	/**
	 * We're one step in front of a wall - we need to turn to avoid it
	 */
	private boolean followInnerRight() {
		if (isEmpty(1, 0, 0)) {
			GormPlugin.log("inner follow right OK");
			dir = dir.rotate(1);
			return true;
		} else {
			GormPlugin.log("inner follow right failed");
			dumpMap();
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
		Block b = world.getBlockAt(p.x, p.y, p.z);
		// GormPlugin.log("  block at "+v.toString()+" is "+b.getType().toString());

		return !b.getType().isSolid();
	}

	/**
	 * End the run
	 */
	private void abort() {
		GormPlugin.log("aborted");
		dumpMap();
		aborted = true;
	}

	/**
	 * Useful debugging; print 5x5 map relative to the turtle = X is solid, _ is
	 * empty.
	 */
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
