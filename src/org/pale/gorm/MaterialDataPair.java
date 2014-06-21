package org.pale.gorm;

import org.bukkit.Material;

public class MaterialDataPair {
	public MaterialDataPair(Material m, int d) {
		this.m = m;
		this.d = d;
	}
	public MaterialDataPair(MaterialDataPair p){
		this.m = p.m;
		this.d = p.d;
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
			GormPlugin.log("cannot get steps for material "+m.toString());
			return null;

		}

	}
}