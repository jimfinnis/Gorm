package org.pale.gorm.rooms;

import org.bukkit.Location;
import org.bukkit.entity.Villager;
import org.pale.gorm.Building;
import org.pale.gorm.Castle;
import org.pale.gorm.Extent;
import org.pale.gorm.MaterialManager;
import org.pale.gorm.Room;
import org.pale.gorm.roomutils.Furniture;
import org.pale.gorm.roomutils.FurnitureItems;
import org.pale.gorm.roomutils.WindowMaker;

public class LibraryRoom extends PlainRoom {

	public LibraryRoom(MaterialManager mgr, Extent e, Building b) {
		super(mgr, e, b);
	}
	
	
	
	@Override
	public Extent build(MaterialManager mgr, Extent buildingExtent) {
		Extent e = super.build(mgr, buildingExtent);
		// they take their libraries seriously.
		if(Castle.getInstance().r.nextDouble()<0.2)
			WindowMaker.makeStainedGlassWall(mgr, this);
		return e;
	}



	@Override
	public void furnish(MaterialManager mgr) {
		furnishUniques(mgr);

		for(int i=0;i<e.xsize()*e.ysize()/5;i++){
			String f = FurnitureItems.random(FurnitureItems.libraryChoices);
			Furniture.place(mgr,this, f);
		}
		
		double chanceOfVillager = b.grade(); 

		if(Castle.getInstance().r.nextDouble() < chanceOfVillager)
			spawnVillager(Villager.Profession.LIBRARIAN);
	}
}
