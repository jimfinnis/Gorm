package org.pale.gorm;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

/**
 * A class to hold a bounding box, or extent or a room or the entire castle
 * 
 * @author white
 * 
 */
public class Extent {
	public int minx;
	public int maxx;
	public int miny;
	public int maxy;
	public int minz;
	public int maxz;
	private boolean isset = false;

	@Override
	public String toString() {
		return String.format("[%d - %d, %d - %d, %d - %d]",minx,maxx,miny,maxy,minz,maxz);
	}
	
	public Extent(int x1,int y1,int z1,int x2,int y2,int z2){
		minx = x1;
		miny = y1;
		minz = z1;
		maxx = x2;
		maxy = y2;
		maxz = z2;
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
	 * @param x
	 * @param y
	 * @param z
	 */
	public Extent(int x,int y,int z){
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
	
	public int xsize(){
		return (maxx-minx)+1;
	}
	public int ysize(){
		return (maxy-miny)+1;
	}
	public int zsize(){
		return (maxz-minz)+1;
	}

	private void add( int x, int y, int z) {
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
	 * Function taking an extent and (x,y,z), returning a new extent
	 * 
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	public Extent union(int x, int y, int z) {
		Extent e = new Extent(this);
		e.add(x, y, z);
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

	/**
	 * Function of two extents, and returning a new extent
	 * 
	 * @param loc
	 */
	public Extent union(Extent e) {
		e = union(e.minx, e.miny, e.minz);
		e.add(e.maxx, e.maxy, e.maxz);
		return e;
	}

	public final static int X = 1;
	public final static int Y = 2;
	public final static int Z = 4;
	public final static int ALL = X | Y | Z;

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
		return !(e.minx > maxx || e.maxx < minx || e.miny > maxy
				|| e.maxy < miny || e.minz > maxz || e.maxz < minz);
	}

	/**
	 * Returns true if this extent contains any blocks we shouldn't overwrite. These
	 * should be natural blocks, not built ones - it should be possible to overwrite these
	 * 
	 * @return
	 */
	public boolean intersectsWorld() {
		World w = Castle.getInstance().getWorld();
		for (int x = minx; x <= maxx; x++) {
			for (int y = miny; y <= maxy; y++) {
				for (int z = minz; z <= maxz; z++) {
					int typeId = w.getBlockAt(x, y, z).getTypeId();
					// avoid anything less than block 17 except for 5 (which is wood, so roofs are ok)
					if (typeId > 0 && typeId < 17 && typeId!=5)
						return true;
				}
			}
		}

		return false;
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
	 * Find the maximum height of this extent above the ground
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
	 * Find the height of a square within this extent, ignoring any blocks above the extent.
	 * Assumes a completely empty column to be -1000; ditto blocks outside the xz of the extent
	 * @param x
	 * @param z
	 * @return
	 */
	public int getHeightWithin(int x, int z){
		World w = Castle.getInstance().getWorld();
		if(x<minx || x>maxx || z<minz || z>maxz)
			return BADHEIGHT;
		for(int y=maxy;y>=miny;y--){
			Block b = w.getBlockAt(x,y,z);
			if(b.getType().isSolid())
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
	 * in the given direction, with a given depth on each side (so '1' gives a depth of 3.)
	 * 
	 */

	public Extent(Direction d, int w, int h,int depth) {
		miny = 0;
		maxy = h - 1;
		switch (d) {
		case NORTH:
		case SOUTH:
			minx = -w / 2;
			maxx = minx + w -1;
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
			maxx = minx + w -1;
			miny = -depth;
			maxy = depth;
			minz = -w / 2;
			maxz = minx + w -1;
		}
	}
	
	public Extent intersect(Extent e){
		if(intersects(e)){
			Extent out = new Extent();
			out.minx = minx>e.minx ? minx : e.minx; 
			out.maxx = maxx<e.maxx ? maxx : e.maxx; 
			out.miny = miny>e.miny ? miny : e.miny; 
			out.maxy = maxy<e.maxy ? maxy : e.maxy; 
			out.minz = minz>e.minz ? minz : e.minz; 
			out.maxz = maxz<e.maxz ? maxz : e.maxz;
			out.isset =true;
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
	public boolean inside(Extent e1) {
		return e1.minx >= minx && e1.maxx <= maxx && e1.miny >= miny
				&& e1.maxy <= maxy && e1.minz >= minz && e1.maxz <= maxz;
	}
	
	/**
	 * grow the extent along the given direction by the given number of steps each way
	 */
	public Extent growDirection(Direction d, int n){
		Extent e = new Extent(this);
		switch(d){
		case EAST:
		case WEST:
			e.minx -= n;e.maxx+=n;
			break;
		case NORTH:
		case SOUTH:
			e.minz -= n;e.maxz+=n;
			break;
		case UP:
		case DOWN:
			e.miny -= n;e.maxy+=n;
			break;
		}
		return e;
	}

}
