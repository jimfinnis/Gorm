package org.pale.gorm;

import java.util.Random;

import org.bukkit.Material;
import org.pale.gorm.roomutils.PitchedRoofBuilder;

/**
 * A hall with stone walls and multiple floors
 * @author white
 *
 */
public class Hall extends Building {

	public Hall(Building parent) {
		Random rnd = Castle.getInstance().r;

		// get the new building's extent (i.e. location and size)
		int x = rnd.nextInt(10) + 5;
		int z = rnd.nextInt(10) + 5;
		int y;
		
		if (rnd.nextFloat() < 0.1)
			y = rnd.nextInt(10) + 20;
		else
			y = rnd.nextInt(3) + 15;
		setInitialExtent(parent, x, y, z);
	}
	
	/**
	 * Draw into the world - call this *before* adding the building!
	 */
	public void build() {
		Castle c = Castle.getInstance();

		// basic building for now
		c.fillBrickWithCracksAndMoss(extent, true);

		c.fill(extent.expand(-1, Extent.ALL), Material.AIR, 1); // 'inside' air

		underfill(false);

		makeRooms();

		// is the bit above the building free?
		Extent e = new Extent(extent);
		e.miny = e.maxy;
		if (e.xsize() > e.zsize())
			e.setHeight(e.xsize() / 2);
		else
			e.setHeight(e.zsize() / 2);
		if (c.intersects(e)) {
			// only building for a small roof
		} else {
			// room for a bigger roof
			new PitchedRoofBuilder().buildRoof(extent);
		}
	}


}
