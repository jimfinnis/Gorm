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
		
		
		if (castle.getBuildingCount() == 0) {
			Extent e = new Extent(v, 20, 0, 30).setHeight(10);
			MaterialManager mgr = new MaterialManager(e.getCentre().getBlock().getBiome());
			Building r = new Garden(e);
			r.build(mgr);
			castle.addBuilding(r);
		} else {
			// first, create the required room, with its extents
			// set around some other room, and slide it around until it fits
			Building r = createAndFitBuilding();
			if (r != null) {
				MaterialManager mgr = new MaterialManager(r.getExtent().getCentre().getBlock().getBiome());
				r.build(mgr);
				castle.addBuilding(r);
				r.makeRandomExit(mgr);

				GormPlugin.log("room created and added!");
			} else {
				GormPlugin.log("could not create room!");
			}
			makeRandomExit();
			makeRandomExit();
		}
	}

	/**
	 * Attempt to construct a random link between two rooms
	 */
	private void makeRandomExit() {
		Building r = castle.getRandomBuilding();
		MaterialManager mgr = new MaterialManager(r.getExtent().getCentre().getBlock().getBiome());		
		if (r != null) {
			r.makeRandomExit(mgr);
		}
	}

	/**
	 * Create a room of the type we most need, in the same location as another
	 * room. Then slide it about until it fits.
	 * 
	 * @return room
	 */
	private Building createAndFitBuilding() {

		// we try to find a suitable room, picking random rooms from the
		// beginning of the list.
		Random rnd = new Random();
		for (int tries = 0; tries < 10; tries++) {
			double qqq = 1.0 / (double) castle.getBuildingCount();
			for (Building r : castle.getBuildings()) {
				if (rnd.nextDouble() < qqq) {
					Building newRoom = createBuilding(r);
					if (moveRoomUntilFit(newRoom, 3 + tries, r)) { // more
																	// laxity in
																	// y-slide
																	// each time
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
	private Building createBuilding(Building r) {
		switch (rnd.nextInt(25)) {
		case 0:
			return new Garden(r);
		default:
			return new Hall(r);

		}

	}

	private boolean moveRoomUntilFit(Building r, int maxyslide,
			Building parentRoom) {
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
			Extent moved = e.addvec(slide[0], slide[1], slide[2]);
			for (int i = 0; i < 100; i++) {
				// GormPlugin.log("sliding...");

				// also try a limited yslide
				for (int yslide = 0; yslide <= maxyslide; yslide++) {
					if (tryMove(r, moved, -yslide, parentRoom))
						return true;
					if (tryMove(r, moved, yslide, parentRoom))
						return true;
				}
				moved = moved.addvec(slide[0], slide[1], slide[2]);
			}
		}
		return false;

	}

	/**
	 * Test out moving a room to a given position, with a bit of y added too.
	 * 
	 * @param r
	 * @param moved
	 * @param yslide
	 * @return
	 */
	private boolean tryMove(Building r, Extent moved, int yslide,
			Building parentRoom) {
		Extent e = moved.addvec(0, yslide, 0);
		Extent eOuter = e.expand(1, Extent.ALL);
		if (!castle.intersects(e) && !e.intersectsWorld()) {
			if (e.getMinHeightAboveWorld() < 3 && parentRoom.intersects(eOuter)) {
				// that'll do!
				r.setExtent(eOuter);
				GormPlugin.log("room moved to: " + r.getExtent().toString());
				return true;
			}
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
