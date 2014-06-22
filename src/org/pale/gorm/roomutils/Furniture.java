package org.pale.gorm.roomutils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.pale.gorm.Builder;
import org.pale.gorm.Castle;
import org.pale.gorm.Direction;
import org.pale.gorm.Extent;
import org.pale.gorm.GormPlugin;
import org.pale.gorm.IntVector;
import org.pale.gorm.MaterialManager;
import org.pale.gorm.Room;
import org.pale.gorm.Turtle;

public class Furniture {


	/**
	 * Private class used to store placement candidates for furniture placement
	 * 
	 * @author white
	 * 
	 */
	private static class PlacementCandidate {
		IntVector p;
		Direction d;
		int score;

		PlacementCandidate(IntVector p, Direction d, int score) {
			this.score = score;
			this.d = d;
			this.p = p;
		}
	}

	/**
	 * Try to place a given bit of furniture - defined by a string - into a room
	 * The string defines the furniture as seen from the centre of its back,
	 * facing towards the back.
	 */
	public static void place(MaterialManager mgr, Room r, String s) {
		Castle c = Castle.getInstance();
		// we keep a list of candidates
		ArrayList<PlacementCandidate> candidates = new ArrayList<PlacementCandidate>();
		
		int maxScore = 0; // candidate max score

		// now we iterate through each wall of the inner space
		Extent innerTrue = r.getExtent().expand(-1, Extent.ALL);
		
		for (Direction wallDir : Direction.values()) {
			Extent inner=new Extent(innerTrue);
			
			// pull in the extent if the axis is long; this puts
			// furniture away from the walls.
			if(inner.xsize()>6)
				inner=inner.expand(-(c.r.nextInt(3)+1), Extent.X);
			if(inner.zsize()>6)
				inner=inner.expand(-(c.r.nextInt(3)+1), Extent.Z);
			
			if (wallDir.vec.y == 0) { // disregard floor/ceiling
				Extent wall = inner.getWall(wallDir);
				// and we move into the room along the opposite vector
				IntVector moveInwardsDir = wallDir.opposite().vec;
				IntVector moveAlongDir = (wallDir.vec.x == 0) ? Direction.EAST.vec
						: Direction.SOUTH.vec;
				int maxMovesInwards = (wallDir.vec.x == 0 ? inner.zsize()
						: inner.xsize()) / 2;
				int maxMovesAlong = (wallDir.vec.x == 0 ? inner.xsize() : inner
						.zsize());

				IntVector posStart = wall.getCorner(Extent.ALL); // get minimum
				for (int movesInwards = 0; movesInwards < maxMovesInwards; movesInwards++) {
					IntVector pos = posStart.add(moveInwardsDir
							.scale(movesInwards));
					// trying each position along that wall
					for (int movesAlong = 0; movesAlong < maxMovesAlong; movesAlong++) {
						// c.getBlockAt(pos).setType(Material.EMERALD_BLOCK);
						// now try to position the furniture there
						Turtle t = new Turtle(mgr, c.getWorld(), pos, wallDir);
						// make sure there's something under every block we make 
						// at floor height
						t.setRoom(r).setModeFlag(Turtle.ENSUREFLOORSUPPORT);
						t.setRoom(r).setModeFlag(Turtle.TEST); // we're just
																// testing, not
																// building,
																// initially

						// try putting the furniture here; if it fits add it as
						// a candidate with its score
						if (t.run(s)) {
							// if the score is lower than the current max score,
							// discard;
							// if the max score has increased, restart the list
							// (throwing away
							// all those lower-scoring candidates
							int score = t.getTestScore();
							if (movesInwards == 0)
								score += 4; // add score to those near the edge
							if (score >= maxScore) {
								if (score > maxScore) {
									candidates.clear();
									maxScore = score;
								}
/*								GormPlugin.log("adding candidate "
										+ pos.toString() + ":"
										+ wallDir.toString() + " score "
										+ t.getTestScore());
*/								PlacementCandidate pc = new PlacementCandidate(
										pos, wallDir, t.getTestScore());
								candidates.add(pc);
							}
						}
						pos = pos.add(moveAlongDir);
					}
				}
			}
		}

		if (candidates.size() != 0) {
			// and we're done - pick at random from the candidates
			PlacementCandidate pc = candidates.get(c.r.nextInt(candidates
					.size()));
//			GormPlugin.log("PICKED candidate " + pc.p.toString() + ":"
//					+ pc.d.toString() + " score " + pc.score);
			// and run a turtle to actually build the furniture
			Turtle t = new Turtle(mgr, c.getWorld(), pc.p, pc.d).setRoom(r);
			t.run(s);
			r.addBlock(t.getWritten()); // add all written blocks to blocked off area
		} //else GormPlugin.log("No candidates found!");
	}

	/**
	 * Generate columns at the edges of an extent
	 * @param e
	 */
	public static void columns(Room r,Extent e, MaterialManager mgr) {
		// we need a columnar spacing such that (width-1)%spacing and (height-1)%spacing
		// are both zero.
		Castle c = Castle.getInstance();
		int x = e.xsize();
		int z = e.zsize();
		int spacing;
		ArrayList<Integer> spacings=new ArrayList<Integer>();
		for(spacing=2;spacing<x && spacing<z;spacing++){
			if((x-1)%spacing==0 && ((z-1)%spacing==0))
				spacings.add(spacing);
		}
		if(spacings.size()==0)
			return; // couldn't do it.
		spacing = spacings.get(c.r.nextInt(spacings.size()));
		
		// now do the fills.
		
		boolean isLit = true;
		
		int tp = c.r.nextInt(); // column type
		
		for(x=e.minx;x<=e.maxx;x+=spacing){
			placeColumn(mgr,r,c,x,e.miny,e.minz,tp);
			placeColumn(mgr,r,c,x,e.miny,e.maxz,tp);
		}
		for(z=e.minz;z<=e.maxz;z+=spacing){
			placeColumn(mgr,r,c,e.minx,e.miny,z,tp);
			placeColumn(mgr,r,c,e.maxx,e.miny,z,tp);
		}
		
	}

	/**
	 * General turtleriffic post placement system, for roof gardens primarily
	 * @param mgr
	 * @param room
	 * @param c
	 * @param x
	 * @param y
	 * @param z
	 * @param tp
	 */
	public static void placePost(MaterialManager mgr, Room room, Castle c, int x, int y, int z,
			int tp) {
		Turtle t = new Turtle(mgr, c.getWorld(), new IntVector(x, y + 1, z),
				Direction.NORTH);
		t.setRoom(room);
		
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

	/**
	 * Column placement 
	 * @param mgr
	 * @param room
	 * @param c
	 * @param x
	 * @param y
	 * @param z
	 * @param tp
	 */
	public static void placeColumn(MaterialManager mgr, Room room, Castle c, int x, int y, int z,
			int tp) {
		Turtle t = new Turtle(mgr, c.getWorld(), new IntVector(x, y, z),
				Direction.NORTH);
		t.setRoom(room);
		t.clrModeFlag(Turtle.CHECKWRITE); // containing room check will still work
		
		switch (tp & 7) {
		case 0:
			t.run("m1:wu");
			break;
		case 1:
			t.run("m2:wu");
			break;
		case 2:
			t.run(":mowu.m1wu");
			break;
		case 3:
			t.run("mp:wu");
			break;
		case 4:
			t.run(":m1wu.m2wu");
			break;
		case 5:
			t.run(":mowu.m2wu");
			break;
		case 6:
			t.run("m1wu:mowu.m1wu.m1wu");
			break;
		case 7:
			t.run("m1wu.m2wu.m1wu.m2w.Mt.fwbbwfLwRRwL:m1wu.m2wu");
			break;
		}
	}

}
