package org.pale.gorm.rooms;

import org.bukkit.Material;
import org.pale.gorm.Building;
import org.pale.gorm.Castle;
import org.pale.gorm.Direction;
import org.pale.gorm.Extent;
import org.pale.gorm.IntVector;
import org.pale.gorm.Room;
import org.pale.gorm.Turtle;

/**
 * A roof garden - must be the last room added to a building. Also changes the building extent.
 * @author white
 *
 */
public class RoofGarden extends Room {

	public RoofGarden(Extent e, Building b) {
		super(e, b, true);
	}

	/**
	 * Note - this changes the extent of the building, adding a new "room" on top.
	 */
	@Override
	public void build(Extent buildingExtent) {
		Castle c = Castle.getInstance();
		// get the material for the underfloor; it's the same material as the
		// centre of the ceiling.
		Extent floor = e.getWall(Direction.DOWN);
		Material underFloorMaterial = floor.getCentre()
				.getBlock().getType();
		
		
		// build the underfloor
		c.fill(floor.subvec(0,1,0),underFloorMaterial,0);
		// clear the area
		c.fill(e.expand(-1, Extent.ALL), Material.AIR, 0);
		// build the floor, using same material as underfloor for edge
		c.checkFill(floor, underFloorMaterial, 0);
		c.fill(floor.expand(-1,Extent.X|Extent.Z), Material.GRASS, 0);
		
		// fill in the perimeter
		perimeter(c);
		
		
		// set the new building extent
		buildingExtent.maxy = e.maxy;
	}

	private void perimeter(Castle c) {
		// place corner blocks
		// for each side, use turtle to place wall. If walls are odd length, consider alternating
		// wall blocks.
	}

}
