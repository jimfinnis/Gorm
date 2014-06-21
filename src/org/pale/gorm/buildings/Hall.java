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
		setStandardInitialExtent(parent, 1.5, 1.5);
	}

	@Override
	public Building createChildBuilding(Random r) {
		return BuildingFactory.createRandom(this, new BuildingType[] {
				BuildingType.PATH, BuildingType.GARDEN, BuildingType.GARDEN,
				BuildingType.CORRIDOR, BuildingType.CORRIDOR,
				BuildingType.HALL, BuildingType.HALL, });
	}
}
