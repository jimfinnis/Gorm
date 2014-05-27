package org.pale.gorm;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

/**
 * Simple vector of integers
 * 
 * @author white
 * 
 */
public class IntVector {
	public int x, y, z;

	public IntVector(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public IntVector(IntVector v) {
		this.x = v.x;
		this.y = v.y;
		this.z = v.z;
	}

	public IntVector(Location loc) {
		x = loc.getBlockX();
		y = loc.getBlockY();
		z = loc.getBlockZ();
	}

	public IntVector add(int x, int y, int z) {
		return new IntVector(this.x + x, this.y + y, this.z + z);
	}

	public IntVector add(IntVector v) {
		return add(v.x, v.y, v.z);
	}

	public IntVector subtract(int x, int y, int z) {
		return new IntVector(this.x - x, this.y - y, this.z - z);
	}

	public IntVector subtract(IntVector v) {
		return subtract(v.x, v.y, v.z);
	}

	public IntVector negate() {
		return new IntVector(-x, -y, -z);
	}

	public IntVector scale(int d) {
		return new IntVector(x * d, y * d, z * d);
	}

	@Override
	public String toString() {
		return String.format("[%d %d %d]", x, y, z);
	}

	/**
	 * Rotate clockwise in XZ plane, given that Z points south.
	 * 
	 * @param turns
	 *            of 90 degs clockwise
	 * @return
	 */
	public IntVector rotate(int turns) {
		IntVector v;
		//GormPlugin.log("rotating "+Integer.toString(turns));
		if(turns<0)
			turns+=4;
		switch (turns) {
		default:
		case 0:
			v = new IntVector(this);
			break;
		case 1:
			v = new IntVector(-z, y, x);
			break;
		case 2:
			v = new IntVector(-x, y, -z);
			break;
		case 3:
			v = new IntVector(z, y, -x);
			break;
		}
		return v;
	}

	/**
	 * rotate a vector to the orientation indicated by the zaxis passed in.
	 * OK, that's clear as mud. What I mean is, generate a rotation such that
	 * the zaxis passed in would be rotated to (0,0,1). 
	 * Note that this produces a rotational system where z axis -ve is IN FRONT of you.
	 * This is a consequence of the z axis pointing south in the world.
	 * No good for up and down vectors.
	 * 
	 * 
	 * @param zaxis
	 * @return
	 */
	public IntVector rotateToSpace(IntVector zaxis) {
		//GormPlugin.log("zaxisvector"+zaxis.toString());
		if (zaxis.x < 0)
			return rotate(3);
		else if (zaxis.x > 0)
			return rotate(1);
		else if (zaxis.z < 0)
			return rotate(0);
		else if (zaxis.z > 0)
			return rotate(2);
		else
			throw new RuntimeException("bad vector in rotateToSpace: "
					+ zaxis.toString());

	}
	
	/**
	 * Yaw to direction - note that it looks a bit weird because of Minecraft's odd south-pointing coord system.
	 * @param f
	 * @return
	 */
	public static Direction yawToDir(float f){
		if(f<0)f+=360;
		if (f > 45 && f <= 135) return Direction.WEST;
		else if (f > 135 && f <= 225) return Direction.NORTH;
		else if (f > 225 && f <= 315) return Direction.EAST;
		else return Direction.SOUTH;
	}

	/**
	 * Overriding equals correctly is really revolting.
	 */
	@Override
	public boolean equals(Object obj) {
		if(obj == null)return false;
		if(obj == this)return true;
		if(!(obj instanceof IntVector))return false;
		IntVector v = (IntVector)obj;
		return v.x == x && v.y == y && v.z == z;
	}

	/**
	 * annoyingly not the same as BlockFace!
	 */
	public Direction toDirection() {
		if (x < 0)
			return Direction.WEST;
		else if (x > 0)
			return Direction.EAST;
		else if (y < 0)
			return Direction.DOWN;
		else if (y > 0)
			return Direction.UP;
		else if (z < 0)
			return Direction.NORTH;
		else if (z > 0)
			return Direction.SOUTH;
		else
			return null;

	}

	public BlockFace toBlockFace() {
		if (x < 0)
			return BlockFace.WEST;
		else if (x > 0)
			return BlockFace.EAST;
		else if (y < 0)
			return BlockFace.DOWN;
		else if (y > 0)
			return BlockFace.UP;
		else if (z < 0)
			return BlockFace.NORTH;
		else if (z > 0)
			return BlockFace.SOUTH;
		else
			return null;
	}
	
	/**
	 * A useful method to get the block at a given vector. Only added late
	 * in the process, there are probably a lot of things that could be refactored
	 * to use this. On the other hand it would involve an extra method call and singleton
	 * instance get etc.
	 * @return
	 */
	public Block getBlock(){
		return Castle.getInstance().getWorld().getBlockAt(x, y, z);
	}
	
	
}
