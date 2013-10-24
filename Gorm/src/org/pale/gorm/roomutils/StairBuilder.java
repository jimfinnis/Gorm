package org.pale.gorm.roomutils;

import java.util.ArrayList;
import java.util.Collection;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.material.Stairs;
import org.pale.gorm.Castle;
import org.pale.gorm.Direction;
import org.pale.gorm.Extent;
import org.pale.gorm.GormPlugin;
import org.pale.gorm.IntVector;
import org.pale.gorm.MaterialManager;
import org.pale.gorm.Room;

/**
 * Class for building stairs.
 * @author white
 *
 */
public class StairBuilder {
	boolean stairsBlocked;
	
	
	public boolean isStairsBlocked() {
		return stairsBlocked;
	}

	/**
	 * Attempt to run out from an extent representing a hole in a wall, dropping
	 * stairs until we hit the floor. We start one step out from the floor block
	 * of the exit.
	 * 
	 * @param mgr material manager
	 * @param e extent of exit where the stairs come from
	 * @param d direction of stairs
	 * @param room the room we're creating the stairs within, or null if there isn't one. Used for blocking checks.
	 * @return null if no stairs, else extent of stairs
	 */
	public Extent dropExitStairs(MaterialManager mgr, Extent e, Direction d, Room room) {
		
		stairsBlocked = false;

		IntVector v = d.vec;
		int floory = e.miny; // get the floor of the exit

		e = e.growDirection(d, 5); // stretch out the extent for scanning
									// purposes

		e.miny -= 100; // so that getHeightWithin() will read heights lower than
						// the current floor

		Extent stairExtent = new Extent(); // the extent of the stairs gets put in this
		
		boolean done = false;
		// we need to iterate over the 'stripes' within a multi-block-wide exit
		// and we add the direction to the start position so we don't start inside the wall
		// (the exit hasn't been "blown" yet)
		if (v.x == 0) {
			// the north-south case
			for (int x = e.minx; x <= e.maxx; x++) {
				int startz = v.z > 0 ? e.minz : e.maxz;
				IntVector start = new IntVector(x, floory, startz);
				done |= stairScan(mgr, start, e, v, stairExtent, room);
			}
		} else {
			// the north-south case
			for (int z = e.minz; z <= e.maxz; z++) {
				int startx = v.x > 0 ? e.minx : e.maxx;
				IntVector start = new IntVector(startx, floory, z);
				done |= stairScan(mgr, start, e, v, stairExtent,room);
			}
		}

		return done ? stairExtent : null;
	}

	/**
	 * Starting at a point, scan along a given vector within the given extent,
	 * filling in drops with stairs as necessary. Return true if we did
	 * anything.
	 * 
	 * @param mgr material manager
	 * @param start the starting point
	 * @param e the extent of the containing area
	 * @param v the direction of the stairs
	 * @param stairExtent output area containing the generated stairs
	 * @param room the room we're creating the stairs within, or null if there isn't one. Used for blocking checks.
	 * @return
	 */
	private boolean stairScan(MaterialManager mgr, IntVector start, Extent e,
			IntVector v, Extent stairExtent, Room room) {
		Castle castle = Castle.getInstance();
		World world = Castle.getInstance().getWorld();

		boolean done = false;
		
		// move out one, because we don't blow the hole yet, and down one because that's where steps go
		
		start = start.add(v).add(0,-1,0);
		// preliminary check
		Block b = castle.getBlockAt(start);
		if(b.getType().isSolid()){
			return true; // we land early with no need for stairs
		}
		
		BlockFace bf = new IntVector(-v.x, 0, -v.z).toBlockFace();
		Collection<BlockState> undoBuffer = new ArrayList<BlockState>();

		int y = start.y;
		Material mat = mgr.getStair();
		
		GormPlugin.log("Start="+start.toString());
		
/*		
		// steps starting from blind walls can happen.. but are stupid.
		IntVector startWall = start.subtract(v).add(0, 1, 0);
		if (world.getBlockAt(startWall.x, startWall.y, startWall.z).getType()
				.isSolid())
			return false;
*/
		for (IntVector pt = new IntVector(start); e
				.contains(pt.x, e.miny, pt.z); pt = pt.add(v)) {
			int newy = e.getHeightWithin(pt.x, pt.z);
			if (Castle.isStairs(world.getBlockAt(pt.x, newy, pt.z).getType()))
				break;
			if (newy > y) {
				// if we've gone UP, exit the loop. We only work down.
				//stairsBlocked = true;
				break;
			} else if (newy < y) {
				// we've gone down, so build up, saving the old block state
				undoBuffer.add(world.getBlockAt(pt.x, y, pt.z).getState());
				if(room!=null && room.isBlocked(new IntVector(pt.x,y,pt.z))){
					// can't create a step here, we have to abandon the whole idea of stairs
					// here. Let's hope the undo buffer works.
					stairsBlocked = true;
					break;
				}
				castle.setStairs(pt.x, y, pt.z, bf, mat);
				stairExtent.union(pt.x,y,pt.z);
				

				b = world.getBlockAt(pt.x, y - 1, pt.z); // check we
																// landed!
				if (b.getType().isSolid()){
					done = true;
					break;
				}
				y--; // and go down again!
			}
		}

		if (!done) {
			// didn't achieve what we wanted, so restore all blocks
			for (BlockState s : undoBuffer) {
				// for some reason update() isn't working for me.
				world.getBlockAt(s.getLocation()).setType(s.getType());
			}
		}
		return done;
	}


}
