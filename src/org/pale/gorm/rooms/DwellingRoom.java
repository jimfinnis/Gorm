package org.pale.gorm.rooms;

import org.pale.gorm.Building;
import org.pale.gorm.Extent;
import org.pale.gorm.MaterialManager;
import org.pale.gorm.roomutils.Furniture;
import org.pale.gorm.roomutils.FurnitureItems;

public class DwellingRoom extends PlainRoom {

	public DwellingRoom(MaterialManager mgr, Extent e, Building b) {
		super(mgr, e, b);
	}


	@Override
	public void furnish(MaterialManager mgr) {
		furnishUniques(mgr);

		for(int i=0;i<e.xsize()*e.ysize()/10;i++){
			String f = FurnitureItems.random(FurnitureItems.dwellingChoices);
			Furniture.place(mgr,this, f);
		}

	}

}
