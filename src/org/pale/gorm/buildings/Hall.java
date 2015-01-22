package org.pale.gorm.buildings;

import java.util.Random;

import org.pale.gorm.Building;
import org.pale.gorm.Extent;

/**
 * A hall with stone walls and multiple floors
 * 
 * @author white
 * 
 */
public class Hall extends Building {

	public Hall(Extent e) {
		setInitialExtent(e);
	}

	public Hall(Building parent) {
		setStandardInitialExtent(parent, 2, 2);
	}

	@Override
	public Building createChildBuilding(Random r) {
		return BuildingFactory.createRandom2(this,
				BuildingType.PATH,1,
				BuildingType.GARDEN,1,
				BuildingType.CORRIDOR,2,
				BuildingType.HALL,2);
	}
}
