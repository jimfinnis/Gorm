package org.pale.gorm.rooms;

import org.bukkit.Material;
import org.pale.gorm.Building;
import org.pale.gorm.Castle;
import org.pale.gorm.Direction;
import org.pale.gorm.Extent;
import org.pale.gorm.MaterialDataPair;
import org.pale.gorm.MaterialManager;
import org.pale.gorm.Room;
import org.pale.gorm.roomutils.Furniture;

public class Gallery extends PlainRoom {

	public Gallery(MaterialManager mgr, Extent e, Building b) {
		super(mgr, e, b);
	}
	
	public static int getSize(Extent e){
		int minDim = Math.min(e.xsize(), e.zsize());
		int gallerySize = minDim/4;
		if(gallerySize>8)gallerySize=8;
		return gallerySize;
	}
	
	public static boolean bigEnough(Extent e){
		return getSize(e)>=2;
	}

	@Override
	public Extent build(MaterialManager mgr, Extent buildingExtent) {
		Castle c = Castle.getInstance();
		// make the actual floor - first layer
		Extent floor = e.expand(-1, Extent.X | Extent.Z).getWall(Direction.DOWN);
		// fill with primary material
		MaterialDataPair prim = mgr.getPrimary();
		c.fill(floor,prim.m,prim.d);
		
		int gallerySize = getSize(e);

		// make the fencing
		Extent fence = floor.expand(-gallerySize,Extent.X|Extent.Z).addvec(0, 1, 0);
		c.fill(fence, mgr.getFence());
		
		// make columns if required
		Extent colExt = new Extent(fence);
		colExt.maxy = e.maxy;
		Furniture.columns(this,colExt,mgr);
		
		
		// blow the hole in the floor, overwriting the fencing in the middle
		// we generated before
		Extent hole = floor.expand(-(gallerySize+1),Extent.X|Extent.Z).expand(1, Extent.Y);
		c.fill(hole,Material.AIR,0);
		
		

		if(b.gradeInt()>1)
			b.lightWalls(e.expand(-1, Extent.ALL));
			
			
		addSignHack();
		
		return null; // we don't modify the building extent
	}
}
