package org.pale.gorm.roomutils;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.pale.gorm.Builder;
import org.pale.gorm.Castle;
import org.pale.gorm.Direction;
import org.pale.gorm.Extent;
import org.pale.gorm.GormPlugin;
import org.pale.gorm.IntVector;
import org.pale.gorm.MaterialManager;
import org.pale.gorm.Turtle;

public class Furniture {
	public static void cornerShelves(Extent e, MaterialManager mgr, int carpet) {
		GormPlugin.log("Beginning Shelves");
		Extent corner1 = new Extent(e.minx, e.miny, e.maxz - 1);
		corner1.maxx++;
		corner1.maxy = e.maxy;
		corner1.maxz = e.maxz;
		Castle c = Castle.getInstance();
		c.fill(corner1, Material.BOOKSHELF, 0);
		Extent cookieCutter = new Extent(corner1.maxx, corner1.miny,
				corner1.minz);
		cookieCutter.maxy = corner1.maxy;
		c.fill(cookieCutter, Material.AIR, 0);
		cookieCutter.maxy = cookieCutter.miny;
		if (carpet >= 0)
			c.fill(cookieCutter, Material.CARPET, carpet);
		GormPlugin.log("Shelves at " + corner1.maxx + " " + corner1.maxy + " "
				+ corner1.maxz);
		Extent corner2 = new Extent(e.maxx - 1, e.miny, e.minz);
		corner2.maxx = e.maxx;
		corner2.maxy = e.maxy;
		corner2.maxz++;
		c.fill(corner2, Material.BOOKSHELF, 0);
		Extent cookieCutter2 = new Extent(corner2.minx, corner2.miny,
				corner2.maxz);
		cookieCutter2.maxy = corner2.maxy;
		c.fill(cookieCutter2, Material.AIR, 0);
		cookieCutter2.maxy = cookieCutter2.miny;
		if (carpet >= 0)
			c.fill(cookieCutter2, Material.CARPET, carpet);
		GormPlugin.log("Shelves at " + corner2.maxx + " " + corner2.maxy + " "
				+ corner2.maxz);
	}
	
	/**
	 * Given a furniture description string, test to see if we can place it within the given
	 * extent
	 * @param mgr material manager
	 * @param pos the initial position of the centre back of the furniture
	 * @param d the direction facing towards the back of the furniture from the front
	 * @param e the limiting extent, beyond which we definitely can't write
	 * @param s the string to test
	 * @return an integer which is -1 if we can't place it, and some other value if 
	 * 			we can - the higher the value, the better
	 */
	public static int testPlacement(MaterialManager mgr,IntVector pos, Direction d, Extent e, String s){
		Turtle t = new Turtle(mgr,Castle.getInstance().getWorld(),pos,d);
		t.setModeFlag(Turtle.TEST);
		return -1;
	}

}
