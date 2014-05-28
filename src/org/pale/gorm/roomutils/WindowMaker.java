package org.pale.gorm.roomutils;

import java.util.Random;

import org.bukkit.Material;
import org.pale.gorm.Castle;
import org.pale.gorm.Direction;
import org.pale.gorm.Extent;
import org.pale.gorm.GormPlugin;
import org.pale.gorm.IntVector;
import org.pale.gorm.MaterialDataPair;
import org.pale.gorm.MaterialManager;
import org.pale.gorm.Room;
import org.pale.gorm.Util;

/**
 * Class for making and decorating windows. Beware the old grey windowmaker!
 * 
 * @author white
 * 
 */
public class WindowMaker {
	private static final float WINDOW_CHANCE = 0.3f;

	/**
	 * Build a window covering the given extent. The key is a random value used
	 * to make sure windows built at the same time look the same.
	 * 
	 * @param mgr
	 * @param e
	 *            extent of window
	 * @param re
	 *            extent of containing room
	 * @param out
	 * @param key
	 */
	public static void window(MaterialManager mgr, Extent e, Extent re,
			Direction out, int key) {
		// first blow the hole itself
		Castle c = Castle.getInstance();
		c.fill(e, Material.AIR, 0);

		// the window is glazed if it is above the floor or one unit in height.
		boolean glazed = (re.miny + 1) < e.miny || (e.ysize() == 1);
		// sometimes, glazing becomes iron bars.
		boolean ironNotGlass = ((key & 1) == 0);

		// so let's fill it

		if (glazed)
			// If not iron, get material and data of window material
			c.fill(e, ironNotGlass ? Material.IRON_FENCE : mgr.getWindow().m,
					ironNotGlass ? 0 : mgr.getWindow().d);
		else {
			// this window isn't going to get filled, lets put some bars in
			c.fill(e, Material.IRON_FENCE, 0);
		}
	}

	/**
	 * Given a material manager and room extent, make a set of windows.
	 * 
	 * @param mgr
	 * @param e
	 */
	public static void buildWindows(MaterialManager mgr, Room r) {

		if (!r.hasWindows())
			return; // no windows in this room!

		Castle c = Castle.getInstance();
		int key = c.r.nextInt();
		Extent roomExt = r.getExtent();

		int y; // base of window within the wall

		// window heights are more common at certain fixed positions
		switch (c.r.nextInt(5)) {
		case 0:
			y = 1;
			break; // windows on the floor
		case 1:
			y = 2;
			break; // windows at eye level
		case 2:
			y = roomExt.ysize() - 3;
			break; // windows near ceiling
		default:
			y = c.r.nextInt(roomExt.ysize() - 4) + 1;
			break; // random
		}
		int height = c.r.nextInt(roomExt.ysize() - y);
		if (y == 1 && height < 2)
			height = 2; // deal with silly windows.

		// GormPlugin.log(String.format("Room extent: %s, height=%d, y=%d",
		// roomExt.toString(), height, y));

		// for each wall, build windows!
		for (Direction d : Direction.values()) {
			if (d.vec.y == 0 && c.r.nextFloat() < WINDOW_CHANCE) { // don't put
																	// windows
																	// on
				// every wall, and
				// only vertical
				// walls!
				GormPlugin.log("Windows for direction " + d.toString());

				Extent wallExtent = roomExt.getWall(d); // the wall on that side
				int width = 1 + Util.randomExp(c.r, 2);
				int len = wallExtent.getLengthOfAxis(Extent.LONGESTXZ);
				int step = c.r.nextInt(Math.min(len / 3, 1)) + width + 1;
				int offset = (len % step + (step - width)) / 2;

				// pull the wall in by 2 to avoid putting windows in the corner.
				// Nobody puts windows in the corner.
				wallExtent = wallExtent.expand(-2, Extent.LONGESTXZ);

				for (IntVector pos : wallExtent.getVectorIterable(step, offset,
						true)) {
					Extent window = new Extent();
					window = window.union(pos.add(0, y, 0)).union(
							pos.add(0, y + height - 1, 0));
					if (!r.windowIntersects(window)) {// avoid intersect with
														// existing window in
														// this room
						if (wallExtent.contains(window) // avoid wide window
														// overrun
								&& !r.exitIntersects(window.expand(2, Extent.X
										| Extent.Z))) {// avoid overwriting
														// exits
							window(mgr, window, roomExt, d, key);
							r.addWindow(window);
						}
					}
				}
			}
		}
	}

	/**
	 * Make one of the walls stained glass
	 * 
	 * @param mgr
	 * @param r
	 */
	public static void makeStainedGlassWall(MaterialManager mgr, Room r) {
		Random rnd = Castle.getInstance().r;
		Direction wallDir = Direction.getRandom(rnd, true);
		Extent wallExtent = r.getExtent().getWall(wallDir).expand(-2, Extent.Y)
				.expand(-2, Extent.LONGEST);

		// remove existing windows in that wall, we're about to overwrite them
		r.removeWindows(wallExtent);
		
		// Now for the fun part - we're going to have to generate a random
		// 2D pattern of colours and put it into the wall.
		
		int n = rnd.nextInt(3)+2;
		MaterialDataPair[] mats = new MaterialDataPair[n];
		for(int i=0;i<n;i++){
			mats[i]=new MaterialDataPair(Material.STAINED_GLASS_PANE,rnd.nextInt(16));
		}
		
		Castle.getInstance().patternFill(wallExtent,mats,n);
		
		r.addWindow(wallExtent);

	}
}
