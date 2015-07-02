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

public class GenericShopRoom extends PlainRoom {

	public GenericShopRoom(MaterialManager mgr, Extent roomExt, Building bld) {
		super(mgr,roomExt,bld);
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
