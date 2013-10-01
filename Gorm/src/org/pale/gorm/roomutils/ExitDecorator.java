package org.pale.gorm.roomutils;

import java.util.Random;

import org.bukkit.Material;
import org.pale.gorm.Castle;
import org.pale.gorm.Exit;
import org.pale.gorm.GormPlugin;
import org.pale.gorm.IntVector;
import org.pale.gorm.Exit.ExitType;
import org.pale.gorm.Extent;
import org.pale.gorm.MaterialDataPair;

/**
 * Static class which decorates exits in various ways.
 */
public class ExitDecorator {
	public static void decorate(Exit e) {
		Castle c = Castle.getInstance();

		if (e.getType() == ExitType.OUTSIDE) // don't decorate outside exits
			return;

		GormPlugin.log("decorate exit does nothing");
		IntVector v = e.getDirection().vec;
		v = v.add(0, 2, 0).add(e.getExtent().getCentre());
		c.fill(new Extent(v, 0, 0, 0), Material.LAPIS_BLOCK, 0);

		// first decorate the sides and top the same way
		decorateSidesAndTop(e);

		// then, if we're an outward or inward exit, maybe add a little porch
		// thing
		if (e.getType() == ExitType.INWARDS || e.getType() == ExitType.OUTWARDS) {
			addPorch(e);
			addDoor(e);
		} else if (c.r.nextFloat() < 0.5)
			addDoor(e);

		// and add a door if we're outside, or maybe just anyway

	}

	private static void addDoor(Exit e) {
		// TODO Auto-generated method stub

	}

	private static void addPorch(Exit e) {
		// TODO Auto-generated method stub

	}

	private static void decorateSidesAndTop(Exit e) {
		MaterialDataPair m;

	}
}
