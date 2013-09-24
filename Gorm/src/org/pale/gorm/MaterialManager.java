package org.pale.gorm;

import java.util.ArrayList;
import java.util.List;
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

	public static class MaterialDataPair {
		MaterialDataPair(Material m, int d) {
			this.m = m;
			this.d = d;
		}

		public Material m;
		public int d;

		public MaterialDataPair toSteps() {
			switch (m) {
			case WOOD:
				switch (d) {
				case 1:
					m = Material.SPRUCE_WOOD_STAIRS;
					break;
				case 2:
					m = Material.BIRCH_WOOD_STAIRS;
					break;
				case 3:
					m = Material.JUNGLE_WOOD_STAIRS;
					break;
				default:
					m = Material.WOOD_STAIRS;
					break;
				}
				return new MaterialDataPair(m, 0);
			case COBBLESTONE:
				return new MaterialDataPair(Material.COBBLESTONE_STAIRS, 0);
			case QUARTZ:
				return new MaterialDataPair(Material.QUARTZ_STAIRS, 0);
			case NETHER_BRICK:
			case NETHERRACK:
				return new MaterialDataPair(Material.NETHER_BRICK_STAIRS, 0);
			case BRICK:
				return new MaterialDataPair(Material.BRICK_STAIRS, 0);
			case SAND:
			case SANDSTONE:
				return new MaterialDataPair(Material.SANDSTONE_STAIRS, 0);
			default:
			case STONE:
			case SMOOTH_BRICK:
				return new MaterialDataPair(Material.SMOOTH_STAIRS, 0);

			}
		}

		public static MaterialDataPair fromSteps(Material m) {
			switch (m) {
			case SMOOTH_STAIRS:
				return new MaterialDataPair(Material.SMOOTH_BRICK, 0);
			case SANDSTONE_STAIRS:
				return new MaterialDataPair(Material.SANDSTONE, 0);
			case WOOD_STAIRS:
				return new MaterialDataPair(Material.WOOD, 0);
			case JUNGLE_WOOD_STAIRS:
				return new MaterialDataPair(Material.WOOD, 3);
			case BIRCH_WOOD_STAIRS:
				return new MaterialDataPair(Material.WOOD, 2);
			case SPRUCE_WOOD_STAIRS:
				return new MaterialDataPair(Material.WOOD, 1);
			case BRICK_STAIRS:
				return new MaterialDataPair(Material.WOOD, 0);
			case QUARTZ_STAIRS:
				return new MaterialDataPair(Material.QUARTZ, 0);
			case NETHER_BRICK_STAIRS:
				return new MaterialDataPair(Material.NETHER_BRICK, 0);
			case COBBLESTONE_STAIRS:
				return new MaterialDataPair(Material.COBBLESTONE, 0);
			default:
				return null;

			}

		}
	}

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
			{ new MaterialDataPair(Material.SANDSTONE_STAIRS, 0) },
			{ new MaterialDataPair(Material.SAND, 0),
					new MaterialDataPair(Material.GRAVEL, 0) },
			{ new MaterialDataPair(Material.WOOD, 0) }, };
	static final MaterialDataPair[][] matsNormal = {
			{ new MaterialDataPair(Material.SMOOTH_BRICK, 0) },
			{ new MaterialDataPair(Material.STONE, 0) },
			{ new MaterialDataPair(Material.GRAVEL, 0) },
			{ new MaterialDataPair(Material.SMOOTH_BRICK, 3) },
			{ new MaterialDataPair(Material.SMOOTH_STAIRS, 0) },
			{ new MaterialDataPair(Material.FENCE, 0),
					new MaterialDataPair(Material.COBBLE_WALL, 0),
					new MaterialDataPair(Material.IRON_FENCE, 0), },
			{ new MaterialDataPair(Material.WOOD_STAIRS, 0),
					new MaterialDataPair(Material.SMOOTH_STAIRS, 0), },
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

	private MaterialDataPair primary, secondary, supSecondary, ornament, fence,
			ground,pole;
	private Material stairs, roofSteps;

	public MaterialManager(Biome b) {
		Random r = Castle.getInstance().r;
		MaterialDataPair[][] baseMats;
		GormPlugin.log("creating mat mgr: biome=" + b.toString());
		switch (b) {
		case DESERT:
		case DESERT_HILLS:
		case BEACH:
			baseMats = matsListsSandy[r.nextInt(matsListsSandy.length)];
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

	static final double RANDOMLAMBDA = 6;

	/**
	 * exponential random number from 0 to n-1
	 * 
	 * @param n
	 * @return
	 */
	private int randomExp(Random r, int n) {
		int i;
		do {
			double u = r.nextFloat(); // uniform from 0 to 1
			u = (Math.log10(1 - u) / Math.log10(2)) / -RANDOMLAMBDA;
			i = (int) (u * n);
		} while (i >= n);
		return i;
	}

	private MaterialDataPair getRandom(Random r,
			MaterialDataPair[] materialDataPairs) {
		if (materialDataPairs.length == 1)
			return materialDataPairs[0];
		else
			return materialDataPairs[randomExp(r, materialDataPairs.length)];
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
	
	public MaterialDataPair getPole(){
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
