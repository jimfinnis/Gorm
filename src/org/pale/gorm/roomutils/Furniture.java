package org.pale.gorm.roomutils;

import java.util.ArrayList;
import java.util.Collections;

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
	public static void cornerShelves(Extent e, MaterialManager mgr, int carpet) {
		GormPlugin.log("Beginning Shelves");
		Extent corner1 = new Extent(e.minx, e.miny, e.maxz - 1);
		corner1.maxx++;
		corner1.maxy = e.maxy;
		corner1.maxz = e.maxz;
		Castle c = Castle.getInstance();
		c.fill(corner1, Material.BOOKSHELF, 0);
		Extent cookieCutter = new Extent(corner1.maxx, corner1.miny,
				corner1.minz);
		cookieCutter.maxy = corner1.maxy;
		c.fill(cookieCutter, Material.AIR, 0);
		cookieCutter.maxy = cookieCutter.miny;
		if (carpet >= 0)
			c.fill(cookieCutter, Material.CARPET, carpet);
		GormPlugin.log("Shelves at " + corner1.maxx + " " + corner1.maxy + " "
				+ corner1.maxz);
		Extent corner2 = new Extent(e.maxx - 1, e.miny, e.minz);
		corner2.maxx = e.maxx;
		corner2.maxy = e.maxy;
		corner2.maxz++;
		c.fill(corner2, Material.BOOKSHELF, 0);
		Extent cookieCutter2 = new Extent(corner2.minx, corner2.miny,
				corner2.maxz);
		cookieCutter2.maxy = corner2.maxy;
		c.fill(cookieCutter2, Material.AIR, 0);
		cookieCutter2.maxy = cookieCutter2.miny;
		if (carpet >= 0)
			c.fill(cookieCutter2, Material.CARPET, carpet);
		GormPlugin.log("Shelves at " + corner2.maxx + " " + corner2.maxy + " "
				+ corner2.maxz);
	}

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
	public static void placeFurniture(MaterialManager mgr, Room r, String s) {
		Castle c = Castle.getInstance();
		// we keep a list of candidates
		ArrayList<PlacementCandidate> candidates = new ArrayList<PlacementCandidate>();
		
		int maxScore = 0; // candidate max score

		// now we iterate through each wall of the inner space
		Extent inner = r.getExtent().expand(-1, Extent.ALL);
		for (Direction wallDir : Direction.values()) {
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

}
