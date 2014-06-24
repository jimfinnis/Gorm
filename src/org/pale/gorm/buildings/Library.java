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
		return BuildingFactory.createRandom(this, 
				new BuildingType[] {
				BuildingType.PATH,
				BuildingType.GARDEN,
				BuildingType.CORRIDOR,
				BuildingType.HALL,
				BuildingType.LIBRARY,
				BuildingType.LIBRARY,
				BuildingType.DWELLING
				
				});
	}
	
	
	@Override
	protected Room createRoom(MaterialManager mgr, Extent roomExt, Building bld) {
		return new LibraryRoom(mgr, roomExt, bld);
	}
}
