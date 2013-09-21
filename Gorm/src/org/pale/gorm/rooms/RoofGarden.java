package org.pale.gorm.rooms;

import org.bukkit.Material;
import org.pale.gorm.Building;
import org.pale.gorm.Castle;
import org.pale.gorm.Direction;
import org.pale.gorm.Extent;
import org.pale.gorm.GormPlugin;
import org.pale.gorm.IntVector;
import org.pale.gorm.Room;
import org.pale.gorm.Turtle;

/**
 * A roof garden - must be the last room added to a building. Also changes the
 * building extent.
 * 
 * @author white
 * 
 */
public class RoofGarden extends Room {

	public RoofGarden(Extent e, Building b) {
		super(e, b, true);
	}

	/**
	 * Note - this changes the extent of the building, adding a new "room" on
	 * top.
	 */
	@Override
	public Extent build(Extent buildingExtent) {
		Castle c = Castle.getInstance();
		// get the material for the underfloor; it's the same material as the
		// centre of the ceiling.
		Extent floor = e.getWall(Direction.DOWN);
		Material underFloorMaterial = floor.getCentre().getBlock().getType();

		// build the underfloor
		c.fill(floor.subvec(0, 1, 0), underFloorMaterial, 0);
		// clear the area
		c.fill(e.expand(-1, Extent.ALL), Material.AIR, 0);
		// build the floor, using same material as underfloor for edge
		c.checkFill(floor, underFloorMaterial, 0);
		c.fill(floor.expand(-1, Extent.X | Extent.Z), Material.GRASS, 0);

		// fill in the perimeter
		perimeter(c);

		// set the new building extent
		buildingExtent.maxy = e.maxy;
		GormPlugin.log("updating building extent to "+buildingExtent.toString());
		return buildingExtent;
	}

	private void perimeter(Castle c) {
		int tp = c.r.nextInt(); // bitfield describing the perimeter posts
		placePost(c, e.minx, e.miny, e.minz, tp);
		placePost(c, e.maxx, e.miny, e.minz, tp);
		placePost(c, e.minx, e.miny, e.maxz, tp);
		placePost(c, e.maxx, e.miny, e.maxz, tp);

		// for each side, use turtle to place wall. If walls are odd length,
		// consider alternating
		// wall blocks.

		Turtle t;
		for (Direction d : Direction.values()) {
			IntVector v;
			switch (d) {
			case NORTH:
				v = new IntVector(e.minx, e.miny, e.maxz);
				break;
			case SOUTH:
				v = new IntVector(e.maxx, e.miny, e.minz);
				break;
			case EAST:
				v = new IntVector(e.minx, e.miny, e.minz);
				break;
			case WEST:
				v = new IntVector(e.maxx, e.miny, e.maxz);
				break;
			default:
				v = null;
			}
			if (v != null) {
				t = new Turtle(c.getWorld(), v, d);
				t.setMaterial(Material.COBBLE_WALL,0); // change this!
				t.up();
				if ((tp & 32) != 0)
					t.up();
				for (;;) {
					t.forwards();
					if (!t.write())
						break;

				}
			}

		}

	}

	private void placePost(Castle c, int x, int y, int z, int tp) {
		Turtle t = new Turtle(c.getWorld(), new IntVector(x, y + 1, z),
				Direction.NORTH);
		boolean alternateMaterial = (tp & 16) == 0;
		switch (tp & 7) {
		case 0:
			if (alternateMaterial) {
				t.setMaterial(Material.SMOOTH_BRICK,3);
			} else
				t.setMaterial(Material.FENCE);
			break;
		case 1:
			t.setMaterial(Material.SMOOTH_BRICK);
			break;
		case 2:
			t.setMaterial(Material.COBBLESTONE);
			break;
		case 3:
			t.setMaterial(Material.SMOOTH_BRICK,3);
			break;
		}
		if ((tp & 32) != 0)
			t.run("wuwumtw");
		else
			t.run("wumtw");
	}

}
