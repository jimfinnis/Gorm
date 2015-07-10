package org.pale.gorm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Handy class for reading materials and material lists from a config
 * @author white
 *
 */
public class ConfigUtils {
	public static HashMap<String,String> matPairAliases;
	// arrays and generics don't mix. Bloody java.
	private static List<RandomCollection<MaterialDataPair>> loot = 
			new ArrayList<RandomCollection<MaterialDataPair>>();

	public static void load(){
		ConfigAccessor a = new ConfigAccessor("biomes.yml");
		loadAliases(a.getConfig());
		MaterialManager.loadMats(a.getConfig());
		loadLoots(new ConfigAccessor("loot.yml").getConfig());
	}

	public static MaterialDataPair getLoot(int grade){
		return loot.get(grade-1).next();
	}

	private static void loadLoots(FileConfiguration f) {
		for(int i=1;i<=4;i++){
			RandomCollection<MaterialDataPair> l = new RandomCollection<MaterialDataPair>();
			loot.add(l);
			// deal with included lists
			ConfigurationSection inc = f.getConfigurationSection("loot."+i+".include");
			if(null!=inc){
				for(String k: inc.getKeys(false)){
					double chanceMult = inc.getDouble(k);
					loadWeightedMDPList(f,k,chanceMult,l);
				}
			}
			// and load the list itself
			loadWeightedMDPList(f,"loot."+i+".list", 1.0, l);
		}
	}

	/// load a hash of weights keyed by MaterialDataPair, and append to a RandomCollection
	/// of MDPs so we can generate one randomly.
	private static void loadWeightedMDPList(FileConfiguration f,String k,double chanceMult, RandomCollection<MaterialDataPair> toAppendTo) {
		ConfigurationSection c = f.getConfigurationSection(k);
		if(null!=c){
			for(String item: c.getKeys(false)){
				GormPlugin.getInstance().getLogger().info("-- "+item);
				double chance = c.getDouble(item)*chanceMult;
				toAppendTo.add(chance, makeMatDataPair(item));
			}
		}
	}
	private static void loadAliases(FileConfiguration f) {
		ConfigurationSection c = f.getConfigurationSection("aliases.matpairs");
		matPairAliases = new HashMap<String,String>();
		if(c!=null){
			for(String k: c.getKeys(false)){
				matPairAliases.put(k, (String)c.get(k));
			}
		}
	}



	private static MaterialDataPair makeMatDataPair(String name){
		Logger log = GormPlugin.getInstance().getLogger();
		if(!name.contains("/")){
			if(matPairAliases.containsKey(name))
				name = matPairAliases.get(name);
			if(!name.contains("/")){
				Material m = Material.getMaterial(name.toUpperCase());
				if(m==null){
					GormPlugin.getInstance().getLogger().log(Level.SEVERE, "Cannot find material "+name);
					m=Material.DIAMOND_BLOCK;
				}
				return new MaterialDataPair(m,0);
			}
		}	
		String parts[] = name.split("/");
		// the first part of a mat/data name can also be an alias; if it aliases
		// to a mat/data name, we drop the data part.
		if(matPairAliases.containsKey(parts[0])){
			parts[0]=matPairAliases.get(parts[0]);
			if(parts[0].contains("/"))
				parts[0]=parts[0].split("/")[0];
		}
		Material m = Material.getMaterial(parts[0].toUpperCase());
		if(m==null){
			GormPlugin.getInstance().getLogger().log(Level.SEVERE, "Cannot find material "+parts[0]);
			return new MaterialDataPair(Material.DIAMOND_BLOCK,0);
		}
		return new MaterialDataPair(m,Integer.parseInt(parts[1]));
	}

	public static MaterialDataPair getMaterialDataPair(FileConfiguration f,String key){
		String s = f.getString(key);
		return makeMatDataPair(s);
	}

	public static RandomCollection<MaterialDataPair> getMaterialDataPairs(FileConfiguration f,String key){
		Logger log = GormPlugin.getInstance().getLogger();
		RandomCollection<MaterialDataPair> collection = new RandomCollection<MaterialDataPair>();
		ConfigurationSection c = f.getConfigurationSection(key);
		if(c==null){
			log.info("Unable to load material list, must inherit from base: "+key);
			return null;
		}
		loadWeightedMDPList(f, key, 1, collection);
		log.info("Strings for "+key+": "+collection.size());
		return collection;
	}
}
