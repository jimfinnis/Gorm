package org.pale.gorm.buildings;

import java.util.Random;

import org.bukkit.World;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.pale.gorm.Building;
import org.pale.gorm.Castle;
import org.pale.gorm.Direction;
import org.pale.gorm.Extent;
import org.pale.gorm.MaterialDataPair;
import org.pale.gorm.MaterialManager;

/**
 * A 'garden' area
 */
public class Garden extends Building {

	/**
	 * used for the initial building
	 * 
	 * @param e
	 */
	public Garden(Extent e) {
		extent = e;
	}

	public Garden(Building parent) {
		Random rnd = Castle.getInstance().r;

		setInitialExtent(parent, rnd.nextInt(10) + 10, 20, rnd.nextInt(10) + 10);
	}

	@Override
	public void build(MaterialManager mgr) {
		Castle c = Castle.getInstance();
		Extent inner = extent.expand(-1, Extent.ALL);

		MaterialDataPair ground;

		if (c.r.nextFloat() < 0.2)
			ground = mgr.getSupSecondary();
		else
			ground = mgr.getGround();

		// fill the floor with a random material
		Extent floor = extent.getWall(Direction.DOWN);
		c.fill(inner, Material.AIR, 0); // fill the inner area unconditionally
		c.checkFill(extent, Material.AIR, 0); // but avoid filling in walls
												// already there
		c.checkFill(floor, ground.m, ground.d);

		underfill(mgr, true);

		makeSingleRoom(mgr).setAllSidesOpen(); // this is a single, exterior
												// room
		plant(floor); // plant some things
		floorLights(inner); // light the inner region
	}

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
					Block b = w.getBlockAt(x, y, z);
					b.setType(mat.m);
					b.setData((byte) mat.d);
				}
			}
		});

	}
}
