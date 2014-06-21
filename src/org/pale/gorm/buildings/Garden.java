package org.pale.gorm.buildings;

import java.util.Random;

import org.bukkit.World;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.pale.gorm.Building;
import org.pale.gorm.Castle;
import org.pale.gorm.Direction;
import org.pale.gorm.Extent;
import org.pale.gorm.GormPlugin;
import org.pale.gorm.MaterialDataPair;
import org.pale.gorm.MaterialManager;
import org.pale.gorm.roomutils.Gardener;

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
		setInitialExtent(e);
	}

	public Garden(Building parent) {
		Random rnd = Castle.getInstance().r;

		setInitialExtent(parent, rnd.nextInt(10) + 10, 10, rnd.nextInt(10) + 10);

		GormPlugin.log("Creating garden.");
		GormPlugin.log("Parent extent: " + parent.extent.toString());
		GormPlugin.log("Parent origex: " + parent.originalExtent.toString());
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

		c.checkFill(inner, Material.AIR, 0); // fill the inner area
		// c.checkFill(extent, Material.AIR, 0); // but in the outer, avoid
		// filling in walls
		// already there
		Extent floor = extent.getWall(Direction.DOWN);

		makeSingleRoom(mgr).setOutside().setAllSidesOpen(); // this is a single,
															// exterior room

		underfill(mgr, true);
		// fill the underfloor with a foundation material, otherwise
		// we can end up unroofing things
		c.checkFill(floor.subvec(0, 1, 0), mgr.getPrimary());

		// fill the floor with a random material
		if (c.r.nextFloat() < 0.9) {
			// patterned floor
			int n = c.r.nextInt(3) + 2;
			MaterialDataPair[] mats = new MaterialDataPair[n];
			for (int i = 0; i < n; i++) {
				switch (c.r.nextInt(5)) {
				case 0:
					mats[i] = mgr.getGround();
					break;
				case 1:
					mats[i] = mgr.getGround();
					break;
				case 2:
					mats[i] = mgr.getSecondary();
					break;
				case 3:
					mats[i] = mgr.getSupSecondary();
					break;
				case 4:
					mats[i] = mgr.getSupSecondary();
					break;
				default:
					break;
				}
			}
			c.patternFill(floor.expand(-1, Extent.X | Extent.Z), mats, n, null);
		} else
			// plain floor
			c.fill(floor.expand(-1, Extent.X | Extent.Z), ground);
		// room
		Gardener.plant(floor); // plant some things
		floorLights(inner); // light the inner region
	}

	@Override
	public Building createChildBuilding(Random r) {
		return BuildingFactory.createRandom(this, 
				new BuildingType[] {
				BuildingType.PATH,
				BuildingType.LIBRARY,
				BuildingType.HALL,
				BuildingType.DWELLING,
				});

	}

}
