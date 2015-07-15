package org.pale.gorm;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.pale.gorm.Building;
import org.pale.gorm.Castle;
import org.pale.gorm.Extent;
import org.pale.gorm.MaterialDataPair;
import org.pale.gorm.MaterialManager;
import org.pale.gorm.Room;
import org.pale.gorm.roomutils.Furniture;

/**
 * Was the chest room, now may contain no chest! That's done using the standard furniture
 * mechanism.
 * Is now also the base class of all rooms with a floor.
 * 
 * @author domos
 *
 */
public class SnarkRoom extends Room {
	public SnarkRoom(MaterialManager mgr,Extent e, Building b) {
		super(mgr, e, b);
	}

	@Override
	public Extent build(MaterialManager mgr, Extent buildingExtent) {
		Castle c = Castle.getInstance();
		// make the actual floor - first layer
		Extent floor = e.expand(-1, Extent.X | Extent.Z);
		floor.maxy = floor.miny;
		
		// fill with floor material
		MaterialDataPair f = mgr.getFloor();
		c.fill(floor,f.m,f.d);

		lightsAndCarpets(true);
		
		
		IntVector pos = e.getCentre(); pos.y = e.miny + 1;
		
		 Block blk = Castle.getInstance().getBlockAt(pos);
		 blk.setType(Material.SIGN_POST); Sign s = (Sign) blk.getState();
		 s.setLine(0, "SNARK ROOM");
		 s.update();
		
		
		return null; // we don't modify the building extent
	}

	@Override
	public void furnish(MaterialManager mgr) {
	}

}
