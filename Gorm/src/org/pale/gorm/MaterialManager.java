package org.pale.gorm;

import java.util.Random;

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

	/**
	 * Each base material has a set of building materials. They're in these
	 * arrays, in the order given by the constants above. Supported-secondary is
	 * a secondary which has gravity, like sand. Fences are chosen randomly;
	 * fence1 is most likely, fence2 second likely and so on,
	 */
	static final MaterialDataPair[][] matsSand = {
			{ new MaterialDataPair(Material.SANDSTONE, 1) }, // plain sandstone
			{ new MaterialDataPair(Material.SANDSTONE, 0) }, // sandstone brick
			{ new MaterialDataPair(Material.SAND, 0) }, // just sand
			{ new MaterialDataPair(Material.SANDSTONE, 2) }, // nicely carved
			{ new MaterialDataPair(Material.SANDSTONE_STAIRS, 0) },
			{ new MaterialDataPair(Material.FENCE, 0),
					new MaterialDataPair(Material.IRON_FENCE, 0) },
			{ new MaterialDataPair(Material.SANDSTONE_STAIRS, 0),
					new MaterialDataPair(Material.BIRCH_WOOD_STAIRS, 0) },
			{ new MaterialDataPair(Material.SAND, 0),
					new MaterialDataPair(Material.GRAVEL, 0) },
			{ new MaterialDataPair(Material.WOOD, 0) }, };
	static final MaterialDataPair[][] matsNether = {
			{ new MaterialDataPair(Material.NETHER_BRICK, 0) },
			{ new MaterialDataPair(Material.NETHER_BRICK, 0) },
			{ new MaterialDataPair(Material.AIR, 0) },
			{ new MaterialDataPair(Material.NETHER_BRICK, 0) },
			{ new MaterialDataPair(Material.NETHER_BRICK_STAIRS, 0) },
			{ new MaterialDataPair(Material.NETHER_FENCE, 0),
					new MaterialDataPair(Material.IRON_FENCE, 0) },
			{ new MaterialDataPair(Material.NETHER_BRICK_STAIRS, 0),
					new MaterialDataPair(Material.NETHER_BRICK_STAIRS, 0) },
			{ new MaterialDataPair(Material.SOUL_SAND, 0),
					new MaterialDataPair(Material.NETHERRACK, 0) },
			{ new MaterialDataPair(Material.NETHER_BRICK, 0) }, };
	static final MaterialDataPair[][] matsNormal = {
			{ new MaterialDataPair(Material.SMOOTH_BRICK, 0) },
			{ new MaterialDataPair(Material.STONE, 0) },
			{ new MaterialDataPair(Material.GRAVEL, 0) },
			{ new MaterialDataPair(Material.SMOOTH_BRICK, 3) },
			{ new MaterialDataPair(Material.SMOOTH_STAIRS, 0),
					new MaterialDataPair(Material.BIRCH_WOOD_STAIRS, 0),
					new MaterialDataPair(Material.JUNGLE_WOOD_STAIRS, 0),
					new MaterialDataPair(Material.SPRUCE_WOOD_STAIRS, 0) },
			{ new MaterialDataPair(Material.FENCE, 0),
					new MaterialDataPair(Material.COBBLE_WALL, 0),
					new MaterialDataPair(Material.IRON_FENCE, 0), },
			{ new MaterialDataPair(Material.WOOD_STAIRS, 0),
					new MaterialDataPair(Material.SMOOTH_STAIRS, 0),
					new MaterialDataPair(Material.BIRCH_WOOD_STAIRS, 0),
					new MaterialDataPair(Material.JUNGLE_WOOD_STAIRS, 0),
					new MaterialDataPair(Material.SPRUCE_WOOD_STAIRS, 0),
					new MaterialDataPair(Material.OBSIDIAN, 0),
					},
			{ new MaterialDataPair(Material.GRASS, 0),
					new MaterialDataPair(Material.GRAVEL, 0),
					new MaterialDataPair(Material.SAND, 0) },
			{ new MaterialDataPair(Material.WOOD, 0),
					new MaterialDataPair(Material.COBBLE_WALL, 0) }, };
	static final MaterialDataPair[][] matsCobble = {
			{ new MaterialDataPair(Material.COBBLESTONE, 0) },
			{ new MaterialDataPair(Material.COBBLESTONE, 0) },
			{ new MaterialDataPair(Material.GRAVEL, 0) },
			{ new MaterialDataPair(Material.COBBLESTONE, 3) },
			{ new MaterialDataPair(Material.COBBLESTONE_STAIRS, 0) },
			{ new MaterialDataPair(Material.IRON_FENCE, 0),
					new MaterialDataPair(Material.FENCE, 0),
					new MaterialDataPair(Material.COBBLE_WALL, 0) },
			{ new MaterialDataPair(Material.WOOD_STAIRS, 0),
					new MaterialDataPair(Material.COBBLESTONE_STAIRS, 0) },
			{ new MaterialDataPair(Material.GRAVEL, 0),
					new MaterialDataPair(Material.GRASS, 0) },
			{ new MaterialDataPair(Material.WOOD, 0),
					new MaterialDataPair(Material.COBBLE_WALL, 0) }, };

	static final MaterialDataPair[][][] matsListsSandy = { matsSand, matsSand,
			matsSand, matsNormal };
	static final MaterialDataPair[][][] matsListsNormal = { matsNormal,
			matsNormal, matsNormal, matsCobble };
	static final MaterialDataPair[][][] matsListsNether = { matsNether,
		matsNether, matsNether, matsNether };

	private MaterialDataPair primary, secondary, supSecondary, ornament, fence,
			ground, pole;
	private Material stairs, roofSteps;

	public MaterialManager(Biome b) {
		Random r = Castle.getInstance().r;
		MaterialDataPair[][] baseMats;
		switch (b) {
		case DESERT:
		case DESERT_HILLS:
		case BEACH:
			baseMats = matsListsSandy[r.nextInt(matsListsSandy.length)];
			break;
		case HELL:
			baseMats = matsListsNether[r.nextInt(matsListsNether.length)];
			break;
		default:
			baseMats = matsListsNormal[r.nextInt(matsListsNormal.length)];
			break;
		}
		primary = getRandom(r, baseMats[PRIMARY]);
		secondary = getRandom(r, baseMats[SECONDARY]);
		supSecondary = getRandom(r, baseMats[SUPSECONDARY]);
		ornament = getRandom(r, baseMats[ORNAMENT]);
		stairs = getRandom(r, baseMats[STAIR]).m;
		ground = getRandom(r, baseMats[GROUND]);
		fence = getRandom(r, baseMats[FENCE]);
		pole = getRandom(r, baseMats[POLE]);
		roofSteps = getRandom(r, baseMats[ROOFSTEPS]).m;
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
}
