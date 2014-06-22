package org.pale.gorm.buildings;

import java.util.Random;

import org.pale.gorm.Building;
import org.pale.gorm.Castle;
import org.pale.gorm.Extent;
import org.pale.gorm.IntVector;
import org.pale.gorm.MaterialManager;
import org.pale.gorm.Noise;
import org.pale.gorm.Room;
import org.pale.gorm.rooms.EmptyRoom;
import org.pale.gorm.rooms.RoofGarden;
import org.pale.gorm.roomutils.BoxBuilder;

public class Corridor extends Building {

	public Corridor(Building parent) {
		Random rnd = Castle.getInstance().r;

		// get the new building's extent (i.e. location and size)

		int x = rnd.nextInt(25) + 20; // long and thin, either way round
		int z = x / 4;
		if (z > 6)
			z = 6;

		if (rnd.nextBoolean()) {
			int q = x;
			x = z;
			z = q;
		}
		int y;

		IntVector loc = parent.extent.getCentre();

		double height = Noise.noise2Dfractal(loc.x, loc.z, 1, 2, 3, 0.5); // 0-1
																			// noise
		double variance = Noise.noise2Dfractal(loc.x, loc.z, 2, 2, 3, 0.5);

		y = rnd.nextInt(5 + Noise.scale(10, variance)) + 10;
		y = Noise.scale(y, height) + 5;

		// this would make the corridor the same height as its parent. Looks
		// silly, often.

		// if(isWall)
		// y=parent.extent.ysize();

		setInitialExtent(parent, x, y, z);
	}

	@Override
	public Building createChildBuilding(Random r) {
		return BuildingFactory.createRandom(this, 
				new BuildingType[] {
				BuildingType.CORRIDOR,
				BuildingType.CORRIDOR,
				BuildingType.CORRIDOR,
				BuildingType.LIBRARY,
				BuildingType.HALL,
				BuildingType.HALL,
				BuildingType.DWELLING,
				BuildingType.DWELLING,
				BuildingType.SHOP,
				BuildingType.SHOP,
				});
	}

	@Override
	protected Room createRoom(MaterialManager mgr, Extent roomExt, Building bld) {
		return new EmptyRoom(mgr, roomExt, bld, false);
	}

	@Override
	public void build(MaterialManager mgr) {
		BoxBuilder.build(mgr, extent); // make the walls
		makeRooms(mgr); // and make the internal rooms
		underfill(mgr, false); // build "stilts" if required
		generateRoof(mgr);
	}

}
