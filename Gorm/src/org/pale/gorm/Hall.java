package org.pale.gorm;

import java.util.Random;

import org.bukkit.Material;
import org.pale.gorm.rooms.RoofGarden;
import org.pale.gorm.roomutils.PitchedRoofBuilder;

/**
 * A hall with stone walls and multiple floors
 * 
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

		if(parent.rooms.getFirst().getIsOutside() && rnd.nextFloat()<0.8) {
			// if the top room of the parent has a garden, high chance that we're a good bit taller.
			y = parent.extent.ysize() + 4 + rnd.nextInt(10);
		}
		else if (rnd.nextFloat() < 0.01)
			y = rnd.nextInt(20) + 30;
		else
			y = rnd.nextInt(20) + 10;
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
		// Calculate an extent for a putative pitched roof, the height
		// of which is half the longest edge (is that right?)
		Extent e = new Extent(extent);
		e.miny = e.maxy;
		if (e.xsize() > e.zsize())
			e.setHeight(e.xsize() / 2);
		else
			e.setHeight(e.zsize() / 2);
		if (c.intersects(e)) {
			// only building for a small roof, so skip.
		} else {
			// room for a bigger roof or maybe a roof garden! Roof garden if we're short.
			if (c.r.nextFloat() < 0.5 || extent.ysize()<24) {
				buildRoofGarden(e);
			} else {
				new PitchedRoofBuilder().buildRoof(extent);
			}
		}
	}

	/**
	 * This adds a new room on top of the existing rooms, and extends the
	 * building upwards. The new room is marked as outside. The floor is grass,
	 * and this room will intrude one block into the innerspace of the the room
	 * below for the underfloor.
	 */
	private void buildRoofGarden(Extent e) {
		Room r= new RoofGarden(e,this);
		addRoomAndBuildExitDown(r, true);
	}

}
