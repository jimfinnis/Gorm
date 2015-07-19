package org.pale.gorm.roomutils;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.pale.gorm.Castle;
import org.pale.gorm.Extent;
import org.pale.gorm.GormPlugin;
import org.pale.gorm.MaterialDataPair;
import org.pale.gorm.MaterialManager;

public class Gardener {
	/**
	 * Plant some stuff
	 * 
	 * @param floor
	 *            INNER extent of floor
	 */
	public static void plant(final MaterialManager mgr,Extent floor) {
		final Castle c = Castle.getInstance();
		final World w = c.getWorld();
		final double chance = 0.7; // chance of each square popping up a flower
		final double rareChance = 0.1; // chance of that flower being rare!

		floor = floor.addvec(0, 1, 0);
		final MaterialManager.MatType mt = c.r.nextDouble()<rareChance ? 
				MaterialManager.MatType.RAREFLOWERS:MaterialManager.MatType.FLOWERS;
		floor.runOnAllLocations(new Extent.LocationRunner() {

			@Override
			public void run(int x, int y, int z) {
				MaterialDataPair mat;
				if(c.r.nextDouble()<chance){
					mat = mgr.getRandom(mt); 
					if(w.getBlockAt(x,y,z).getType()==Material.AIR){
						Block b = w.getBlockAt(x, y - 1, z);
						if (b.getType() == Material.GRASS
								|| b.getType() == Material.DIRT) {
							b = w.getBlockAt(x, y, z);
							if (b.getType() == Material.AIR) {
								b = w.getBlockAt(x, y, z);
								b.setType(mat.m);
								b.setData((byte) mat.d);
							}
						}
					}
				}
			}
		});

	}

	private static final Material[] crops = { Material.CROPS, Material.CROPS,
		Material.CROPS, Material.CARROT, Material.POTATO };

	public static void makeFarm(Extent floor) {
		Castle c = Castle.getInstance();
		c.fill(floor, new MaterialDataPair(Material.SOIL, 0));

		MaterialDataPair crop = new MaterialDataPair(
				crops[c.r.nextInt(crops.length)], 0);

		for (int x = floor.minx; x <= floor.maxx; x++) {
			Extent tmp = new Extent(floor).setX(x);
			if ((x - floor.minx) % 3 == 2) {
				c.fill(tmp, new MaterialDataPair(Material.WATER, 0));
				c.fill(tmp.setZ((floor.minz + floor.maxz) / 2).addvec(0, 1, 0),
						new MaterialDataPair(Material.STEP, 0));

			} else {
				c.fill(tmp.addvec(0, 1, 0), crop);
			}
		}

	}

}
