package org.pale.gorm;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Given a biome, the material manager handles possible materials for walls,
 * columns and fences (and anything else I think of). The idea is that people
 * building in this biome would use local materials!
 *
 * @author white
 *
 */
public class MaterialManager {


	// indices into the material lists
	public enum MatType {
		PRIMARY,SECONDARY,SUPSECONDARY,ORNAMENT,STAIR,FENCE,ROOFSTEPS,GROUND,
		POLE,WINDOW,FLOOR,DOOR,FLOWERS,RAREFLOWERS
	}

	// three level array for each biome!
	// Top level, set of top level data for the biome to pick from randomly
	// when the manager is initialised.
	// Next level, different types as indexed by function with  the constants above
	// Bottom level, list of material/data pairs to pick from for that function.

	private static Map<Biome,List<Map<MatType,RandomCollection<MaterialDataPair>>>> biomeMetaList;

	private Map<MatType,RandomCollection<MaterialDataPair>> baseMats;
	private Map<MatType,MaterialDataPair> mats;

	public MaterialManager(Biome b) {
		Random r = Castle.getInstance().r;

		List<Map<MatType,RandomCollection<MaterialDataPair>>> lst = biomeMetaList.get(b);
		baseMats = lst.get(r.nextInt(lst.size()));
		
		mats = new HashMap<MatType,MaterialDataPair>();
		for(MatType t:MatType.values()){
			mats.put(t, baseMats.get(t).next());
		}
	}

	// this will get a new mdp every time it is called, which isn't usually what you want.
	public MaterialDataPair getRandom(MatType m){
		return baseMats.get(m).next();
	}

	public MaterialDataPair getPrimary() {
		return mats.get(MatType.PRIMARY);
	}

	public MaterialDataPair getSecondary() {
		return mats.get(MatType.SECONDARY);
	}

	public MaterialDataPair getSupSecondary() {
		return mats.get(MatType.SUPSECONDARY);
	}

	public MaterialDataPair getOrnament() {
		return mats.get(MatType.ORNAMENT);
	}

	public MaterialDataPair getDoor(Random r){
		return mats.get(MatType.DOOR);
	}

	/**
	 * Note that this returns a material; the data determines the direction
	 *
	 * @return
	 */
	public MaterialDataPair getRoofSteps() {
		return mats.get(MatType.ROOFSTEPS);
	}

	public MaterialDataPair getPole() {
		return mats.get(MatType.POLE);
	}

	/**
	 * Note that this returns a material; the data determines the direction
	 *
	 * @return
	 */
	public MaterialDataPair getStair() {
		return mats.get(MatType.STAIR);
	}

	public MaterialDataPair getGround() {
		return mats.get(MatType.GROUND);
	}

	public MaterialDataPair getFence() {
		return mats.get(MatType.FENCE);
	}

	public MaterialDataPair getWindow() {
		return mats.get(MatType.WINDOW);
	}
	
	public MaterialDataPair getFloor(){
		return mats.get(MatType.FLOOR);
	}
	
	public MaterialDataPair getFlowers(){
		return mats.get(MatType.FLOWERS);
	}
	
	public MaterialDataPair getRareFlowers(){
		return mats.get(MatType.RAREFLOWERS);
	}
	
	public MaterialDataPair get(MatType t){
		return mats.get(t);
	}
	
	public MaterialDataPair get(String t){
		try {
			MatType mt = MatType.valueOf(t.toUpperCase());
			return mats.get(mt);
		} catch(IllegalArgumentException e){
			throw new RuntimeException("no such material type: "+t);
		}
	}

	private static HashMap<Biome,List<String>> biomeMetaListNames;

	public static void loadMats(FileConfiguration f){
		// first, we load the material metalists for each biome. These are lists
		// of material list names from which we select at random to generate some terrain
		// in that biome. The material lists themselves contain the material data.
		Logger log = GormPlugin.getInstance().getLogger();
		Map<String,Map<MatType, RandomCollection<MaterialDataPair>>> materialLists = 
				new HashMap<String,Map<MatType, RandomCollection<MaterialDataPair>>>();
		biomeMetaListNames = new HashMap<Biome,List<String>>();
		biomeMetaList = new HashMap<Biome,List<Map<MatType,RandomCollection<MaterialDataPair>>>>();
		for(Biome b: Biome.values()){
			loadMatMetaListForBiome(f,b);
		}
		// and the default
		loadMatMetaListForBiome(f,null);
		List<String> defaultMetaListNames = biomeMetaListNames.get(null);
		if(defaultMetaListNames==null)
			log.severe("no default metalist given - could cause problems!");

		// first build a list of patterns and the matlists to which they resolve.
		ConfigurationSection sec = f.getConfigurationSection("matmatch");
		List<Map.Entry<Pattern,List<String>>> patternList = new ArrayList<Map.Entry<Pattern,List<String>>>();
		for(Map.Entry<String, Object> e: sec.getValues(false).entrySet()){
			String regex = e.getKey();
			// now because YAML is rubbish we have to replace some things in the regex
			regex = regex.replace("*", ".*");
			log.info("New matcher entry: "+regex);
			Pattern p = Pattern.compile(regex,Pattern.CASE_INSENSITIVE);
			if(e.getValue() instanceof List<?>){
				Map.Entry<Pattern, List<String>> ne = 	
						new AbstractMap.SimpleEntry<Pattern, List<String>>(p,(List<String>)e.getValue());
				for(String ss: ne.getValue()){
					log.info("  New val: "+ss);
				}
				patternList.add(ne);
			} else throw new RuntimeException("value not a string list in matmatch block");
		}
		log.info("Matchers: "+patternList.size());

		// now go through the biome list and see if we can find a match for those with no matlist already
		// set. There are, no doubt, more efficient ways.

		for(Biome b: Biome.values()){
			String s = b.toString();
			List<String> res = null;
			for(Map.Entry<Pattern,List<String>> e: patternList){
				Matcher m = e.getKey().matcher(s);
				if(m.matches()){
					res = e.getValue();break;
				}
			}
			if(res!=null){
				log.info("match found for "+s+", maps to:"+res);
				biomeMetaListNames.put(b, res);
			} else {
				biomeMetaListNames.put(b, defaultMetaListNames);
			}
		}


		// load the actual matlists
		sec = f.getConfigurationSection("mats");
		for(String s:sec.getKeys(false)){
			Map<MatType, RandomCollection<MaterialDataPair>> list = getMats(f,"mats."+s); // getMats uses the whole file namespace.
			log.info("Retrieved matlist "+s+", size: "+(list==null?"null":list.size()));
			materialLists.put("mats."+s, list);
			//			log.info("Matlist stored as mats."+s+":");
			//			for(int i=0;i<MATLISTCT;i++){
			//				log.info(" list ["+i+"] :"+(list[i]==null?"null":list[i].length));
			//			}
		}

		// we now have the biome meta lists, which are lists of names of material lists.
		// Now we can dereference those. But first we resolve the default (null key)

		List<Map<MatType, RandomCollection<MaterialDataPair>>> list = 
				new ArrayList<Map<MatType, RandomCollection<MaterialDataPair>>>();
		for(String s: biomeMetaListNames.get(null)){
			list.add(materialLists.get(s));
		}
		biomeMetaList.put(null,list);


		for(Biome b: Biome.values()){
			list = new ArrayList<Map<MatType, RandomCollection<MaterialDataPair>>>();
			for(String s: biomeMetaListNames.get(b)){
				list.add(materialLists.get(s));
			}
			if(list.size()==0) // use default if no list
				biomeMetaList.put(b,biomeMetaList.get(null));
			else
				biomeMetaList.put(b,list);
		}

		/**
		 * If there is are any null material lists for some matlist, resolve them to their base. If there
		 * isn't a base set explicitly, use "default"
		 */
		boolean repeat; // we loop until we didn't do it
		int retry = 0;
		do {
			repeat=false;
			for(String name:materialLists.keySet()){
				String baseName = f.getString(name+".base");
				if(baseName==null)baseName="mats.default";
				Map<MatType, RandomCollection<MaterialDataPair>> matList = materialLists.get(name);
				Map<MatType, RandomCollection<MaterialDataPair>> baseList = materialLists.get(baseName);
				//log.info("Matlist to resolve: "+name+", Base to resolve to: "+baseName);
				if(baseList==null)
					log.severe("Base material list not found:"+baseName);
				if(matList==null)
					log.severe("Actual material list not found (shouldn't happen!):"+name);
				//				for(int i=0;i<MATLISTCT;i++){
				//					log.info("Baselist ["+i+"] :"+(baseList[i]==null?"null":baseList[i].length));
				//				}
				for(MatType i: MatType.values()){
					if(matList.get(i)==null || matList.get(i).size()==0){
						repeat=true;
						//						log.info("No material list found for "+name+":"+i+", attempting base deref to "+baseName+":"+i);
						matList.put(i, baseList.get(i));
					}
				}
			}
			if(++retry == 10){
				log.severe("Unable to complete base matlist resolution, aborting.");
				return; // this will result in an exception cascade, no doubt, since the mats aren't setup.
			}
		} while(repeat);

	}
	private static void loadMatMetaListForBiome(FileConfiguration f,Biome biome){
		String name = biome==null ? "default" : biome.toString().toLowerCase();
		Logger log = GormPlugin.getInstance().getLogger();
		List<String> lst = f.getStringList("matlist."+name);
		if(lst==null || lst.size()==0){
			log.info("No metalist for biome: "+name+", using default or match");
		}else{
			log.info("Loaded metalist for biome: "+name+", count = "+String.valueOf(lst.size()));			
		}
		biomeMetaListNames.put(biome, lst);
	}
	
	



	/**
	 * Given a key, return the materials array. This is an array of arrays of materialdatapairs. The outer
	 * index is the material type (primary,secondary, etc.). The inner index is a random bunch of materials
	 * to select from. The first material is the most likely.
	 * @param key
	 */
	private static Map<MatType,RandomCollection<MaterialDataPair>> getMats(FileConfiguration f,String key){
		Map<MatType,RandomCollection<MaterialDataPair>> mats = new HashMap<MatType,RandomCollection<MaterialDataPair>>();

		mats.put(MatType.PRIMARY,Config.getMaterialDataPairs(f,key+".primary")); 
		mats.put(MatType.SECONDARY,Config.getMaterialDataPairs(f,key+".secondary")); 
		mats.put(MatType.SUPSECONDARY,Config.getMaterialDataPairs(f,key+".supsecondary")); 
		mats.put(MatType.ORNAMENT,Config.getMaterialDataPairs(f,key+".ornament")); 
		mats.put(MatType.STAIR,Config.getMaterialDataPairs(f,key+".stair")); 
		mats.put(MatType.FENCE,Config.getMaterialDataPairs(f,key+".fence")); 
		mats.put(MatType.ROOFSTEPS,Config.getMaterialDataPairs(f,key+".roofsteps")); 
		mats.put(MatType.GROUND,Config.getMaterialDataPairs(f,key+".ground")); 
		mats.put(MatType.POLE,Config.getMaterialDataPairs(f,key+".pole")); 
		mats.put(MatType.WINDOW,Config.getMaterialDataPairs(f,key+".window")); 
		mats.put(MatType.DOOR,Config.getMaterialDataPairs(f,key+".door")); 
		mats.put(MatType.FLOOR,Config.getMaterialDataPairs(f,key+".floor")); 
		mats.put(MatType.FLOWERS,Config.getMaterialDataPairs(f,key+".flowers")); 
		mats.put(MatType.RAREFLOWERS,Config.getMaterialDataPairs(f,key+".rareflowers")); 
		return mats;
	}
}
