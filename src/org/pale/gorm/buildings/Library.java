package org.pale.gorm.buildings;

import java.util.Random;

import org.pale.gorm.Building;
import org.pale.gorm.Extent;
import org.pale.gorm.MaterialManager;
import org.pale.gorm.Room;
import org.pale.gorm.rooms.LibraryRoom;

public class Library extends Building {

	public Library(Building parent) {
		setStandardInitialExtent(parent,1,1);
	}
	
	@Override
	public Building createChildBuilding(Random r) {
		return BuildingFactory.createRandom2(this, 
				BuildingType.PATH,1,
				BuildingType.GARDEN,1,
				BuildingType.CORRIDOR,1,
				BuildingType.HALL,1,
				BuildingType.LIBRARY,2,
				BuildingType.DWELLING,1
				);
	}
	
	
	@Override
	protected Room createRoom(MaterialManager mgr, Extent roomExt, Building bld) {
		return new LibraryRoom(mgr, roomExt, bld);
	}
}
