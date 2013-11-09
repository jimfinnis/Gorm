package org.pale.gorm;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

/**
 * A class to hold an axis-aligned bounding box and manipulate it.
 * 
 * @author white
 * 
 */
public class Extent {

	public interface LocationRunner {
		void run(int x, int y, int z);

	}

	/**
	 * An iterable for iterating over points in an extent, returned by
	 * getVectorIterator()
	 * 
	 * @author white
	 * 
	 */
	public class VectorIterable implements Iterable<IntVector> {
		/**
		 * Used to iterate along walls of an extent, returned by
		 * VectorIterable.iterator()
		 * 
		 * @author white
		 * 
		 */
		public class VectorIterator implements Iterator<IntVector> {
			IntVector pos, vec;

			public VectorIterator() {
				pos = new IntVector(minx, miny, minz);
				switch (xzOnly ? getLongestAxisXZ() : getLongestAxis()) {
				case X:
					vec = new IntVector(1, 0, 0);
					break;
				case Y:
					vec = new IntVector(0, 1, 0);
					break;
				default:
				case Z:
					vec = new IntVector(0, 0, 1);
					break;
				}
				pos = pos.add(vec.scale(offset));
				vec = vec.scale(step);
			}

			@Override
			public boolean hasNext() {
				return contains(pos);
			}

			@Override
			public IntVector next() {
				IntVector p = new IntVector(pos);
				pos = pos.add(vec);
				return p;
			}

			@Override
			public void remove() {
			}
		}

		private int step, offset;
		private boolean xzOnly;

		VectorIterable(int step, int offset, boolean xzOnly) {
			this.step = step;
			this.offset = offset;
			this.xzOnly = xzOnly;
		}

		@Override
		public Iterator<IntVector> iterator() {
			return new VectorIterator();
		}

	}

	public int minx;
	public int maxx;
	public int miny;
	public int maxy;
	public int minz;
	public int maxz;
	private boolean isset = false;

	@Override
	public String toString() {
		if(isset)
		return String.format("[%d - %d, %d - %d, %d - %d]", minx, maxx, miny,
				maxy, minz, maxz);
		else
			return "[invalid]";
	}

	/**
	 * Construct an extent from corners of rectangles. It doesn't matter which
	 * is the max or min corner, the system will sort it out.
	 * @param x1
	 * @param y1
	 * @param z1
	 * @param x2
	 * @param y2
	 * @param z2
	 */
	public Extent(int x1, int y1, int z1, int x2, int y2, int z2) {
		if(x1<x2){
			minx = x1;
			maxx = x2;			
		} else {
			minx = x2;
			maxx = x1;			
		}
		
		if(y1<y2){
			miny = y1;
			maxy = y2;			
		} else {			
			miny = y2;
			maxy = y1;			
		}
		if(z1<z2){
			minz = z1;
			maxz = z2;
		} else {
			minz = z2;
			maxz = z1;			
		}
		isset = true;
	}

	/**
	 * Creates an unset extent.
	 */
	public Extent() {
	}

	public Extent(Location loc) {
		minx = loc.getBlockX();
		miny = loc.getBlockY();
		minz = loc.getBlockZ();
		maxx = minx;
		maxy = miny;
		maxz = minz;
		isset = true;
	}

	/**
	 * Construct an extent centred around a given point in xz, floored at a
	 * given point in y
	 * 
	 * @param loc
	 * @param xsize
	 * @param ysize
	 * @param zsize
	 */
	public Extent(IntVector loc, int xsize, int ysize, int zsize) {
		minx = loc.x - xsize / 2;
		miny = loc.y;
		minz = loc.z - zsize / 2;
		maxx = minx + xsize;
		maxy = miny + ysize;
		maxz = minz + zsize;
		isset = true;
	}

	/**
	 * Construct a single-block extent
	 * 
	 * @param x
	 * @param y
	 * @param z
	 */
	public Extent(int x, int y, int z) {
		minx = x;
		maxx = x;
		miny = y;
		maxy = y;
		minz = z;
		maxz = z;
		isset = true;
	}

	/**
	 * copy constructor
	 * 
	 * @param e
	 */
	public Extent(Extent e) {
		minx = e.minx;
		maxx = e.maxx;
		miny = e.miny;
		maxy = e.maxy;
		minz = e.minz;
		maxz = e.maxz;
		isset = e.isset;

	}

	public int xsize() {
		return (maxx - minx) + 1;
	}

	public int ysize() {
		return (maxy - miny) + 1;
	}

	public int zsize() {
		return (maxz - minz) + 1;
	}

	/**
	 * Similar to union with a point, but acts in place.
	 * @param x
	 * @param y
	 * @param z
	 */
	public void addPoint(int x, int y, int z) {
		if (!isset) {
			minx = x;
			maxx = x;
			miny = y;
			maxy = y;
			minz = z;
			maxz = z;
			isset = true;
		} else {
			if (x < minx)
				minx = x;
			if (x > maxx)
				maxx = x;
			if (y < miny)
				miny = y;
			if (y > maxy)
				maxy = y;
			if (z < minz)
				minz = z;
			if (z > maxz)
				maxz = z;
		}
	}
	
	/**
	 * produce a vector from the extent, with some fields set to the minimum, and some set
	 * to the maximum. 
	 */
	public IntVector getCorner(int minFields){
		int x = ((minFields & X)!=0) ? minx : maxx;
		int y = ((minFields & Y)!=0) ? miny : maxy;
		int z = ((minFields & Z)!=0) ? minz : maxz;
		return new IntVector(x,y,z);
	}

	/**
	 * Function taking an extent and (x,y,z), returning a new extent
	 * 
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	public Extent union(int x, int y, int z) {
		Extent e = new Extent(this);
		e.addPoint(x, y, z);
		return e;
	}

	/**
	 * Function taking an extent and a location, and returning a new extent
	 * 
	 * @param loc
	 */
	public Extent union(Location loc) {
		return union(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
	}

	public Extent union(IntVector v) {
		return union(v.x, v.y, v.z);
	}

	/**
	 * Function of two extents, and returning a new extent
	 * 
	 * @param loc
	 */
	public Extent union(Extent e) {
		e = union(e.minx, e.miny, e.minz);
		e.addPoint(e.maxx, e.maxy, e.maxz);
		return e;
	}

	public final static int X = 1;
	public final static int Y = 2;
	public final static int Z = 4;
	public final static int ALL = X | Y | Z;
	public final static int LONGEST = 8;
	public final static int LONGESTXZ = 16;

	/**
	 * Get the longest axis as a code
	 * 
	 * @return
	 */
	public int getLongestAxis() {
		if (xsize() > ysize() && xsize() > zsize())
			return X;
		else if (zsize() > ysize())
			return Z;
		else
			return Y;
	}

	public int getLongestAxisXZ() {
		return xsize() > zsize() ? X : Z;
	}

	/**
	 * Get length of axis
	 */
	public int getLengthOfAxis(int axis) {
		if (axis == LONGEST)
			axis = getLongestAxis();
		else if (axis == LONGESTXZ) {
			axis = getLongestAxisXZ();
		}
		switch (axis) {
		case X:
			return xsize();
		case Y:
			return ysize();
		case Z:
		default:
			return zsize();
		}
	}

	/**
	 * Expand the extent by N voxels in some axes
	 * 
	 * @param n
	 * @param axis
	 *            flags
	 * @return new extent
	 */
	public Extent expand(int n, int axes) {
		Extent e = new Extent(this);

		if ((axes & LONGEST) != 0) {
			axes |= getLongestAxis();
		}
		if ((axes & LONGESTXZ) != 0) {
			axes |= getLongestAxisXZ();
		}

		if ((axes & X) != 0) {
			e.minx -= n;
			e.maxx += n;
		}
		if ((axes & Y) != 0) {
			e.miny -= n;
			e.maxy += n;
		}
		if ((axes & Z) != 0) {
			e.minz -= n;
			e.maxz += n;
		}
		return e;
	}

	/**
	 * stretch the extent by a factor of X,Z in the XY plane
	 * 
	 * @return new extent
	 * 
	 */
	public Extent scaleXZ(double x, double z) {
		Extent e = new Extent(this);
		double minxd = minx;
		double maxxd = maxx;
		double minzd = miny;
		double maxzd = maxy;
		double cx = (minxd + maxxd) * 0.5;
		double cz = (minzd + maxzd) * 0.5;
		double sx = (maxxd - minxd) * x * 0.5;
		double sz = (maxzd - minzd) * z * 0.5;

		e.minx = (int) (cx - sx);
		e.maxx = (int) (cx + sx);
		e.minz = (int) (cz - sz);
		e.maxz = (int) (cz + sz);
		return e;
	}

	/**
	 * Assuming that the floor (miny) is correct, set the ceiling height.
	 * 
	 * @param n
	 */
	public Extent setHeight(int n) {
		Extent e = new Extent(this);
		e.maxy = miny + n;
		return e;
	}

	public boolean contains(IntVector v) {
		return contains(v.x, v.y, v.z);
	}

	public boolean contains(int x, int y, int z) {
		return x >= minx && x <= maxx && y >= miny && y <= maxy && z >= minz
				&& z <= maxz;
	}

	public boolean intersects(Extent e) {

            return
                  e.minx <= maxx && e.maxx >= minx && 
                  e.miny <= maxy && e.maxy >= miny &&
                  e.minz <= maxz && e.maxz >= minz;
	}

	/**
	 * Returns true if this extent contains any blocks we shouldn't overwrite.
	 * These should be natural blocks, not built ones - it should be possible to
	 * overwrite these
	 * 
	 * @return
	 */
	public boolean intersectsWorld() {
		World w = Castle.getInstance().getWorld();
		for (int x = minx; x <= maxx; x++) {
			for (int y = miny; y <= maxy; y++) {
				for (int z = minz; z <= maxz; z++) {
					int typeId = w.getBlockAt(x, y, z).getTypeId();
					// avoid anything less than block 17 except for 5 (which is
					// wood, so roofs are ok)
					if (typeId > 0 && typeId < 17 && typeId != 5)
						return true;
				}
			}
		}

		return false;
	}

	/**
	 * Returns how much of this extent is filled with dirt
	 * @return
	 */
	public double amountOfSoil() {
		World w = Castle.getInstance().getWorld();
		// basically we down until we find a block with greater than a certain
		// number of natural solids in it
		int count = 0; // count of natural blocks
		for (int y = maxy; y >= miny; y--) {
			for (int x = minx; x <= maxx; x++) {
				for (int z = minz; z <= maxz; z++) {
					int typeId = w.getBlockAt(x, y, z).getTypeId();
					// avoid anything less than block 17 except for 5 (which is
					// wood, so roofs are ok)
					if (typeId > 0 && typeId < 17 && typeId != 5)
						count++;
				}
			}
		}
		
		return ((double)count)/((double)volume());
	}

	public IntVector getCentre() {
		return new IntVector((minx + maxx) / 2, (miny + maxy) / 2,
				(minz + maxz) / 2);
	}

	public Extent addvec(int x, int y, int z) {
		Extent e = new Extent(this);
		e.minx += x;
		e.maxx += x;
		e.miny += y;
		e.maxy += y;
		e.minz += z;
		e.maxz += z;
		return e;
	}

	public Extent addvec(IntVector v) {
		return addvec(v.x, v.y, v.z);
	}

	public Extent subvec(int x, int y, int z) {
		Extent e = new Extent(this);
		e.minx -= x;
		e.maxx -= x;
		e.miny -= y;
		e.maxy -= y;
		e.minz -= z;
		e.maxz -= z;
		return e;
	}

	public Extent subvec(IntVector v) {
		return subvec(v.x, v.y, v.z);
	}

	/**
	 * Find the maximum height of this extent above the ground
	 * 
	 * @return
	 */
	public int getMaxHeightAboveWorld() {
		int maxheight = 0;
		World w = Castle.getInstance().getWorld();
		for (int x = minx; x <= maxx; x++) {
			for (int z = minz; z <= maxz; z++) {
				int h = w.getHighestBlockYAt(x, z);
				int diff = miny - h;
				if (diff > maxheight)
					maxheight = diff;
			}
		}

		return maxheight;
	}

	/**
	 * Find the minimum height of this extent above the ground
	 * 
	 * @return
	 */
	public int getMinHeightAboveWorld() {
		int minheight = 1000;
		World w = Castle.getInstance().getWorld();
		for (int x = minx; x <= maxx; x++) {
			for (int z = minz; z <= maxz; z++) {
				int h = w.getHighestBlockYAt(x, z);
				int diff = miny - h;
				if (diff < minheight)
					minheight = diff;
			}
		}

		return minheight;
	}

	static final int BADHEIGHT = -1000;

	/**
	 * Find the height of a square within this extent, ignoring any blocks above
	 * the extent. Assumes a completely empty column to be -1000; ditto blocks
	 * outside the xz of the extent
	 * 
	 * @param x
	 * @param z
	 * @return
	 */
	public int getHeightWithin(int x, int z) {
		World w = Castle.getInstance().getWorld();
		if (x < minx || x > maxx || z < minz || z > maxz)
			return BADHEIGHT;
		for (int y = maxy; y >= miny; y--) {
			Block b = w.getBlockAt(x, y, z);
			if (b.getType().isSolid())
				return y;
		}
		return BADHEIGHT;
	}

	/**
	 * Returns a new extent which is a single block thick, representing one of
	 * the walls of this extent.
	 * 
	 * @return
	 */
	public Extent getWall(Direction d) {
		Extent e = new Extent(this);
		switch (d) {
		case NORTH:
			e.maxz = e.minz; // remember NORTH is direction of decreasing Z
			break;
		case SOUTH:
			e.minz = e.maxz;
			break;
		case EAST:
			e.minx = e.maxx;
			break;
		case WEST:
			e.maxx = e.minx;
			break;
		case UP:
			e.miny = e.maxy;
			break;
		case DOWN:
			e.maxy = e.miny;
			break;
		}
		return e;
	}

	/**
	 * Generate an extent for an extent of given width and height, normal facing
	 * in the given direction, with a given depth on each side (so '1' gives a
	 * depth of 3.)
	 * 
	 */

	public Extent(Direction d, int w, int h, int depth) {
		miny = 0;
		maxy = h - 1;
		switch (d) {
		case NORTH:
		case SOUTH:
			minx = -w / 2;
			maxx = minx + w - 1;
			miny = 0;
			maxy = h - 1;
			minz = -depth;
			maxz = depth;
			break;
		case EAST:
		case WEST:
			minx = -depth;
			maxx = depth;
			miny = 0;
			maxy = h - 1;
			minz = -w / 2;
			maxz = minx + w - 1;
			break;
		case UP: // for up and down, h is ignored and w is used in both x and z
		case DOWN:
			minx = -w / 2;
			maxx = minx + w - 1;
			miny = -depth;
			maxy = depth;
			minz = -w / 2;
			maxz = minx + w - 1;
		}
	}

	public Extent intersect(Extent e) {
		if (intersects(e)) {
			Extent out = new Extent();
			out.minx = minx > e.minx ? minx : e.minx;
			out.maxx = maxx < e.maxx ? maxx : e.maxx;
			out.miny = miny > e.miny ? miny : e.miny;
			out.maxy = maxy < e.maxy ? maxy : e.maxy;
			out.minz = minz > e.minz ? minz : e.minz;
			out.maxz = maxz < e.maxz ? maxz : e.maxz;
			out.isset = true;
			return out;
		} else
			return null;
	}

	/**
	 * Return true if the passed extent is entirely inside me
	 * 
	 * @param e1
	 * @return
	 */
	public boolean contains(Extent e1) {
		return e1.minx >= minx && e1.maxx <= maxx && e1.miny >= miny
				&& e1.maxy <= maxy && e1.minz >= minz && e1.maxz <= maxz;
	}

	public boolean containsThis(Material m) {
		World w = Castle.getInstance().getWorld();
		boolean contains = false;
		for (int x = this.minx; x <= this.maxx; x++) {
			for (int z = this.minz; z <= this.maxz; z++) {
				for (int y = this.miny; y <= this.maxy; y++) {
					Block thisBlock = w.getBlockAt(x, y, z);
					if (thisBlock.getType() == m) {
						contains = true;
					}
				}
			}
		}
		return contains;
	}

	public int volume() {
		return (this.xsize() * this.zsize() * this.ysize());
	}

	/**
	 * grow the extent along the given direction by the given number of steps;
	 * faster than scale-add.
	 */
	public Extent growDirection(Direction d, int n) {
		Extent e = new Extent(this);
		switch (d) {
		case EAST:
			e.maxx += n;
			break;
		case WEST:
			e.minx -= n;
			break;
		case NORTH:
			e.minz -= n;
			break;
		case SOUTH:
			e.maxz += n;
			break;
		case UP:
			e.maxy += n;
			break;
		case DOWN:
			e.miny -= n;
			break;
		}
		return e;
	}

	/**
	 * Create an iterator which will steps along the longest axis of an extent.
	 * 
	 * @return
	 */
	public Iterable<IntVector> getVectorIterable(int step, int offset,
			boolean xzOnly) {
		return new VectorIterable(step, offset, xzOnly);
	}

	static final int INTERNAL_CHUNK_SIZE = 32;

	/**
	 * Return a 'chunk code' for a given pixel. Nothing to do with MC's chunks,
	 * although it was once.
	 * 
	 * @param x
	 * @param z
	 * @return
	 */
	private static int getChunkCode(int x, int z) {
		x /= INTERNAL_CHUNK_SIZE;
		z /= INTERNAL_CHUNK_SIZE;
		return x + z * INTERNAL_CHUNK_SIZE;
	}

	/**
	 * Build a list of all the chunks in the extent. They're not actually real
	 * chunks, just unique codes. My chunks are bigger than MCs.
	 * 
	 * @return
	 */
	public Set<Integer> getChunks() {
		Set<Integer> chunks = new HashSet<Integer>();
		for (int x = minx; x < maxx + INTERNAL_CHUNK_SIZE; x += INTERNAL_CHUNK_SIZE/2) {
			for (int z = minz; z < maxz + INTERNAL_CHUNK_SIZE; z += INTERNAL_CHUNK_SIZE/2) {

				chunks.add(getChunkCode(x, z));
			}
		}
		return chunks;
	}

	public void runOnAllLocations(LocationRunner e) {
		for (int x = minx; x <= maxx; x++) {
			for (int y = miny; y <= maxy; y++) {
				for (int z = minz; z <= maxz; z++) {
					e.run(x, y, z);
				}
			}
		}
	}


}
