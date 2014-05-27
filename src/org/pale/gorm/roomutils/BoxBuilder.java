package org.pale.gorm.roomutils;

import java.util.Iterator;
import java.util.Random;

import org.bukkit.Material;
import org.bukkit.World;
import org.pale.gorm.Castle;
import org.pale.gorm.Direction;
import org.pale.gorm.Extent;
import org.pale.gorm.GormPlugin;
import org.pale.gorm.IntVector;
import org.pale.gorm.MaterialDataPair;
import org.pale.gorm.MaterialManager;
import org.pale.gorm.Turtle;

/**
 * A class containing static methods to help build hollow boxes - i.e. buildings
 * - in different ways
 * 
 * @author white
 * 
 */
public class BoxBuilder {
	public static void build(MaterialManager mgr, Extent e) {
		Random r = Castle.getInstance().r;
		switch (r.nextInt(3)) {
		case 0:
		case 1:
			plain(mgr, e);
			break;
		case 2:
			bevelled(mgr, e);
			break;
		}
		if (r.nextFloat() < 0.3)
			buttress(mgr, e);
	}

	/**
	 * Just build a plain box whose walls are the outer limit of the extent
	 * 
	 * @param mgr
	 * @param e
	 */
	public static void plain(MaterialManager mgr, Extent e) {
		// fill with primary material
		Castle c = Castle.getInstance();
		MaterialDataPair prim = mgr.getPrimary();
		c.fill(e, prim.m, prim.d);
		c.fill(e.expand(-1, Extent.ALL), Material.AIR, 1); // 'inside' air
	}

	/**
	 * Build a box whole walls have a vertical bevelled effect
	 * 
	 * @param mgr
	 * @param e
	 */
	public static void bevelled(MaterialManager mgr, Extent e) {
		Castle c = Castle.getInstance();
		MaterialDataPair prim = mgr.getPrimary();

		c.fill(e.getWall(Direction.NORTH).expand(-1, Extent.X), prim);
		c.fill(e.getWall(Direction.SOUTH).expand(-1, Extent.X), prim);
		c.fill(e.getWall(Direction.EAST).expand(-1, Extent.Z), prim);
		c.fill(e.getWall(Direction.WEST).expand(-1, Extent.Z), prim);

		// easiest way to build the ceiling and floor while still maintaining
		// the "cross" shape
		c.fill(e.getWall(Direction.UP).expand(-1, Extent.X), prim);
		c.fill(e.getWall(Direction.UP).expand(-1, Extent.Z), prim);
		c.fill(e.getWall(Direction.DOWN).expand(-1, Extent.X), prim);
		c.fill(e.getWall(Direction.DOWN).expand(-1, Extent.Z), prim);

		c.fill(e.expand(-1, Extent.ALL), Material.AIR, 1); // 'inside' air

	}

	/**
	 * Classed used for producing "buttress-style" intervals 
	 * @author white
	 *
	 */
	public static class SpacingIterator implements Iterator<Integer> {
		int spacing;
		int offset;
		int current=-1;
		int width;

		public SpacingIterator(int width) {
			this.width = width;
			for (int x = width / 2; x >= 2; x--) {
				for (int y = 1; y <= width / 3; y++) {
					if ((width - (2 * y + 1)) % x == 0) {
						spacing = x;
						offset = y;
						current = offset;
						return;
					}
				}
			}
		}
		
		@Override
		public boolean hasNext() {
			return current!=-1;
		}

		@Override
		public Integer next() {
			int c = current;
			current += spacing;
			if(current>=width)
				current = -1;
			return c;
		}

		@Override
		public void remove() {
		}
	}

	/**
	 * build buttresses outside the walls, which may be overwritten by other
	 * buildings
	 * 
	 * @param mgr
	 * @param e
	 */
	public static void buttress(MaterialManager mgr, Extent e) {
		// we buttress the longest side

		World w = Castle.getInstance().getWorld();
		SpacingIterator i;
		IntVector a,b;
		Direction dir;
		if(e.xsize()>e.zsize()){
			a = new IntVector(e.minx,e.maxy,e.minz-1);
			b = new IntVector(e.minx,e.maxy,e.maxz+1);
			i = new SpacingIterator(e.xsize());
			dir = Direction.EAST;
		} else {
			a = new IntVector(e.minx-1,e.maxy,e.maxz);
			b = new IntVector(e.maxx+1,e.maxy,e.maxz);			
			i = new SpacingIterator(e.zsize());
			dir = Direction.NORTH;
		}
		
		while(i.hasNext()){
			Turtle t;
			IntVector start;
			int current = i.next();
			
			start = a.add(dir.vec.scale(current));
			t = new Turtle(mgr,w,start,dir.vec.rotate(-1).toDirection());
			doButtress(t);
			start = b.add(dir.vec.scale(current));
			t = new Turtle(mgr,w,start,dir.vec.rotate(1).toDirection());
			doButtress(t);
		}
	}

	private static void doButtress(Turtle t) {
		t.run("+S.mS.wdfw.m1bw.dfw.+c+w:d");
	}
}
