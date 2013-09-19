package org.pale.gorm;

import org.bukkit.Material;

public class UnroofedSpace extends Building {

	public UnroofedSpace(Extent e) {
		super(BuildingType.GARDEN,e);
	}

	@Override
	void render() {
		Extent e;
		Castle c = Castle.getInstance();
		
		// just some practice.
		c.fillBrickWithCracksAndMoss(extent,true);
		e = new Extent(extent);
		e.miny = (e.miny+e.maxy)/2;
		e.maxy = e.miny;
		c.checkFill(e, Material.SMOOTH_BRICK, 3);
		
		e =extent.expand(-1, Extent.ALL);
		e.maxy++;
		c.fill(e,Material.AIR,1); // fill with 'inside' air
		
		makeSingleRoom();
		lightWalls(e);
		floorLights(e);
		underfill();
	}
}
