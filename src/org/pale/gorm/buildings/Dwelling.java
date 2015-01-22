package org.pale.gorm.buildings;

import java.util.Random;

import org.pale.gorm.Building;
import org.pale.gorm.Extent;
import org.pale.gorm.MaterialManager;
import org.pale.gorm.Room;
import org.pale.gorm.rooms.DwellingRoom;

public class Dwelling extends Building {
	public Dwelling(Building parent){
		setStandardInitialExtent(parent,1,1);
	}
	
	@Override
	public Building createChildBuilding(Random r) {
		return BuildingFactory.createRandom2(this, 
				BuildingType.CORRIDOR,3,
				BuildingType.DWELLING,8,
				BuildingType.GARDEN,7,
				BuildingType.FARM,2,
				BuildingType.HALL,5);
	}
	
	
	@Override
	protected Room createRoom(MaterialManager mgr, Extent roomExt, Building bld) {
		return new DwellingRoom(mgr, roomExt, bld);
	}
}
