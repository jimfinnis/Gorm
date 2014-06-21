package org.pale.gorm.roomutils;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.pale.gorm.Castle;
import org.pale.gorm.Extent;
import org.pale.gorm.MaterialDataPair;

public class Gardener {
	/**
	 * Plant some stuff
	 * 
	 * @param floor
	 *            INNER extent of floor
	 */
	public static void plant(Extent floor) {
		final Castle c = Castle.getInstance();
		final World w = c.getWorld();

		// note the hardwired material codes. Probably best change these!

		floor = floor.addvec(0, 1, 0);
		floor.runOnAllLocations(new Extent.LocationRunner() {

			@Override
			public void run(int x, int y, int z) {
				MaterialDataPair mat = null;

				if (c.r.nextFloat() < 0.1) {
					switch (c.r.nextInt(10)) {
					case 0:
						mat = new MaterialDataPair(Material.YELLOW_FLOWER, 0);
						break;
					case 1:
						mat = new MaterialDataPair(Material.RED_ROSE, 0);
						break;
					case 2:
						if (c.r.nextFloat() < 0.1)
							mat = new MaterialDataPair(Material.SAPLING, 0);
						break;
					case 3:
					case 4:
					case 5:
					case 6:
					case 7:
					case 8:
					case 9:
						mat = new MaterialDataPair(Material.LONG_GRASS, 1);
						break;
					default:
						break;
					}
				}
				if (mat != null) {
					Block b = w.getBlockAt(x, y-1, z);
					if(b.getType() == Material.GRASS || b.getType()==Material.DIRT){
						b = w.getBlockAt(x, y, z);
						b.setType(mat.m);
						b.setData((byte) mat.d);
					}
				}
			}
		});

	}

}
