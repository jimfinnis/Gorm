package org.pale.gorm.roomutils;

import org.bukkit.Material;
import org.pale.gorm.Castle;
import org.pale.gorm.Direction;
import org.pale.gorm.Extent;
import org.pale.gorm.GormPlugin;
import org.pale.gorm.IntVector;
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
		boolean glazed = re.miny != e.miny || (e.ysize() == 1);
		// sometimes, glazing becomes iron bars.
		boolean ironNotGlass = ((key & 1) == 0);

		// so let's fill it

		if (glazed)
			c.fill(e, ironNotGlass ? Material.IRON_FENCE : Material.THIN_GLASS,
					0);
		else if (re.miny == e.miny) {
			// this big window is dangerously low! Put some bars in the bottom
			// part.
			c.fill(e.getWall(Direction.DOWN), Material.IRON_FENCE, 0);
		}
	}

	/**
	 * Given a material manager and room extent, make a set of windows.
	 * 
	 * @param mgr
	 * @param e
	 */
	public static void buildWindows(MaterialManager mgr, Room r) {
		Castle c = Castle.getInstance();
		int key = c.r.nextInt();
		Extent roomExt = r.getExtent();

		int y; // base of window within the wall

		// window heights are more common at certain fixed positions
		switch (c.r.nextInt(4)) {
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
		case 3:
			y = c.r.nextInt(roomExt.ysize() - 4) + 1;
			break; // random
		}
		int height = c.r.nextInt(roomExt.ysize() - y);
		if (y == 1 && height < 2)
			height = 2; // deal with silly windows.

		GormPlugin.log(String.format("Room extent: %s, height=%d, y=%d",
				roomExt.toString(), height, y));

		// for each wall, build windows!
		for (Direction d : Direction.values()) {
			if (d.vec.y == 0 && c.r.nextFloat() < 0.5) { // don't put windows on
															// every wall, and
															// only vertical
															// walls!
				GormPlugin.log("Windows for direction " + d.toString());

				Extent wallExtent = roomExt.getWall(d); // the wall on that side
				int width = 1 + Util.randomExp(c.r, 2);
				int len = wallExtent.getLengthOfAxis(Extent.LONGESTXZ);
				int step = c.r.nextInt(Math.min(len / 3,1)) + width + 1;
				int offset = (len % step + (step - width)) / 2;

				// pull the wall in by 2 to avoid putting windows in the corner.
				// Nobody puts windows in the corner.
				wallExtent = wallExtent.expand(-2, Extent.LONGESTXZ);

				for (IntVector pos : wallExtent.getVectorIterable(step, offset,
						true)) {
					Extent window = new Extent();
					window = window.union(pos.add(0, y, 0)).union(
							pos.add(0, y + height - 1, 0));
					if (wallExtent.contains(window) // avoid wide window
													// overrun
							&& !r.exitIntersects(window.expand(2, Extent.X
									| Extent.Z))) {// avoid overwriting
													// exits
						window(mgr,window,roomExt,d,key);
						r.addWindow(window);
					}
				}
			}
		}
	}
}
