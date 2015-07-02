package org.pale.gorm.rooms;

import org.pale.gorm.Building;
import org.pale.gorm.Castle;
import org.pale.gorm.Extent;
import org.pale.gorm.GormPlugin;
import org.pale.gorm.IntVector;
import org.pale.gorm.MaterialDataPair;
import org.pale.gorm.MaterialManager;
import org.pale.gorm.Room;
import org.pale.gorm.roomutils.DungeonObjects;
import org.pale.gorm.roomutils.Furniture;
import org.pale.gorm.roomutils.FurnitureItems;

/**
 * Was the chest room, now may contain no chest! That's done using the standard furniture
 * mechanism.
 * Is now also the base class of all rooms with a floor.
 * 
 * @author domos
 *
 */
public class EmptyRoom extends Room {
	private boolean hasChest=false;
	public EmptyRoom(MaterialManager mgr,Extent e, Building b, boolean addChest) {
		super(mgr, e, b);
		hasChest=addChest;
	}

	@Override
	public Extent build(MaterialManager mgr, Extent buildingExtent) {
		Castle c = Castle.getInstance();
		// make the actual floor - first layer
		Extent floor = e.expand(-1, Extent.X | Extent.Z);
		floor.maxy = floor.miny;
		
		// fill with floor material
		MaterialDataPair f = mgr.getFloor();
		c.fill(floor,f.m,f.d);

		lightsAndCarpets(true);
		
		addSignHack();
		
		return null; // we don't modify the building extent
	}

	@Override
	public void furnish(MaterialManager mgr) {
		// This places a chest somewhere
		if(hasChest)
			Furniture.place(mgr,this, "Ccw");
	}

}
