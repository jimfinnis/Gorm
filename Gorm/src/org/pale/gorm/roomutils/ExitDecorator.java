package org.pale.gorm.roomutils;

import java.util.Random;

import org.bukkit.Material;
import org.pale.gorm.Castle;
import org.pale.gorm.Direction;
import org.pale.gorm.Exit;
import org.pale.gorm.GormPlugin;
import org.pale.gorm.IntVector;
import org.pale.gorm.Exit.ExitType;
import org.pale.gorm.Extent;
import org.pale.gorm.MaterialDataPair;
import org.pale.gorm.MaterialManager;

/**
 * Static class which decorates exits in various ways.
 */
public class ExitDecorator {
	public static void decorate(MaterialManager mgr, Exit e) {
		Castle c = Castle.getInstance();

		if (e.getType() == ExitType.OUTSIDE) // don't decorate outside exits
		{

			GormPlugin.log("decorate exit does nothing");
			IntVector v = e.getDirection().vec;
			v = v.add(0, 2, 0).add(e.getExtent().getCentre());
			c.fill(new Extent(v, 0, 0, 0), Material.LAPIS_BLOCK, 0);
			return;
		}
		// first decorate the sides and top the same way
		decorateSidesAndTop(e, mgr);

		// then, if we're an outward or inward exit, maybe add a little porch
		// thing
		// and add a door if we're outside, or maybe just anyway

		if (e.getType() == ExitType.INWARDS || e.getType() == ExitType.OUTWARDS) {
			addPorch(e, mgr);
			addDoor(e, mgr);
		} else if (c.r.nextFloat() < 0.5)
			addDoor(e, mgr);

		// add a light, perhaps
		IntVector lintel = e.getExtent().getCentre();
		lintel.y = e.getExtent().maxy + 1;
		IntVector lightPos = lintel.add(e.getDirection().vec);
		if (c.r.nextFloat() < 0.2
				|| Castle.requiresLight(lightPos.x, lightPos.y, lightPos.z)) {
			c.checkFill(new Extent(lightPos, 0, 0, 0), Material.TORCH, 0);
		}
		lightPos = lintel.add(e.getDirection().vec.negate());
		if (c.r.nextFloat() < 0.2
				|| Castle.requiresLight(lightPos.x, lightPos.y, lightPos.z)) {
			c.checkFill(new Extent(lightPos, 0, 0, 0), Material.TORCH, 0);
		}

	}

	private static void addDoor(Exit e, MaterialManager mgr) {
		// TODO Auto-generated method stub

	}

	private static void addPorch(Exit e, MaterialManager mgr) {
		// TODO Auto-generated method stub

	}

	private static void decorateSidesAndTop(Exit e, MaterialManager mgr) {
		Castle c = Castle.getInstance();
		Extent x = e.getExtent();

		if (c.r.nextFloat() < 2) {
			if (e.getDirection() == Direction.NORTH
					|| e.getDirection() == Direction.SOUTH)
				x = x.expand(1, Extent.Y | Extent.X);
			else
				x = x.expand(1, Extent.Y | Extent.Z);
			x.miny = e.getExtent().miny;
			//c.fill(x, mgr.getOrnament());
			c.fill(x, Material.SMOOTH_BRICK,3);
			c.fill(e.getExtent(), Material.AIR, 0);
		}
	}
}
