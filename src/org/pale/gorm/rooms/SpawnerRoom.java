package org.pale.gorm.rooms;

import java.util.ArrayList;

import org.pale.gorm.Building;
import org.pale.gorm.Castle;
import org.pale.gorm.Extent;
import org.pale.gorm.MaterialDataPair;
import org.pale.gorm.MaterialManager;
import org.pale.gorm.Room;
import org.pale.gorm.roomutils.DungeonObjects;
import org.bukkit.entity.EntityType;

/**
 * A Dark room to spawn mobs.
 * @author domos
 *
 */
public class SpawnerRoom extends Room {

	public SpawnerRoom(MaterialManager mgr,Extent e, Building b) {
		super(mgr, e, b);
	}
	
	/**
	 * The spawner room has no windows
	 * @return
	 */
	public boolean hasWindows(){
		return false;
	}

	@Override
	public Extent build(MaterialManager mgr, Extent buildingExtent) {
		Castle c = Castle.getInstance();
		// make the actual floor - first layer
		Extent floor = e.expand(-1, Extent.X | Extent.Z);
		floor.maxy = floor.miny;
		
		// fill with primary material
		MaterialDataPair prim = mgr.getPrimary();
		c.fill(floor,prim.m,prim.d);
		
		
		addSignHack();
		
		return null; // we don't modify the building extent
	}

	@Override
	public void furnish(MaterialManager mgr) {
		Castle c = Castle.getInstance();
		ArrayList<EntityType> entities = new ArrayList<EntityType>();
		entities.add(EntityType.CAVE_SPIDER);
		entities.add(EntityType.SKELETON);
		entities.add(EntityType.SPIDER);
		entities.add(EntityType.ZOMBIE);
		entities.add(EntityType.WITCH);
		
		Extent inner = e.expand(-1, Extent.ALL);
		DungeonObjects.spawner(inner,entities.get(c.r.nextInt(entities.size())), 1);
	}

}
