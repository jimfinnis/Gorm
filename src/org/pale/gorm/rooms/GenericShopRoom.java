package org.pale.gorm.rooms;

import org.bukkit.entity.Villager;
import org.pale.gorm.Building;
import org.pale.gorm.Castle;
import org.pale.gorm.Direction;
import org.pale.gorm.Extent;
import org.pale.gorm.MaterialDataPair;
import org.pale.gorm.MaterialManager;
import org.pale.gorm.Room;
import org.pale.gorm.roomutils.Furniture;
import org.pale.gorm.roomutils.FurnitureItems;

public class GenericShopRoom extends Room {

	public GenericShopRoom(MaterialManager mgr, Extent roomExt, Building bld) {
		super(mgr,roomExt,bld);
	}

	@Override
	public Extent build(MaterialManager mgr, Extent buildingExtent) {
		Castle c = Castle.getInstance();
		// make the actual floor - first layer
		Extent floor = e.expand(-1, Extent.X | Extent.Z).getWall(Direction.DOWN);
		// fill with primary material
		MaterialDataPair prim = mgr.getPrimary();
		c.fill(floor,prim.m,prim.d);

		lightsAndCarpets(false);
		addSignHack();
		return null; // we don't modify the building extent
	}

	@Override
	public void furnish(MaterialManager mgr) {
		furnishUniques(mgr);

		for(int i=0;i<e.xsize()*e.ysize()/10;i++){
			String f = FurnitureItems.random(FurnitureItems.shopChoices);
			Furniture.place(mgr,this, f);
		}
		
		double chanceOfVillager = b.grade(); 

		if(Castle.getInstance().r.nextDouble() < chanceOfVillager){
			spawnVillager(Villager.Profession.BUTCHER);
		}
	}
	
	

}
