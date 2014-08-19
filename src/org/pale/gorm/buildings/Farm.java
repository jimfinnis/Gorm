package org.pale.gorm.buildings;

import java.util.Random;

import org.bukkit.entity.Villager;
import org.pale.gorm.Building;
import org.pale.gorm.Castle;
import org.pale.gorm.Extent;
import org.pale.gorm.MaterialManager;
import org.pale.gorm.roomutils.Gardener;

public class Farm extends Garden {

	public Farm(Building parent) {
		super(parent);
	}

	@Override
	public Building createChildBuilding(Random r) {
		return BuildingFactory.createRandom(this, new BuildingType[] {
				BuildingType.PATH, BuildingType.LIBRARY, BuildingType.HALL,
				BuildingType.DWELLING, BuildingType.SHOP, });

	}

	@Override
	public void build(MaterialManager mgr) {
		Extent floor = prepareGround(mgr).expand(-1, Extent.X | Extent.Z);
		Gardener.makeFarm(floor);
	}
	
	@Override
	public void furnish(MaterialManager mgr) {
		if(Castle.getInstance().r.nextDouble() < 0.8){
			rooms.get(0).spawnVillager(Villager.Profession.FARMER);
		}
	}


}
