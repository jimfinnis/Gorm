package org.pale.gorm.config;

import org.bukkit.configuration.ConfigurationSection;
import org.pale.gorm.Castle;
import org.pale.gorm.GormPlugin;
import org.pale.gorm.RandomCollection;

public class ConfigUtils {
	public static class MissingAttributeException extends Exception{
		public String name;
		public ConfigurationSection cf;
		public MissingAttributeException(String s, ConfigurationSection cf){
			super("Missing attribute: "+cf.getName()+"."+s);
			name = cf.getName()+"."+s;
		}
	}
	
	/**
	 * Read a min and max value. It's either a single value or a 2-tuple. Similar
	 * to getRandomValueInRange, but that actually does the randomisation too.
	 * @throws MissingAttributeException 
	 */
	public static double[] getDoubleRange(ConfigurationSection c,String string) throws MissingAttributeException{
		double r[] = new double[2];
		if(c.isList(string)){
			r[0] = c.getDoubleList(string).get(0);
			r[1] = c.getDoubleList(string).get(1);
		} else if(c.isDouble(string) || c.isInt(string)) {
			r[0] = c.getDouble(string);
			r[1] = c.getDouble(string);
		} else {
			throw new MissingAttributeException(string,c);
		}
		return r;
	}
		

	/**
	 * Read either a double, in which case it is returned; or a list [a,b],
	 * in which case a value between a and b is returned.
	 * @param c
	 * @param string
	 * @return
	 * @throws MissingAttributeException 
	 */
	public static double getRandomValueInRange(ConfigurationSection c,
			String string) throws MissingAttributeException {

		if(c.isList(string)){
			double a,b;
			a = c.getDoubleList(string).get(0);
			b = c.getDoubleList(string).get(1);
			double r = Castle.getInstance().r.nextDouble()*(b-a)+a;
			return r;
		} else if(c.isDouble(string) || c.isInt(string)) {
			return c.getDouble(string);
		} else {
			throw new MissingAttributeException(string,c);
		}
	}
	

	/**
	 * Read either an int, in which case it is returned; or a list [a,b],
	 * in which case a value between a and b is returned.
	 * @param c
	 * @param string
	 * @return
	 * @throws MissingAttributeException 
	 */
	public static int getRandomValueInRangeInt(ConfigurationSection c,
			String string) throws MissingAttributeException {
		
		if(c.isList(string)){
			int a,b;
			a = c.getIntegerList(string).get(0);
			b = c.getIntegerList(string).get(1);
			return Castle.getInstance().r.nextInt(b-a)+a;
		} else if(!c.isInt(string)) {
			throw new MissingAttributeException(string,c);
		} else {
			return c.getInt(string);
		}
	}
	
	public static String getWeightedRandom(ConfigurationSection c,String name) throws MissingAttributeException{
		if(!c.isConfigurationSection(name))
			throw new MissingAttributeException(name, c);
		ConfigurationSection cf = c.getConfigurationSection(name);
		RandomCollection<String> r = new RandomCollection<String>(Castle.getInstance().r);
		for(String s: cf.getKeys(false)){
			r.add(cf.getDouble(s), s);
		}
		return r.next();
		
	}
}
