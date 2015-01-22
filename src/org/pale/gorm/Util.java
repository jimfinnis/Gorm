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
	
	/**
	 * Given an array (or varargs) of object,weight,object,weight,
	 * pick an item.
	 * @param objsAndWeights object,weight,object,weight etc.
	 * @return
	 */

	@SuppressWarnings("unchecked")
	public
	static <T> T choose(Object... objsAndWeights){
		double totalf=0;
		for(int i=0;i<objsAndWeights.length;i+=2){
			Object oo = objsAndWeights[i];
			Object wo = objsAndWeights[i+1];
			if(!(oo instanceof Object))
				throw new RuntimeException("Bad type in building list (item)");
			if(!(wo instanceof Integer))
				throw new RuntimeException("Bad type in building list (expected int)");
			int f = (Integer)wo;
			totalf += f;
		}
		
		double r = Castle.getInstance().r.nextDouble()*totalf;
		
		double countf=0;
		for(int i=0;i<objsAndWeights.length;i+=2){
			int f = (Integer)(objsAndWeights[i+1]);
			countf += (double)(f);
			if(countf>=r){
				return (T)(objsAndWeights[i]);
			}
		}
		throw new RuntimeException("out of options in choose");
	}


}
