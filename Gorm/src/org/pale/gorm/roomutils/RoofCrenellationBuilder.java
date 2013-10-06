package org.pale.gorm.roomutils;

import org.bukkit.Material;
import org.pale.gorm.Castle;
import org.pale.gorm.Direction;
import org.pale.gorm.Extent;
import org.pale.gorm.IntVector;
import org.pale.gorm.MaterialDataPair;
import org.pale.gorm.MaterialManager;
import org.pale.gorm.Turtle;

/**
 * A bit like the perimeter walls for roof gardens, this. BUT DELIBERATELY
 * DIFFERENT.
 * 
 * @author white
 * 
 */
public class RoofCrenellationBuilder {

	Extent e;

	public void buildRoof(MaterialManager mgr, Extent extent) {
		e = extent.getWall(Direction.UP);
		e.setHeight(5);
		e.addvec(0, 1, 0);
		perimeter(mgr,Castle.getInstance());
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
		
		MaterialDataPair main;

		switch (c.r.nextInt(6)) {
		case 0:
		default:
			main = mgr.getFence();
			break;
		case 1:
			main = mgr.getPrimary();
			break;
		case 2:
			main = mgr.getSecondary();
			break;
		case 3:
			main = mgr.getOrnament();
			break;
		case 4:
			main = mgr.getOrnament();
			break;
		}

		MaterialDataPair alt;
		switch (c.r.nextInt(6)) {
		case 0:
		default:
			alt = mgr.getSupSecondary();
			break;
		case 1:
			alt = mgr.getPrimary();
			break;
		case 2:
			alt = mgr.getSecondary();
			break;
		case 3:
			alt = mgr.getOrnament();
			break;
		case 4:
			alt = mgr.getSupSecondary();
			break;
		case 5:
			alt = mgr.getSupSecondary();
			break;
		}

		// this is a roof which doesn't need safety rails!
		if (c.r.nextFloat() < 0.4)
			main = new MaterialDataPair(Material.AIR, 0);
		if (c.r.nextFloat() < 0.4)
			alt = new MaterialDataPair(Material.AIR, 0);


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

	private static void placePost(MaterialManager mgr, Castle c, int x, int y,
			int z, int tp) {
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
