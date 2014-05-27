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
		extent = e;
	}

	public Garden(Building parent) {
		Random rnd = Castle.getInstance().r;

		setInitialExtent(parent, rnd.nextInt(10) + 10, 10, rnd.nextInt(10) + 10);
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

		c.fill(inner, Material.AIR, 0); // fill the inner area unconditionally
		c.checkFill(extent, Material.AIR, 0); // but avoid filling in walls
												// already there
		Extent floor = extent.getWall(Direction.DOWN);

		// fill the underfloor with a foundation material, otherwise
		// we can end up unroofing things
		c.fill(floor.subvec(0,1,0), mgr.getSecondary());
		

		makeSingleRoom(mgr).setOutside().setAllSidesOpen(); // this is a single, exterior room
		// fill the floor with a random material
		c.checkFill(floor, ground.m, ground.d);
		underfill(mgr, true);
												// room
		Gardener.plant(floor); // plant some things
		floorLights(inner); // light the inner region
	}

}
