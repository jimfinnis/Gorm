package org.pale.gorm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.block.Biome;

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
	private static Set<String> listNames;
	public static void loadMats(){
		// first, we load the material metalists for each biome. These are lists
		// of material list names from which we select at random to generate some terrain
		// in that biome. The material lists themselves contain the material data.
		Map<String,MaterialDataPair[][]> materialLists = new HashMap<String,MaterialDataPair[][]>();
		biomeMetaListNames = new HashMap<Biome,List<String>>();
		biomeMetaList = new HashMap<Biome,MaterialDataPair[][][]>();
		listNames = new HashSet<String>();
		for(Biome b: Biome.values()){
			loadMatMetaListForBiome(b);
		}
		// and the default
		loadMatMetaListForBiome(null);
		// that done, we now go through the names we got and try to load them.
		for(String s:listNames){
			MaterialDataPair[][] list = getMats(s);
			materialLists.put(s, list);
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
	}
	private static void loadMatMetaListForBiome(Biome biome){
		String name = biome==null ? "default" : biome.toString().toLowerCase();
		Logger log = GormPlugin.getInstance().getLogger();
		List<String> lst = GormPlugin.getInstance().getConfig()
				.getStringList("matlist."+name);
		if(lst==null || lst.size()==0){
			log.info("No metalist for biome: "+name+", using default");
			lst = GormPlugin.getInstance().getConfig().getStringList("matlist.default");
		}else{
			log.info("Loaded metalist for biome: "+name+", count = "+String.valueOf(lst.size()));			
		}
		if(lst==null || lst.size()==0){
			log.severe("no metalist for "+name+" and no default either!");
		} else {
			// tell the biome what the names of the lists are
			biomeMetaListNames.put(biome, lst);
			// add the names to the global list so we can check they exist later.
			for(String s: lst)
				listNames.add(s);
		}
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
 