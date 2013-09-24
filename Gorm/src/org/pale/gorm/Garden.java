package org.pale.gorm;

import java.util.Random;

import org.bukkit.Material;

/**
 * A 'garden' area
 */
public class Garden extends Building {

	/**
	 * used for the initial building
	 * @param e
	 */
	public Garden(Extent e) {
		extent = e;
	}
	
	public Garden(Building parent){
		Random rnd = Castle.getInstance().r;
		
		setInitialExtent(parent, rnd.nextInt(10)+10, 20, rnd.nextInt(10)+10);
	}

	@Override
	public void build(MaterialManager mgr) {
		Castle c = Castle.getInstance();
		Extent inner = extent.expand(-1, Extent.ALL);
		

		MaterialManager.MaterialDataPair ground;
		
		if(c.r.nextFloat()<0.2)
			ground = mgr.getSupSecondary();
		else
			ground = mgr.getGround();

		// fill the floor with a random material
		Extent floor = extent.getWall(Direction.DOWN);
		c.fill(inner, Material.AIR, 0);	// fill the inner area unconditionally
		c.checkFill(extent, Material.AIR, 0); // but avoid filling in walls already there
		c.checkFill(floor, ground.m, ground.d);
		
		underfill(mgr,true);
		
		makeSingleRoom(mgr).setAllSidesOpen(); // this is a single, exterior room
		floorLights(inner); // light the inner region
	}
}
