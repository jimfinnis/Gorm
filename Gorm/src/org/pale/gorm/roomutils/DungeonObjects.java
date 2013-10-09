package org.pale.gorm.roomutils;

import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.pale.gorm.Building;
import org.pale.gorm.Castle;
import org.pale.gorm.Direction;
import org.pale.gorm.Extent;
import org.pale.gorm.GormPlugin;
import org.pale.gorm.IntVector;
import org.pale.gorm.MaterialManager;
import org.pale.gorm.Room;

public class DungeonObjects {
	
	/**
	 * Generates Random Loot Chests
	 * 
	 * @param inner defines the internal area of the room
	 * @param chance how likely each chest room is to have a chest
	 * @param grade the grade, or danger level, of the current room,
	 * 			from 1 (most dangerous) to 4 (pretty much completely safe)
	 */
	public static void chest(Extent inner, double chance) {
		Castle c = Castle.getInstance();
		int grade = c.gradeInt(inner);
		
		IntVector centreFloor = inner.getWall(Direction.DOWN).getCentre();
		Block b = c.getBlockAt(centreFloor);
		
		if (c.r.nextFloat() <= chance){
			b.setType(Material.CHEST);
			Chest chest = (Chest) b.getState();
			GormPlugin plugin = new GormPlugin();
			ArrayList<Integer> loot = plugin.getLoot(grade);
			//How many items should the chest contain? Lower grade (more dangerous) areas get more
			int items = (int) ((10 + c.r.nextInt(10)) / grade);
			for(int i=1;i<items;i++){
				//Choose an item ID from those available for this grade of room
				int itemID = loot.get(c.r.nextInt(loot.size()));
				chest.getBlockInventory().addItem(new ItemStack(itemID));
			}
			chest.update();
			GormPlugin.log("Chest Generated @ " + centreFloor);
		}
	}
	
	/**
	 * Generates a spawner in the centre of the room
	 * @param inner defines the internal area of the room
	 * @param spawnEntity the entity type to be spawned by the spawner
	 * @param chance how likely each spawner room is to have a spawner
	 */
	
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
