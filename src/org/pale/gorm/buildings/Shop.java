package org.pale.gorm.buildings;

import java.util.Random;

import org.pale.gorm.Building;
import org.pale.gorm.Castle;
import org.pale.gorm.Extent;
import org.pale.gorm.MaterialManager;
import org.pale.gorm.Room;
import org.pale.gorm.rooms.GenericShopRoom;
import org.pale.gorm.rooms.LibraryRoom;
import org.pale.gorm.rooms.SmithyRoom;

public class Shop extends Building {

	public Shop(Building parent) {
		setStandardInitialExtent(parent, 1, 1);
	}

	@Override
	public Building createChildBuilding(Random r) {
		return BuildingFactory.createRandom2(this, BuildingType.PATH,1,
				BuildingType.CORRIDOR,1,
				BuildingType.GARDEN,2,
				BuildingType.DWELLING,4);
	}

	@Override
	protected Room createRoom(MaterialManager mgr, Extent roomExt, Building bld) {
		switch (Castle.getInstance().r.nextInt(4)) {
		case 0:
		case 1:
		case 2:
			return new GenericShopRoom(mgr, roomExt, bld);
		case 3:
		default:
			return new SmithyRoom(mgr, roomExt, bld);
		}
	}

}
