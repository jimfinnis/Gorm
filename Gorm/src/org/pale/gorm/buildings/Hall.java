package org.pale.gorm.buildings;

import java.util.Random;

import org.bukkit.Material;
import org.pale.gorm.Building;
import org.pale.gorm.Castle;
import org.pale.gorm.Extent;
import org.pale.gorm.GormPlugin;
import org.pale.gorm.MaterialManager;
import org.pale.gorm.Noise;
import org.pale.gorm.Room;
import org.pale.gorm.rooms.RoofGarden;
import org.pale.gorm.roomutils.BoxBuilder;
import org.pale.gorm.roomutils.PitchedRoofBuilder;
import org.pale.gorm.roomutils.RoofBuilder;

/**
 * A hall with stone walls and multiple floors
 * 
 * @author white
 * 
 */
public class Hall extends Building {
	
	public Hall(Extent e) {
		extent = e;
	}

	public Hall(Building parent) {
		Random rnd = Castle.getInstance().r;
		
		

		// get the new building's extent (i.e. location and size)
		int x = rnd.nextInt(10) + 5;
		int z = rnd.nextInt(10) + 5;
		int y;
		
		double height = Noise.noise2Dfractal(x, z, 1, 2, 3, 0.5); //0-1 noise
		double variance = Noise.noise2Dfractal(x,z,2,2,3,0.5);

		// TODO this is a blatant special case hack.
		if(parent.rooms.getFirst() instanceof RoofGarden && rnd.nextFloat()<0.8) {
			// if the top room of the parent has a garden, high chance that we're a good bit taller.
			y = parent.extent.ysize() + 4 + rnd.nextInt((int)(height*10.0));
		} else {
			y = rnd.nextInt(5+Noise.scale(15, variance))+10;
			y = Noise.scale(y,height)+5;
		}
		
		
		setInitialExtent(parent, x, y, z);
	}

	/**
	 * Draw into the world - call this *before* adding the building!
	 */
	public void build(MaterialManager mgr) {
		Castle c = Castle.getInstance();
		
		BoxBuilder.build(mgr, extent); // make the walls


		makeRooms(mgr); // and make the internal rooms

		underfill(mgr,false); // build "stilts" if required

		// is the bit above the building free?
		// Calculate an extent for a putative pitched roof, the height
		// of which is half the longest edge (is that right?)
		Extent e = new Extent(extent);
		e.miny = e.maxy;
		if (e.xsize() > e.zsize())
			e=e.setHeight(e.xsize() / 2);
		else
			e=e.setHeight(e.zsize() / 2);
		if (c.intersects(e)) {
			// only building for a small roof, so skip.
			RoofBuilder.randomRoof(mgr,extent);
		} else {
			// room for a bigger roof or maybe a roof garden! Roof garden if we're short.
			if (c.r.nextFloat() < 0.5 || extent.ysize()<24) {
				buildRoofGarden(mgr,e);
			} else {
				roofHeight = RoofBuilder.randomRoof(mgr,extent);
			}
		}
	}

	/**
	 * This adds a new room on top of the existing rooms, and extends the
	 * building upwards. The new room is marked as outside. The floor is grass,
	 * and this room will intrude one block into the innerspace of the the room
	 * below for the underfloor.
	 */
	private void buildRoofGarden(MaterialManager mgr,Extent e) {
		GormPlugin.log("Garden extent: "+e.toString());
		GormPlugin.log("Extent before roof garden: "+extent.toString());
		Room r= new RoofGarden(mgr,e,this);
		addRoomAndBuildExitDown(r, true);
		GormPlugin.log("Extent after roof garden: "+extent.toString());
	}

}
