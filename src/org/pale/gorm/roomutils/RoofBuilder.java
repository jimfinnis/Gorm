package org.pale.gorm.roomutils;

import org.pale.gorm.Castle;
import org.pale.gorm.Extent;
import org.pale.gorm.MaterialManager;

/**
 * An interface for things which build roofs. I've not made this a static class,
 * as I've done with things like WindowMaker and ExitDecorator, because roof
 * builders could get quite complicated.
 * 
 * Also provides a sort of "factory method" which both builds and runs a random
 * subclass.
 * 
 * @author white
 * 
 */
public abstract class RoofBuilder {
	public abstract int buildRoof(MaterialManager mgr, Extent buildingExtent);

	public static int randomRoof(MaterialManager mgr, Extent extent) {
		switch (Castle.getInstance().r.nextInt(4)) {
		case 0:
			return new PitchedRoofBuilder().buildRoof(mgr, extent);
		case 1:
		default:
			return new RoofCrenellationBuilder().buildRoof(mgr, extent);
		}
	}
}
