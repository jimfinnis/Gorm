package org.pale.gorm.rooms;

import org.bukkit.Material;
import org.pale.gorm.Building;
import org.pale.gorm.Castle;
import org.pale.gorm.Direction;
import org.pale.gorm.Extent;
import org.pale.gorm.GormPlugin;
import org.pale.gorm.IntVector;
import org.pale.gorm.MaterialDataPair;
import org.pale.gorm.MaterialManager;
import org.pale.gorm.Room;
import org.pale.gorm.Turtle;
import org.pale.gorm.roomutils.Gardener;

/**
 * A roof garden - must be the last room added to a building. Also changes the
 * building extent.
 * 
 * @author white
 * 
 */
public class RoofGarden extends Room {

	public RoofGarden(MaterialManager mgr, Extent e, Building b) {
		super(mgr, e, b);
		setAllSidesOpen();
	}

	/**
	 * Note - this changes the extent of the building, adding a new "room" on
	 * top.
	 */
	@Override
	public Extent build(MaterialManager mgr, Extent buildingExtent) {
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
		c.fill(floor.expand(-1, Extent.X | Extent.Z), mgr.getGround());

		// fill in the perimeter
		perimeter(mgr, c);
		
		Gardener.plant(floor.expand(-1,Extent.X|Extent.Z));
		
		addSignHack();

		// set the new building extent
		buildingExtent.maxy = e.maxy;
		GormPlugin.log("updating building extent to "
				+ buildingExtent.toString());
		return buildingExtent; // return modified building extent
	}

	private void perimeter(MaterialManager mgr, Castle c) {
		// bitfield describing the perimeter posts
		// bits 0-2 are the post types
		// bit 4 if means 'alternate block with secondary if wall is odd length'
		// bit 5 means alts have torches
		// bits 6-7 if both are 11 means 'use steps instead of primary block'
		int tp = c.r.nextInt();
		placePost(mgr, c, e.minx, e.miny, e.minz, tp);
		placePost(mgr, c, e.maxx, e.miny, e.minz, tp);
		placePost(mgr, c, e.minx, e.miny, e.maxz, tp);
		placePost(mgr, c, e.maxx, e.miny, e.maxz, tp);

		boolean alternate = (tp & 16) == 1;
		boolean useStepsAsWall = ((tp >> 5) & 3) == 3;
		boolean altTorch = (tp & 32) == 1;

		alternate = true;
		useStepsAsWall = true;

		// for each side, use turtle to place wall. If walls are odd length,
		// consider alternating
		// wall blocks.

		Turtle t;
		for (Direction d : Direction.values()) {
			IntVector v;
			int len;
			switch (d) {
			case NORTH:
				v = new IntVector(e.minx, e.miny, e.maxz);
				len = e.zsize();
				break;
			case SOUTH:
				v = new IntVector(e.maxx, e.miny, e.minz);
				len = e.zsize();
				break;
			case EAST:
				v = new IntVector(e.minx, e.miny, e.minz);
				len = e.xsize();
				break;
			case WEST:
				v = new IntVector(e.maxx, e.miny, e.maxz);
				len = e.xsize();
				break;
			default:
				v = null;
				len = 0;
			}

			if ((len & 1) == 0) // make sure we can't alternate on even walls.
								// Looks weird.
				alternate = false;

			MaterialDataPair main = mgr.getFence();
			MaterialDataPair alt = mgr.getSupSecondary();

			if (v != null) {
				t = new Turtle(mgr, c.getWorld(), v, d);
				if (useStepsAsWall)
					t.setMaterial(mgr.getStair());
				else
					t.setMaterial(main);

				t.setMaterial(main);
				t.up();
				for (int i = 0; i < 200; i++) {
					t.forwards();
					if (useStepsAsWall) {
						if (alternate) {
							if ((i & 1) != 0)
								t.setMaterial(alt);
							else
								t.setMaterial(mgr.getStair());
						}
						t.rotate(1);
						if (!t.write())
							break;
						t.rotate(-1);
					} else {
						if (alternate)
							t.setMaterial((i & 1) == 0 ? main : alt);
						if (!t.write())
							break;
						if (alternate && altTorch)
							t.run("Mtuwd");
					}

				}
			}

		}

	}

	private static void placePost(MaterialManager mgr, Castle c, int x, int y, int z,
			int tp) {
		Turtle t = new Turtle(mgr, c.getWorld(), new IntVector(x, y + 1, z),
				Direction.NORTH);
		switch (tp & 7) {
		case 0:
			t.run("m1wu.Mtw");
			break;
		case 1:
			t.run("m2wu.m2wu.Mtw");
			break;
		case 2:
			t.run("mowu.m1wu.Mtw");
			break;
		case 3:
			t.run("m1wu.mpwu.Mtw");
			break;
		case 4:
			t.run("mowu.mpwu.Mtw");
			break;
		case 5:
			t.run("mpwu.Mtw");
			break;
		case 6:
			t.run("mowu.Mtw");
			break;
		case 7:
			t.run("m1wu.m2wu.m1wu.m2w.Mt.fwbbwfLwRRw");
			break;
		}
	}

}
