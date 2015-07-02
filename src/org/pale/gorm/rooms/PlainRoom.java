package org.pale.gorm.rooms;

import java.util.Random;

import org.bukkit.Material;
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
import org.pale.gorm.roomutils.WindowMaker;

/**
 * A room which requires no extra walls or contents to be built.
 * @author white
 *
 */
public class PlainRoom extends EmptyRoom {

	public PlainRoom(MaterialManager mgr, Extent e, Building b) {
		super(mgr, e, b,false);
	}

	@Override
	public void furnish(MaterialManager mgr) {
		Random r = Castle.getInstance().r;
		double grade = b.grade() + r.nextGaussian()*0.1; // get room grade
		
		furnishUniques(mgr);
		
		for(int i=0;i<e.xsize()*e.ysize()/10;i++){
			String f;
			if(grade<0.1)
				f = FurnitureItems.random(FurnitureItems.scaryChoices);
			else if(grade>1)
				f = FurnitureItems.random(FurnitureItems.poshChoices);
			else
				f = FurnitureItems.random(FurnitureItems.defaultChoices);
			Furniture.place(mgr,this, f);
		}

	}

}
