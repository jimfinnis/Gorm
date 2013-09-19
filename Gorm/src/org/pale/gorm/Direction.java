package org.pale.gorm;

/**
 * A simple direction enum which behaves nicely with IntVector.
 * @author white
 *
 */
public enum Direction {
	NORTH(0, 0, -1), SOUTH(0, 0, 1), EAST(1, 0, 0), WEST(-1, 0, 0), UP(0,
			1, 0), DOWN(0, -1, 0);

	Direction(int x, int y, int z) {
		vec = new IntVector(x, y, z);
	}
	
	/**
	 * There are faster ways to do this :)
	 * @return
	 */
	public Direction opposite(){
		return vec.rotate(2).toDirection();
	}

	public final IntVector vec;
}