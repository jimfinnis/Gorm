package org.pale.gorm.rooms;

import org.bukkit.Material;
import org.bukkit.entity.Villager;
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
import org.pale.gorm.roomutils.Furniture;
import org.pale.gorm.roomutils.Gardener;

/**
 * A roof garden - must be the last room added to a building. Also changes the
 * building extent.
 * 
 * @author white
 * 
 */
public class RoofGarden extends Room {

	private boolean isFarm = false;

	public RoofGarden(MaterialManager mgr, Extent e, Building b) {
		super(mgr, e, b);
		setAllSidesOpen();
		setOutside();
	}

	@Override
	public boolean canBeBelowGallery() {
		return false;
	}

	@Override
	public boolean tallNeighbourRequired() {
		return false;
	}
	

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

		addSignHack();

		// set the new building extent
		buildingExtent.maxy = e.maxy;
		GormPlugin.log("updating building extent to "
				+ buildingExtent.toString());
		return buildingExtent; // return modified building extent
	}



	protected void perimeter(MaterialManager mgr, Castle c) {
		// bitfield describing the perimeter posts
		// bits 0-2 are the post types
		// bit 4 if means 'alternate block with secondary if wall is odd length'
		// bit 5 means alts have torches
		// bits 6-7 if both are 11 means 'use steps instead of primary block'
		int tp = c.r.nextInt();
		Furniture.placePost(mgr, this, c, e.minx, e.miny, e.minz, tp);
		Furniture.placePost(mgr, this, c, e.maxx, e.miny, e.minz, tp);
		Furniture.placePost(mgr, this, c, e.minx, e.miny, e.maxz, tp);
		Furniture.placePost(mgr, this, c, e.maxx, e.miny, e.maxz, tp);

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

	@Override
	public void furnish(MaterialManager mgr) {
		Extent floor = e.getWall(Direction.DOWN);
		if (Castle.getInstance().r.nextFloat() < 0.5)
			Gardener.plant(mgr,floor.expand(-1, Extent.X | Extent.Z));
	}

}
