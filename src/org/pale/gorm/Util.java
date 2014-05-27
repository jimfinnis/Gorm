package org.pale.gorm;

import java.util.Random;

public class Util {
	static final double RANDOMLAMBDA = 6;

	/**
	 * exponential random number from 0 to n-1
	 * 
	 * @param n
	 * @return
	 */
	public static int randomExp(Random r, int n) {
		int i;
		do {
			double u = r.nextFloat(); // uniform from 0 to 1
			u = (Math.log10(1 - u) / Math.log10(2)) / -RANDOMLAMBDA;
			i = (int) (u * n);
		} while (i >= n);
		return i;
	}


}
