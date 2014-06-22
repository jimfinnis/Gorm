package org.pale.gorm.buildings;

import java.util.Random;

import org.pale.gorm.Building;
import org.pale.gorm.Castle;

public class BuildingFactory {
	
	static Building create(Building parent, BuildingType t){
		switch(t){
		default:
		case HALL:return new Hall(parent); 
		case LIBRARY:return new Library(parent);
		case GARDEN:return new Garden(parent);
		case DWELLING:return new Dwelling(parent);
		case PATH:return new Path(parent);
		case CORRIDOR:return new Corridor(parent);
		case SHOP:return new Shop(parent);
		case FARM:return new Farm(parent);
		}
	}
	
	static Building createRandom(Building parent,BuildingType[] arr){
		Random r = Castle.getInstance().r;
		return create(parent,arr[r.nextInt(arr.length)]);
	}
}
