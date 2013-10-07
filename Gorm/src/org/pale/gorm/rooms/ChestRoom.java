package org.pale.gorm.rooms;

import org.pale.gorm.Building;
import org.pale.gorm.Castle;
import org.pale.gorm.Extent;
import org.pale.gorm.MaterialDataPair;
import org.pale.gorm.MaterialManager;
import org.pale.gorm.Room;
import org.pale.gorm.roomutils.DungeonObjects;

/**
 * A plain, carpeted room.
 * @author white
 *
 */
public class ChestRoom extends Room {

	public ChestRoom(MaterialManager mgr,Extent e, Building b) {
		super(mgr, e, b);
	}

	@Override
	public Extent build(MaterialManager mgr, Extent buildingExtent) {
		DungeonObjects dObj = new DungeonObjects();
		Castle c = Castle.getInstance();
		// make the actual floor - first layer
		Extent floor = e.expand(-1, Extent.X | Extent.Z);
		floor.maxy = floor.miny;
		
		// fill with primary material
		MaterialDataPair prim = mgr.getPrimary();
		c.fill(floor,prim.m,prim.d);

		Extent inner = e.expand(-1, Extent.ALL);
		b.carpet(inner, c.r.nextInt(14));
		b.lightWalls(inner);
		b.floorLights(inner);
		dObj.chest(floor, 1);
		
		addSignHack();
		
		return null; // we don't modify the building extent
	}

}
