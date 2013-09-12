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
	private Material mat = Material.SMOOTH_BRICK;
	private Random r = new Random();

	int cur = 0;
	private String string;

	private int mode = CHECKWRITE;
	private boolean aborted;
	private int loopPoint = -1;
	/**
	 * Stack of values for the internal maths/condition system
	 */
	private boolean status; // status variable for conditions
	private boolean lastMoveCausedTurn; // true if the last follow we did caused
										// a turn

	private boolean isModeFlag(int f) {
		return (mode & f) != 0;
	}

	// flags
	static final int FOLLOW = 1; // follow walls if we hit them
	static final int SUPPORTFOLLOW = 2; // edge following on (build direction
										// will turn to ensure support under
										// built blocks)
	static final int LEFT = 4; // following will try left turn first
	static final int RANDOM = 8; // following will try either left or right
									// (overrides LEFT)
	static final int HUGEDGE = 16; // we must always keep a wall to our left or
									// right, and follow them round
	static final int WRITEONMOVE = 32; // write at end of every move
	static final int CHECKWRITE = 64; // abort if write would cause overwrite of
										// non-air block (on by default)
	static final int BACKSTAIRS = 128; // write stairs as facing away from us

	char getNext() {
		if (cur >= string.length())
			return 0;
		else {
			char c = string.charAt(cur++);
			GormPlugin.log("Next char: " + Character.toString(c));
			return c;
		}
	}

	public Turtle(World  w, IntVector initpos, IntVector.Direction initdir) {
		world = w;
		dir = initdir.vec;
		pos = initpos;
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

	private void moveForwards() {
		pos = pos.add(dir);
		IntVector tmpDir = dir;
		if (isModeFlag(FOLLOW))
			follow();
		if (isModeFlag(HUGEDGE))
			hugEdge();
		if (isModeFlag(WRITEONMOVE))
			write();
		lastMoveCausedTurn = !tmpDir.equals(dir);

	}

	private void execute(char c) {
		switch (c) {
		case 'f': // forward
			moveForwards();
			break;
		case 'L': // move right (caps to distinguish from rotating!)
			dir = dir.rotate(3);
			moveForwards();
			dir = dir.rotate(1);
			break;
		case 'R':// move left (caps to distinguish from rotating!)
			dir = dir.rotate(1);
			moveForwards();
			dir = dir.rotate(3);
			break;
		case 'B': // move backwards
			dir = dir.rotate(2);
			moveForwards();
			dir = dir.rotate(2);
			break;
		case 'r': // turn right
			dir = dir.rotate(1);
			break;
		case 'l': // turn left
			dir = dir.rotate(3);
			break;
		case 'u':// go down
			pos = pos.add(0, 1, 0);
			if (isModeFlag(WRITEONMOVE))
				write();
			break;
		case 'd':// go up
			pos = pos.add(0, -1, 0);
			if (isModeFlag(WRITEONMOVE))
				write();
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
		case ':': // mark the repeat point - we loop back here until we abort
			loopPoint = cur;
			break;
		case '?': // condition - set the flag 
			checkCondition(getNext());
			break;
		case '(': // if flag is false, jump forwards to ')'
			if(!status){
				while(getNext()!=')');
			}
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

	private void setMaterial(char next) {

		switch (next) {
		case 'w':
			mat = Material.WOOD;
			break;
		case 'l':
			mat = Material.LOG;
			break;
		case 'b':
			mat = Material.SMOOTH_BRICK;
			break;
		case 's':
			mat = Material.SMOOTH_STAIRS;
			break;
		case 'L':
			mat = Material.LAPIS_BLOCK; // for fun
			break;
		default:
		case 'a':
			mat = Material.AIR;
			break;
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
	 * Look at the block relative to the turtle - in turtle space - and tell
	 * whether it's empty
	 * 
	 * @param x
	 * @param z
	 * @return
	 */
	private boolean isEmpty(int x, int y, int z) {
		IntVector v = new IntVector(x, y, z).rotateToSpace(dir);
		IntVector p = pos.add(v);
		Block b = world.getBlockAt(p.x, p.y, p.z);
		// GormPlugin.log("  block at "+v.toString()+" is "+b.getType().toString());

		return b.isEmpty();
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

	private Block get() {
		return world.getBlockAt(pos.x, pos.y, pos.z);
	}

	private void write() {
		if (!isModeFlag(CHECKWRITE) || isEmpty(0, 0, 0)) {
			Block b = get();
			b.setType(mat);

			if (Castle.isStairs(mat)) { // if we're writing stairs, they are
										// facing
										// upwards away from us
				Stairs s = new Stairs(mat);
				IntVector d = isModeFlag(BACKSTAIRS) ? dir.negate() : dir;
				s.setFacingDirection(d.toBlockFace());
				b.setData(s.getData());
			}
		} else
			abort();
	}
}
