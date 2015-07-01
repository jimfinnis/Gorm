package org.pale.gorm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Handy class for reading materials and material lists from a config
 * @author white
 *
 */
public class ConfigUtils {
	public static HashMap<String,String> matPairAliases;

	public static void load(){
		loadAliases();
		MaterialManager.loadMats();
	}
	private static void loadAliases() {
		ConfigurationSection c = GormPlugin.getInstance().getConfig().getConfigurationSection("aliases.matpairs");
		matPairAliases = new HashMap<String,String>();
		for(String k: c.getKeys(false)){
			matPairAliases.put(k, (String)c.get(k));
		}
	}

	public static Material[] getMaterialList(String key){
		Logger log = GormPlugin.getInstance().getLogger();
		List<Material> l = new ArrayList<Material>();
		for(String s: GormPlugin.getInstance().getConfig().getStringList(key)){
			Material m = Material.getMaterial(s.toUpperCase());
			if(m==null){
				log.log(Level.SEVERE, "Cannot find material in "+key+": "+s);
				m = Material.DIAMOND_BLOCK;
			}
			l.add(m);
		}
		return l.toArray(new Material[l.size()]);
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

	public static MaterialDataPair getMaterialDataPair(String key){
		String s = GormPlugin.getInstance().getConfig().getString(key);
		return makeMatDataPair(s);
	}

	public static MaterialDataPair[] getMaterialDataPairs(String key){
		Logger log = GormPlugin.getInstance().getLogger();
		List<MaterialDataPair> l = new ArrayList<MaterialDataPair>();
		List<String> lst = GormPlugin.getInstance().getConfig().getStringList(key);
		if(lst==null){
			log.info("Unable to load key, must inherit from base: "+key);
			return null;
		}
		log.info("Strings for "+key+": "+lst.size());
		for(String s: GormPlugin.getInstance().getConfig().getStringList(key)){
			MaterialDataPair mp = makeMatDataPair(s);
			l.add(mp);
		}
		return  l.toArray(new MaterialDataPair[l.size()]);
	}
}
