package org.pale.gorm.rooms;

import org.pale.gorm.Building;
import org.pale.gorm.Castle;
import org.pale.gorm.Extent;
import org.pale.gorm.MaterialDataPair;
import org.pale.gorm.MaterialManager;
import org.pale.gorm.Room;
import org.pale.gorm.roomutils.Furniture;

/**
 * because every castle needs books.
 * @author domos
 *
 */
public class ReadingRoom extends Room {

	public ReadingRoom(MaterialManager mgr,Extent e, Building b) {
		super(mgr, e, b);
	}

	@Override
	public Extent build(MaterialManager mgr, Extent buildingExtent) {
		Castle c = Castle.getInstance();
		// make the actual floor - first layer
		Extent floor = e.expand(-1, Extent.X | Extent.Z);
		floor.maxy = floor.miny;
		
		// fill with primary material
		MaterialDataPair prim = mgr.getPrimary();
		c.fill(floor,prim.m,prim.d);
		Furniture f = new Furniture();
		Extent inner = e.expand(-1, Extent.ALL);
		int carpetCol = c.r.nextInt(14);
		b.carpet(inner, carpetCol);
		b.lightWalls(inner);
		b.floorLights(inner);
		f.cornerShelves(inner,mgr,carpetCol);
		
		addSignHack();
		return null; // we don't modify the building extent
	}

}
