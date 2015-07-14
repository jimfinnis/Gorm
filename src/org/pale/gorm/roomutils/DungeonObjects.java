package org.pale.gorm.roomutils;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.pale.gorm.Castle;
import org.pale.gorm.Config;
import org.pale.gorm.GormPlugin;
import org.pale.gorm.IntVector;
import org.pale.gorm.MaterialDataPair;

public class DungeonObjects {

	public static void chest(IntVector location, IntVector dir) {
		Castle c = Castle.getInstance();
		int grade = c.gradeInt(location);

		Block b = c.getBlockAt(location);

		b.setType(Material.CHEST);
		Chest chest = (Chest) b.getState();
		
		chest.getBlockInventory().clear();
		int items = (int) ((10 + c.r.nextInt(10)) / grade);
		for (int i = 1; i < items; i++) {
			// Choose an item ID from those available for this grade of room
			MaterialDataPair pair = Config.getLoot(grade);
			GormPlugin.getInstance().getLogger().info("--Adding "+pair.m.toString());
			chest.getBlockInventory().addItem(new ItemStack(pair.m));
		}

		
        chest.update();
		
	}

	public static void spawner(IntVector location, EntityType spawnEntity) {
		Castle c = Castle.getInstance();
		Block b = c.getBlockAt(location);

		b.setType(Material.MOB_SPAWNER);
		CreatureSpawner spawner = (CreatureSpawner) b.getState();
		spawner.setSpawnedType(spawnEntity);
		spawner.update();
	}

}
