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
	
	// if we use the new system for making furniture we won't need to keep this :)
	int carpetCol; 

	@Override
	public Extent build(MaterialManager mgr, Extent buildingExtent) {
		Castle c = Castle.getInstance();
		// make the actual floor - first layer
		Extent floor = e.expand(-1, Extent.X | Extent.Z);
		floor.maxy = floor.miny;
		
		// fill with primary material
		MaterialDataPair prim = mgr.getPrimary();
		c.fill(floor,prim.m,prim.d);
		carpetCol = lightsAndCarpets(true);
		addSignHack();
		return null; // we don't modify the building extent
	}

	@Override
	public void furnish(MaterialManager mgr) {
		Extent inner = e.expand(-1, Extent.ALL);
		Furniture.cornerShelves(inner,mgr,carpetCol);
	}

}
