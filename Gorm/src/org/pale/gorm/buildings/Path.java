package org.pale.gorm.buildings;

import java.util.Random;

import org.pale.gorm.buildings.Garden;
import org.pale.gorm.Building;
import org.pale.gorm.Castle;

public class Path extends Garden {

	public Path(Building parent) {
		super(parent);
		Random rnd = Castle.getInstance().r;

		int x = rnd.nextInt(30) + 10;
		int z = rnd.nextInt(2) + 3;

		if (rnd.nextFloat() < 0.5) {
			int t;
			t = x;
			x = z;
			z = t;
		}
		setInitialExtent(parent, x, 40, z);
	}
}
