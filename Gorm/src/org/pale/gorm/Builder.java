package org.pale.gorm;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.material.Sign;
import org.pale.gorm.buildings.Garden;
import org.pale.gorm.buildings.Hall;

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
		IntVector v = new IntVector(loc).subtract(0,1,0);
		
		if (castle.getBuildingCount() == 0) {
			Extent e = new Extent(v, 20, 0, 30).setHeight(10);
			MaterialManager mgr = new MaterialManager(e.getCentre().getBlock().getBiome());
			Building b = new Garden(e);
			b.build(mgr);
			castle.addBuilding(b);
			// very important - tell the castle to re-sort the room list!
			castle.sortRooms();
			b.update();
		} else {
			// first, create the required room, with its extents
			// set around some other room, and slide it around until it fits
			Building b = createAndFitBuilding();
			if (b != null) {
				MaterialManager mgr = new MaterialManager(b.getExtent().getCentre().getBlock().getBiome());
				b.build(mgr);
				
				castle.addBuilding(b);
				// very important - tell the castle to re-sort the room list!
				castle.sortRooms();
				b.makeRandomExit();

				GormPlugin.log("Building "+Integer.toString(b.id)+" created and added!");
			} else {
				GormPlugin.log("Could not move building to a good place.");
			}
			makeRandomExit();
			makeRandomExit();
			if(b!=null)
				b.update();
		}
	}

	/**
	 * Attempt to construct a random link between two rooms
	 */
	private boolean makeRandomExit() {
		for(Room r: castle.getRooms()){
			if(r.attemptMakeExit()){
				r.update();
				return true;
			}
		}
		return false;
	}

	/**
	 * Create a building of the type we most need, in the same location as another
	 * room. Then slide it about until it fits.
	 * 
	 * @return room
	 */
	private Building createAndFitBuilding() {

		// we try to find a suitable building, picking random buidingsfrom the
		// beginning of the list.
		Random rnd = new Random();
		for (int tries = 0; tries < 10; tries++) {
			double qqq = 1.0 / (double) castle.getBuildingCount();
			for (Building b : castle.getBuildings()) {
				if (rnd.nextDouble() < qqq) {
					Building newBuilding = createBuilding(b);
					GormPlugin.log("New building "+Integer.toString(newBuilding.id)+" created: attempting move");
					if (moveRoomUntilFit(newBuilding, 3 + tries, b)) { // more
																	// laxity in
																	// y-slide
																	// each time
						return newBuilding;
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
	 * @param b
	 * @param moved
	 * @param yslide
	 * @return
	 */
	private boolean tryMove(Building b, Extent moved, int yslide,
			Building parentRoom) {
		Extent e = moved.addvec(0, yslide, 0);
		Extent eOuter = e.expand(1, Extent.ALL);
		if (!castle.intersects(e) && !e.intersectsWorld()) {
			if (e.getMinHeightAboveWorld() < 3 && parentRoom.intersects(eOuter)) {
				// that'll do!
				b.setExtent(eOuter);
				GormPlugin.log("building "+Integer.toString(b.id)+" moved to: " + b.getExtent().toString());
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
