package org.pale.gorm.roomutils;

import org.pale.gorm.Castle;

/**
 * This class contains definitions of furniture items as turtle strings with test zones
 * to ensure clearance. It also contains some arrays of these strings.
 * 
 * The string defines the furniture as seen from the centre of its back,
 * facing towards the back.
 * 
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
	
	private static final String bed = "LTRRTLbLTRRTLf.MD0wbMD1w";
	
	private static final String column = "bTfLTRRTL.m1:uTw";
	private static final String shelfCol= "bTfLTRRTL.MB:uTw";
	
	private static final String loot = "Ccw";
	private static final String chest= "MCw";
	private static final String spawner = "Csw";
	private static final String brewingStand = "MSbw";
	private static final String flowerPot = "Mww.uCpw";
	private static final String craftTable = "MStw";
	private static final String cauldron = "MScw";
	public static final String furnace = "MSfw";
	private static final String anvil = "MSaw";
	
	public static final String[] defaultChoices = {
		chair1,sofa1,shelves1,shelves2,chest,flowerPot,
		chair1,sofa1,shelves1,shelves2,chest,flowerPot,
		chair1,sofa1,shelves1,shelves2,chest,flowerPot,
		chair1,sofa1,shelves1,shelves2,chest,loot
	};
	
	public static final String[] smithChoices = {
		chest,chest,craftTable,chair1,chair1,loot,
		furnace,furnace,craftTable,anvil,cauldron,
		furnace,furnace,craftTable,anvil
	};
	
	public static final String[] shopChoices = {
		chest,chest,craftTable,chair1,chair1,loot
	};
	
	public static final String[] scaryChoices = {
		loot,column
	};
	
	public static final String [] uniqueScaryChoices = {
		spawner
	};
	
	public static final String[] poshChoices = {
		loot,chest,chest,chest,shelves1,shelves2,sofa1,brewingStand,
		flowerPot,flowerPot,sofa1,sofa1
	};
	
	public static final String[] dwellingChoices = {
		loot,bed,sofa1,chair1,chair1,shelves1,flowerPot,bed,
		chest,bed,sofa1,chair1,chair1,shelves1,flowerPot,bed,
		chest,bed,sofa1,chair1,chair1,shelves1,flowerPot,bed,
		
	};
	
	public static final String[] libraryChoices = {
		shelves1,shelves1,shelves2,chair1,shelfCol
	};

	/** helper for getting random string */
	public static String random(String[] a){
		return a[Castle.getInstance().r.nextInt(a.length)];
	}
}
