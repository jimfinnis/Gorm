package org.pale.gorm;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;

/**
 * Simple vector of integers
 * 
 * @author white
 * 
 */
public class IntVector {
	public enum Direction {
		NORTH(0, 0, -1), SOUTH(0, 0, 1), EAST(1, 0, 0), WEST(-1, 0, 0), UP(0,
				1, 0), DOWN(0, -1, 0);

		Direction(int x, int y, int z) {
			vec = new IntVector(x, y, z);
		}

		final IntVector vec;
	}

	public int x, y, z;

	IntVector(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	IntVector(IntVector v) {
		this.x = v.x;
		this.y = v.y;
		this.z = v.z;
	}

	public IntVector(Location loc) {
		x = loc.getBlockX();
		y = loc.getBlockY();
		z = loc.getBlockZ();
	}

	IntVector add(int x, int y, int z) {
		return new IntVector(this.x + x, this.y + y, this.z + z);
	}

	IntVector add(IntVector v) {
		return add(v.x, v.y, v.z);
	}

	IntVector subtract(int x, int y, int z) {
		return new IntVector(this.x - x, this.y - y, this.z - z);
	}

	IntVector subtract(IntVector v) {
		return subtract(v.x, v.y, v.z);
	}

	IntVector negate() {
		return new IntVector(-x, -y, -z);
	}

	IntVector scale(int d) {
		return new IntVector(x * d, y * d, z * d);
	}

	@Override
	public String toString() {
		return String.format("[%d %d %d]", x, y, z);
	}

	/**
	 * Rotate clockwise in XZ plane
	 * 
	 * @param turns
	 *            of 90 degs clockwise
	 * @return
	 */
	IntVector rotate(int turns) {
		IntVector v;
		//GormPlugin.log("rotating "+Integer.toString(turns));

		switch (turns) {
		default:
		case 0:
			v = new IntVector(this);
			break;
		case 1:
			v = new IntVector(z, y, -x);
			break;
		case 2:
			v = new IntVector(-x, y, -z);
			break;
		case 3:
			v = new IntVector(-z, y, x);
			break;
		}
		return v;
	}

	/**
	 * rotate a vector to the orientation indicated by the zaxis passed in:
	 * (0,0,1) would require no rotation, (1,0,0) a 90deg clockwise, and so on.
	 * No good for up and down vectors.
	 * 
	 * @param zaxis
	 * @return
	 */
	IntVector rotateToSpace(IntVector zaxis) {
		//GormPlugin.log("zaxisvector"+zaxis.toString());
		if (zaxis.x < 0)
			return rotate(3);
		else if (zaxis.x > 0)
			return rotate(1);
		else if (zaxis.z < 0)
			return rotate(2);
		else if (zaxis.z > 0)
			return rotate(0);
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
		if (f > 45 && f <= 135) return IntVector.Direction.WEST;
		else if (f > 135 && f <= 225) return IntVector.Direction.NORTH;
		else if (f > 225 && f <= 315) return IntVector.Direction.EAST;
		else return IntVector.Direction.SOUTH;
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
}
