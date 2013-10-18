package org.pale.gorm.rooms;

import org.bukkit.Material;
import org.pale.gorm.Building;
import org.pale.gorm.Castle;
import org.pale.gorm.Direction;
import org.pale.gorm.Extent;
import org.pale.gorm.MaterialDataPair;
import org.pale.gorm.MaterialManager;
import org.pale.gorm.Room;
import org.pale.gorm.roomutils.Furniture;

/**
 * A room which requires no extra walls or contents to be built.
 * @author white
 *
 */
public class PlainRoom extends Room {

	public PlainRoom(MaterialManager mgr, Extent e, Building b) {
		super(mgr, e, b);
	}

	@Override
	public Extent build(MaterialManager mgr, Extent buildingExtent) {
		Castle c = Castle.getInstance();
		// make the actual floor - first layer
		Extent floor = e.expand(-1, Extent.X | Extent.Z).getWall(Direction.DOWN);
		// fill with primary material
		MaterialDataPair prim = mgr.getPrimary();
		c.fill(floor,prim.m,prim.d);

		lightsAndCarpets(true);
		addSignHack();
		Furniture.placeFurniture(mgr,this, "ms.LwRwRw.bTLTLTbTRTRTbTLTLT");

		return null; // we don't modify the building extent
	}

}
