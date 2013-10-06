package org.pale.gorm.roomutils;

import org.bukkit.Material;
import org.pale.gorm.Castle;
import org.pale.gorm.Direction;
import org.pale.gorm.Extent;
import org.pale.gorm.MaterialDataPair;
import org.pale.gorm.MaterialManager;

/**
 * A class containing static methods to help build hollow boxes - i.e. buildings - in different ways
 * @author white
 *
 */
public class BoxBuilder {
	public static void build(MaterialManager mgr,Extent e){
		switch(Castle.getInstance().r.nextInt(3)){
		case 0:
		case 1:
			plain(mgr,e);break;
		case 2:
			bevelled(mgr,e);break;
		}
	}
	public static void plain(MaterialManager mgr,Extent e){
		// fill with primary material
		Castle c = Castle.getInstance();
		MaterialDataPair prim = mgr.getPrimary();
		c.fill(e,prim.m,prim.d);
		c.fill(e.expand(-1, Extent.ALL), Material.AIR, 1); // 'inside' air
	}
	public static void bevelled(MaterialManager mgr,Extent e){
		Castle c = Castle.getInstance();
		MaterialDataPair prim = mgr.getPrimary();
		
		c.fill(e.getWall(Direction.NORTH).expand(-1, Extent.X), prim);
		c.fill(e.getWall(Direction.SOUTH).expand(-1, Extent.X), prim);
		c.fill(e.getWall(Direction.EAST).expand(-1, Extent.Z), prim);
		c.fill(e.getWall(Direction.WEST).expand(-1, Extent.Z), prim);
		
		// easiest way to build the ceiling and floor while still maintaining
		// the "cross" shape
		c.fill(e.getWall(Direction.UP).expand(-1, Extent.X),prim);
		c.fill(e.getWall(Direction.UP).expand(-1, Extent.Z),prim);
		c.fill(e.getWall(Direction.DOWN).expand(-1, Extent.X),prim);
		c.fill(e.getWall(Direction.DOWN).expand(-1, Extent.Z),prim);

		c.fill(e.expand(-1, Extent.ALL), Material.AIR, 1); // 'inside' air

	}
}
