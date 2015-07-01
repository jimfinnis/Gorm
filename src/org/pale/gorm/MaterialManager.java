package org.pale.gorm;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Material;
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
	private static final int PRIMARY = 0;
	private static final int SECONDARY = 1;
	private static final int SUPSECONDARY = 2;
	private static final int ORNAMENT = 3;
	private static final int STAIR = 4;
	private static final int FENCE = 5;
	private static final int ROOFSTEPS = 6;
	private static final int GROUND = 7;
	private static final int POLE = 8;
	private static final int WINDOW = 9;

	private static final int MATLISTCT = 10;


	private MaterialDataPair primary, secondary, supSecondary, ornament, fence,
	ground, pole, window;

	// in 1.8, there are several different kinds of door. We have an array
	// of possible doortypes for different biomes.
	static final Material doorMatsDefault[] = {
		Material.WOODEN_DOOR, Material.ACACIA_DOOR, Material.SPRUCE_DOOR,
		Material.WOODEN_DOOR, Material.WOODEN_DOOR, Material.DARK_OAK_DOOR,
		Material.IRON_DOOR
	};

	private Material stairs, roofSteps;
	private Material doorMats[];
	// three level array for each biome!
	// Top level, set of top level data for the biome to pick from randomly
	// when the manager is initialised.
	// Next level, different types as indexed by function with  the constants above
	// Bottom level, list of material/data pairs to pick from for that function.

	private static Map<Biome,MaterialDataPair[][][]> biomeMetaList;

	public MaterialManager(Biome b) {
		Random r = Castle.getInstance().r;

		doorMats = doorMatsDefault; // TODO proper door randomness

		MaterialDataPair[][][] lst = biomeMetaList.get(b);
		MaterialDataPair[][] baseMats = lst[r.nextInt(lst.length)];

		primary = getRandom(r, baseMats[PRIMARY]);
		secondary = getRandom(r, baseMats[SECONDARY]);
		supSecondary = getRandom(r, baseMats[SUPSECONDARY]);
		ornament = getRandom(r, baseMats[ORNAMENT]);
		stairs = getRandom(r, baseMats[STAIR]).m;
		ground = getRandom(r, baseMats[GROUND]);
		fence = getRandom(r, baseMats[FENCE]);
		pole = getRandom(r, baseMats[POLE]);
		roofSteps = getRandom(r, baseMats[ROOFSTEPS]).m;
		window = getRandom(r, baseMats[WINDOW]);
	}

	public Material getRandomDoor(Random r){
		return doorMats[r.nextInt(doorMats.length)];
	}

	private MaterialDataPair getRandom(Random r,
			MaterialDataPair[] materialDataPairs) {
		if (materialDataPairs.length == 1)
			return materialDataPairs[0];
		else
			return materialDataPairs[Util
			                         .randomExp(r, materialDataPairs.length)];
	}

	public MaterialDataPair getPrimary() {
		return primary;
	}

	public MaterialDataPair getSecondary() {
		return secondary;
	}

	public MaterialDataPair getSupSecondary() {
		return supSecondary;
	}

	public MaterialDataPair getOrnament() {
		return ornament;
	}

	/**
	 * Note that this returns a material; the data determines the direction
	 *
	 * @return
	 */
	public Material getRoofSteps() {
		return roofSteps;
	}

	public MaterialDataPair getPole() {
		return pole;
	}

	/**
	 * Note that this returns a material; the data determines the direction
	 *
	 * @return
	 */
	public Material getStair() {
		return stairs;
	}

	public MaterialDataPair getGround() {
		return ground;
	}

	public MaterialDataPair getFence() {
		return fence;
	}

	public MaterialDataPair getWindow() {
		return window;
	}

	private static HashMap<Biome,List<String>> biomeMetaListNames;
	public static void loadMats(){
		// first, we load the material metalists for each biome. These are lists
		// of material list names from which we select at random to generate some terrain
		// in that biome. The material lists themselves contain the material data.
		Logger log = GormPlugin.getInstance().getLogger();
		FileConfiguration conf = GormPlugin.getInstance().getConfig();
		Map<String,MaterialDataPair[][]> materialLists = new HashMap<String,MaterialDataPair[][]>();
		biomeMetaListNames = new HashMap<Biome,List<String>>();
		biomeMetaList = new HashMap<Biome,MaterialDataPair[][][]>();
		for(Biome b: Biome.values()){
			loadMatMetaListForBiome(b);
		}
		// and the default
		loadMatMetaListForBiome(null);
		List<String> defaultMetaListNames = biomeMetaListNames.get(null);
		if(defaultMetaListNames==null)
			log.severe("no default metalist given - could cause problems!");

		// first build a list of patterns and the matlists to which they resolve.
		ConfigurationSection sec = conf.getConfigurationSection("matmatch");
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
		sec = conf.getConfigurationSection("mats");
		for(String s:sec.getKeys(false)){
			MaterialDataPair[][] list = getMats("mats."+s); // getMats uses the whole file namespace.
			log.info("Retrieved matlist "+s+", size: "+(list==null?"null":list.length));
			materialLists.put("mats."+s, list);
			//			log.info("Matlist stored as mats."+s+":");
			//			for(int i=0;i<MATLISTCT;i++){
			//				log.info(" list ["+i+"] :"+(list[i]==null?"null":list[i].length));
			//			}
		}

		// we now have the biome meta lists, which are lists of names of material lists.
		// Now we can dereference those. But first we resolve the default (null key)

		List<MaterialDataPair[][]> list = new ArrayList<MaterialDataPair[][]>();
		for(String s: biomeMetaListNames.get(null)){
			list.add(materialLists.get(s));
		}
		biomeMetaList.put(null,list.toArray(new MaterialDataPair[0][][]));


		for(Biome b: Biome.values()){
			list = new ArrayList<MaterialDataPair[][]>();
			for(String s: biomeMetaListNames.get(b)){
				list.add(materialLists.get(s));
			}
			if(list.size()==0) // use default if no list
				biomeMetaList.put(b,biomeMetaList.get(null));
			else
				biomeMetaList.put(b,list.toArray(new MaterialDataPair[0][][]));
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
				String baseName = conf.getString(name+".base");
				if(baseName==null)baseName="mats.default";
				MaterialDataPair matList[][] = materialLists.get(name);
				MaterialDataPair baseList[][] = materialLists.get(baseName);
				//log.info("Matlist to resolve: "+name+", Base to resolve to: "+baseName);
				if(baseList==null)
					log.severe("Base material list not found:"+baseName);
				if(matList==null)
					log.severe("Actual material list not found (shouldn't happen!):"+name);
				//				for(int i=0;i<MATLISTCT;i++){
				//					log.info("Baselist ["+i+"] :"+(baseList[i]==null?"null":baseList[i].length));
				//				}
				for(int i=0;i<MATLISTCT;i++){
					if(matList[i]==null || matList[i].length==0){
						repeat=true;
						//						log.info("No material list found for "+name+":"+i+", attempting base deref to "+baseName+":"+i);
						matList[i] = baseList[i];
					}
				}
			}
			if(++retry == 10){
				log.severe("Unable to complete base matlist resolution, aborting.");
				return; // this will result in an exception cascade, no doubt, since the mats aren't setup.
			}
		} while(repeat);

	}
	private static void loadMatMetaListForBiome(Biome biome){
		String name = biome==null ? "default" : biome.toString().toLowerCase();
		Logger log = GormPlugin.getInstance().getLogger();
		List<String> lst = GormPlugin.getInstance().getConfig()
				.getStringList("matlist."+name);
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
	private static MaterialDataPair[][] getMats(String key){
		MaterialDataPair mats[][] = new MaterialDataPair[MATLISTCT][];

		mats[PRIMARY]=ConfigUtils.getMaterialDataPairs(key+".primary"); 
		mats[SECONDARY]=ConfigUtils.getMaterialDataPairs(key+".secondary"); 
		mats[SUPSECONDARY]=ConfigUtils.getMaterialDataPairs(key+".supsecondary"); 
		mats[ORNAMENT]= ConfigUtils.getMaterialDataPairs(key+".ornament"); 
		mats[STAIR]= ConfigUtils.getMaterialDataPairs(key+".stair"); 
		mats[FENCE]= ConfigUtils.getMaterialDataPairs(key+".fence"); 
		mats[ROOFSTEPS]= ConfigUtils.getMaterialDataPairs(key+".roofsteps"); 
		mats[GROUND]= ConfigUtils.getMaterialDataPairs(key+".ground"); 
		mats[POLE]= ConfigUtils.getMaterialDataPairs(key+".pole"); 
		mats[WINDOW]= ConfigUtils.getMaterialDataPairs(key+".window"); 
		return mats;
	}
}
