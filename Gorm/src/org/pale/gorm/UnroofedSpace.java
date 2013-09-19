package org.pale.gorm;

import org.bukkit.Material;

public class UnroofedSpace extends Room {

	public UnroofedSpace(Extent e) {
		super(RoomType.GARDEN,e);
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
		
		lightWalls(e);
		floorLights(e);
		underfill();
		
		makeExit(e.miny,true);
		makeExit(e.miny,true);
		makeExit(e.miny,true);
	}
	
	

}
