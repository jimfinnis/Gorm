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

	private int mode = 0;
	private boolean aborted;
	private int loopPoint=-1;
	static final int FOLLOW = 1; // wall following mode

	char getNext() {
		if (cur >= string.length())
			return 0;
		else {
			char c = string.charAt(cur++);
			GormPlugin.log("Next char: " + Character.toString(c));
			return c;
		}
	}

	public Turtle(Castle c, IntVector initpos, IntVector.Direction initdir) {
		world = c.getWorld();
		dir = initdir.vec;
		pos = initpos;
	}

	public void run(String s) {
		char c;
		string = s;
		aborted = false;
		for(;;){
			c = getNext();
			if(c==0){
				if(loopPoint>=0){
					cur = loopPoint;
					c = getNext();
				}
			}
			if(aborted)return;
			execute(c);
		}
	}

	private void execute(char c) {
		switch (c) {
		case 'f': // forward
			pos = pos.add(dir);
			if ((mode & FOLLOW) != 0)
				follow();
			break;
		case 'r': // turn right
			dir = dir.rotate(1);
			break;
		case 'l': // turn left
			dir = dir.rotate(3);
			break;
		case 'u':// go down
			pos = pos.add(0, 1, 0);
			break;
		case 'd':// go up
			pos = pos.add(0, -1, 0);
			break;
		case 'w': // write the material to the current block - but abort if we are going to overwrite!
			if(isEmpty(0,0,0))
				write();
			else
				abort();
			break;
		case '+':// set mode
			mode |= getNextMode();
			break;
		case '-':// clear mode
			mode &= ~getNextMode();
		case 'm':
			setMaterial(getNext());
			break;
		case ':': // mark the repeat point - we loop back here until we abort
			loopPoint = cur;
			break;
		}
	}

	/**
	 * If the left and right of this square are both empty, turn to follow the
	 * wall around. This follow mode allows us to follow outside edges.
	 */
	private void follow() {
		GormPlugin.log("following: dir "+dir.toString());
		dumpMap();
		if (isEmpty(-1,0,0) && isEmpty(1,0,0)) {
			GormPlugin.log("left and right are empty");
			if (r.nextBoolean()) {
				// attempt follow left first
				if (!followLeft())
					if (!followRight())
						abort();
			} else {
				// attempt follow left first
				if (!followRight())
					if (!followLeft())
						abort();
			}
		}

	}

	/**
	 * Look at the block relative to the turtle - in turtle space - and tell whether it's empty
	 * @param x
	 * @param z
	 * @return
	 */
	private boolean isEmpty(int x,int y,int z) {
		IntVector v = new IntVector(x,y,z).rotateToSpace(dir);
		IntVector p = pos.add(v);
		Block b = world.getBlockAt(p.x, p.y, p.z);
		//GormPlugin.log("  block at "+v.toString()+" is "+b.getType().toString());

		return b.isEmpty();
	}

	/**
	 * End the run
	 */
	private void abort() {
		dumpMap();
		aborted = true;
	}

	/**
	 * Useful debugging; print 5x5 map relative to the turtle = X is solid, _ is empty.
	 */
	private void dumpMap() {
		for(int z=2;z>=-2;z--){
			String foo=String.format("%3d",z);
			for(int x=-2;x<=2;x++){
				IntVector v = new IntVector(x,0,z).rotateToSpace(dir);
				IntVector p = pos.add(v);
				Block b = world.getBlockAt(p.x, p.y, p.z);
				if(b.getType()==Material.LAPIS_BLOCK)
					foo+="L";
				else if(isEmpty(x,0,z))
					foo+="_";
				else
					foo+="X";
			}
			GormPlugin.log("MAP: "+foo);
		}
	}

	/**
	 * With nothing on the left or the right, attempt to follow the left hand
	 * wall we just left.
	 * 
	 * @return
	 */
	private boolean followLeft() {
		if(isEmpty(-1,0,-1)){ // if the spot behind and to the left isn't a wall, fail
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
	private boolean followRight() {
		if(isEmpty(1,0,-1)){ // if the spot behind and to the right isn't a wall, fail
			GormPlugin.log("follow right failed");
			return false;
		} else {
			dir = dir.rotate(1); // or turn right
			GormPlugin.log("follow right OK");
			return true;
		}
	}

	private int getNextMode() {
		switch (getNext()) {
		case 'f':
			return FOLLOW;
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
		default:
		case 'a':
			mat = Material.AIR;
			break;
		}

	}

	private Block get() {
		return world.getBlockAt(pos.x, pos.y, pos.z);
	}

	private void write() {
		Block b = get();
		b.setType(mat);

		if (Castle.isStairs(mat)) { // if we're writing stairs, they are facing
									// upwards away from us
			Stairs s = new Stairs(mat);
			s.setFacingDirection(dir.toBlockFace());
			b.setData(s.getData());
		}
	}
}
