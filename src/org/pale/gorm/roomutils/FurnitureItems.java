package org.pale.gorm.roomutils;

import org.pale.gorm.Castle;

/**
 * This class contains definitions of furniture items as turtle strings with test zones
 * to ensure clearance. It also contains some arrays of these strings.
 * @author white
 *
 */
public class FurnitureItems {
	/** a chair using signs as arms */
	private static final String chair1 = "Mjw.uBLBRRBLd.Mq.rbw.rrbbw.rb.tLTBLtbtRtRt";
	/** a sofa using signs as arms */
	private static final String sofa1 = "MjwRw.uBLBRRBRBLLd.Mq.rbbw.rrbbbw.rb.TBLTBLTBLTbtRtRtRt";
	/** some bookshelves 2x2*/
	private static final String shelves1 = "MB.wuwdR.wuwd.bTBLTBbtRt";
	/** some bookshelves 2x3*/
	private static final String shelves2 = "MB.wuwuwddR.wuwuwdd.bTBLTBbtRt";
	
	private static final String chest = "Ccw";
	
	public static final String[] defaultChoices = {
		chair1,sofa1,shelves1,shelves2,
		chest
	};
	
	/** helper for getting random string */
	public static String random(String[] a){
		return a[Castle.getInstance().r.nextInt(a.length)];
	}
}
