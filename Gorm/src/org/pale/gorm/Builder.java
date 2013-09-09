package org.pale.gorm;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.World;

/**
 * This object actually does the building inside the castle
 * 
 * @author white
 * 
 */
public class Builder {
	private Castle castle;
	
	static Random rnd = new Random();

	Builder(World w) {
		castle = Castle.getInstance();
		castle.setWorld(w);
	}

	/**
	 * Build the next room. loc is only used if there is no existing room
	 * 
	 * @param loc
	 *            initial room location
	 */
	public void build(Location loc) {
		// initial room test
		IntVector v = new IntVector(loc);
		if (castle.getRoomCount() == 0) {
			Extent e = new Extent(v, 20, 0, 30).setHeight(10);
			Room r = new UnroofedSpace(e);
			r.render();
			castle.addRoom(r);
		} else {
			// first, create the required room, with its extents
			// set around some other room, and slide it around until it fits
			Room r = createAndFitRoom();
			if (r != null) {
				castle.addRoom(r);
				r.render();
				r.makeExit(true);

				GormPlugin.log("room created and added!");
			} else {
				GormPlugin.log("could not create room!");
			}
		}
	}

	/**
	 * Create a room of the type we most need, in the same location as another
	 * room. Then slide it about until it fits.
	 * 
	 * @return room
	 */
	private Room createAndFitRoom() {

		// we try to find a suitable room, picking random rooms from the
		// beginning of the list.
		Random rnd = new Random();
		for (int tries = 0; tries < 10; tries++) {
			double qqq = 1.0 / (double) castle.getRoomCount();
			for (Room r : castle.getRooms()) {
				if (rnd.nextDouble() < qqq) {
					Room newRoom = createRoom(r);
					if (moveRoomUntilFit(newRoom,3+tries,r)) { // more laxity in y-slide each time
						return newRoom;
					}
				}
			}
		}
		return null;

	}

	/**
	 * Create the room most required, centred around another room. We'll slide
	 * it about to get it to fit.
	 * 
	 * @return
	 */
	private Room createRoom(Room r) {
		GormPlugin.log("creating room around: " + r.getExtent().toString());
		Extent e = new Extent(r.getExtent().getCentre(), 
				rnd.nextInt(10)+5, 
				1,  // the y setting here is unused
				rnd.nextInt(10)+5);
		e.miny = r.getExtent().miny; // make floors align
		if(rnd.nextFloat()<0.1)
			e=e.setHeight(rnd.nextInt(20)+15);
		else
			e=e.setHeight(rnd.nextInt(3)+10);

		GormPlugin.log("new room has extent: " + e.toString());
		return new Room(Room.RoomType.ROOM, e);

	}

	private boolean moveRoomUntilFit(Room r,int maxyslide,Room parentRoom) {
		Extent start = new Extent(r.getExtent());

		// we have to shrink the room by 1 so that walls are in common - 'start'
		// will be
		// the internal space of the room
		Extent e = start.expand(-1, Extent.ALL);

		// we repeatedly try different "sliding" strategies until it fits. These
		// strategies are sorted randomly, but we always try XZ before Y.

		// should be refactored with IntVector
		int[][] slides = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 0, 1 }, { 0, 0, -1 } };
		shuffle(slides);

		for (int[] slide : slides) {
			Extent moved = e.addvec(slide[0],slide[1],slide[2]);
			for (int i = 0; i < 100; i++) {
				GormPlugin.log("sliding...");

				// also try a limited yslide
				for (int yslide = 0; yslide<=maxyslide; yslide++) {
					// slide up, then down
					if(tryMove(r,moved,yslide,parentRoom))return true;
					if(tryMove(r,moved,-yslide,parentRoom))return true;
				}
				moved = moved.addvec(slide[0],slide[1],slide[2]);
			}
		}
		return false;

	}

	/**
	 * Test out moving a room to a given position, with a bit of y added too.
	 * @param r
	 * @param moved
	 * @param yslide
	 * @return
	 */
	private boolean tryMove(Room r, Extent moved, int yslide,Room parentRoom) {
		Extent e = moved.addvec(0,yslide,0);
		if (!castle.intersects(e) &&  // we're not intersecting with existing castle bits
				!e.intersectsWorld() &&  // and we're not intersecting with dirt/rock etc.
				e.getMaxHeightAboveWorld()<3 && // and we're on the ground (ish)
				parentRoom.intersects(e.expand(1,Extent.ALL)) // and we still intersect with the parent room
				) {
			// that'll do!
			r.setExtent(e.expand(1, Extent.ALL));
			GormPlugin.log("room moved to: "
					+ r.getExtent().toString());
			return true;
		}
		return false;
	}

	static void shuffle(int ar[][]) { // Fisher-Yates shuffle
		for (int i = ar.length - 1; i > 0; i--) {
			int index = rnd.nextInt(i + 1);
			// Simple swap
			int[] a = ar[index];
			ar[index] = ar[i];
			ar[i] = a;
		}
	}

}
