package org.pale.gorm.roomutils;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.pale.gorm.Castle;
import org.pale.gorm.Direction;
import org.pale.gorm.Extent;
import org.pale.gorm.GormPlugin;
import org.pale.gorm.IntVector;

public class DungeonObjects {


	/**
	 * Generates Random Loot Chests
	 * 
	 * @param floor - defines the area in which the chest will spawn
	 * @param chance - how likely each room is to have a chest
	 */
	public static void chest(Extent inner, double chance) {
		Castle c = Castle.getInstance();
		World w = c.getWorld();
		
		// I've tidied this up a bit using IntVector. The call to getWall is probably
		// unnecessary - jcf
		IntVector centreFloor = inner.getWall(Direction.DOWN).getCentre();
		Block b = c.getBlockAt(centreFloor);
		
		if (c.r.nextFloat() <= chance){
			b.setType(Material.CHEST);
			Chest chest = (Chest) b.getState();
			for(int i=1;i<(2+(int)(c.r.nextFloat()*((10-2)+1)));i++){
				chest.getBlockInventory().addItem(new ItemStack(256+(int)(c.r.nextFloat()*((382-256)+1))));
			}
			chest.update();
			GormPlugin.log("Chest Generated @ " + centreFloor);
		}
	}
	
	public static void spawner(Extent inner, EntityType spawnEntity, double chance) {
		Castle c = Castle.getInstance();
		IntVector centreFloor = inner.getWall(Direction.DOWN).getCentre();
		Block b = c.getBlockAt(centreFloor);
		if (c.r.nextFloat() <= chance){
			b.setType(Material.MOB_SPAWNER);
			CreatureSpawner spawner = (CreatureSpawner) b.getState();
			spawner.setSpawnedType(spawnEntity);
			spawner.update();
			GormPlugin.log("Spawner Generated @ " + centreFloor);
		}
	}
	
}
