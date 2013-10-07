package org.pale.gorm.roomutils;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.inventory.ItemStack;
import org.pale.gorm.Castle;
import org.pale.gorm.Extent;
import org.pale.gorm.GormPlugin;

public class DungeonObjects {


	/**
	 * Generates Random Loot Chests
	 * 
	 * @param floor - defines the area in which the chest will spawn
	 * @param chance - how likely each room is to have a chest
	 */
	public static void chest(Extent floor, double chance) {
		Castle c = Castle.getInstance();
		World w = c.getWorld();
		floor = new Extent(floor);
		floor.minx = floor.minx + (floor.xsize()/2);
		floor.minz = floor.minz + (floor.zsize()/2);
		Block b = w.getBlockAt(floor.minx,floor.miny + 1,floor.minz);
		if (c.r.nextFloat() <= chance){
			b.setType(Material.CHEST);
			Chest chest = (Chest) b.getState();
			for(int i=1;i<(2+(int)(c.r.nextFloat()*((10-2)+1)));i++){
				chest.getBlockInventory().addItem(new ItemStack(256+(int)(c.r.nextFloat()*((382-256)+1))));
			}
			chest.update();
			GormPlugin.log("Chest Generated @ " + floor.minx + " " + floor.miny + " " + floor.minz);
		}
	}
	
}
