package org.pale.gorm.rooms;

import org.bukkit.Material;
import org.bukkit.entity.Villager;
import org.pale.gorm.Building;
import org.pale.gorm.Castle;
import org.pale.gorm.Direction;
import org.pale.gorm.Extent;
import org.pale.gorm.GormPlugin;
import org.pale.gorm.MaterialManager;
import org.pale.gorm.roomutils.Gardener;

public class RoofFarm extends RoofGarden {

	public RoofFarm(MaterialManager mgr, Extent e, Building b) {
		super(mgr, e, b);
	}

	@Override
	public Extent build(MaterialManager mgr, Extent buildingExtent) {
		Castle c = Castle.getInstance();
		// get the material for the underfloor; it's the same material as the
		// centre of the ceiling.
		Extent floor = e.getWall(Direction.DOWN);
		Material underFloorMaterial = floor.getCentre().getBlock().getType();

		// build the underfloor
		c.fill(floor.subvec(0, 1, 0), underFloorMaterial, 0);
		// clear the area
		c.fill(e.expand(-1, Extent.ALL), Material.AIR, 0);
		// build the floor, using same material as underfloor for edge
		c.checkFill(floor, underFloorMaterial, 0);

		Gardener.makeFarm(floor.expand(-1, Extent.X | Extent.Z));
		if (c.r.nextFloat() < 0.1) {
			spawnVillager(Villager.Profession.FARMER);
		}

		// fill in the perimeter
		perimeter(mgr, c);

		addSignHack();

		// set the new building extent
		buildingExtent.maxy = e.maxy;
		GormPlugin.log("updating building extent to "
				+ buildingExtent.toString());
		return buildingExtent; // return modified building extent
	}

	@Override
	public boolean canHaveHoleInFloor() {
		return false;
	}

	@Override
	public void furnish(MaterialManager mgr) {

	}
}
