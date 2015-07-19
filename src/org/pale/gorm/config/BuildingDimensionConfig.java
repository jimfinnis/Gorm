package org.pale.gorm.config;

import java.util.Random;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.pale.gorm.Building;
import org.pale.gorm.Castle;
import org.pale.gorm.GormPlugin;
import org.pale.gorm.IntVector;
import org.pale.gorm.Noise;
import org.pale.gorm.config.ConfigUtils.MissingAttributeException;

public class BuildingDimensionConfig {

	private String type;
	private double height; // height range (which will be scaled by a position noise factor)
	private double[] size; // size range (for x,y in normal rooms; corridors are different)
	private int fixedheight=-1; // overrides height if present
	private Building parent;
	private IntVector loc;

	public BuildingDimensionConfig(Building parent,IntVector loc,ConfigurationSection c) throws MissingAttributeException {
		this.parent=parent;
		this.loc = loc;
		ConfigurationSection cf = c.getConfigurationSection("dimensions");
		if(cf==null)
			throw new ConfigUtils.MissingAttributeException("dimensions", c);

		type = cf.getString("type");
		size = ConfigUtils.getDoubleRange(cf, "size");
		if(cf.contains("height"))
			height = ConfigUtils.getRandomValueInRange(cf,"height");
		else if(cf.contains("fixedheight"))			
			fixedheight = ConfigUtils.getRandomValueInRangeInt(cf,"fixedheight");
		else throw new MissingAttributeException("height or fixedheight", cf);
	}

	public IntVector getDimensions() {
		IntVector r;
		if(type.equals("corridor")){
			r=getDimsCorridor();
		}else{
			r=getDimsStd();
		}
		if(fixedheight>0)
			r.y=fixedheight;
		return r;
	}

	private IntVector getDimsCorridor(){
		Random rnd = Castle.getInstance().r;

		int x = (int)(size[0]+rnd.nextDouble()*(size[1]-size[0]));
		int z = x/4;
		if(z>6)z=6;
		else if(z<4)z=4;

		if(rnd.nextBoolean()){
			int q=x;
			x=z;
			z=q;
		}
		int y;
		double height = Noise.noise2Dfractal(loc.x, loc.z, 1, 2, 3, 0.5); // 0-1																		  // noise
		double variance = Noise.noise2Dfractal(loc.x, loc.z, 2, 2, 3, 0.5);
		y = rnd.nextInt(5 + Noise.scale(10, variance)) + 10;
		y = Noise.scale(y, height) + 5;
		return new IntVector(x,y,z);
	}

	private IntVector getDimsStd(){
		Random rnd = Castle.getInstance().r;

		int x = (int)(size[0]+rnd.nextDouble()*(size[1]-size[0]));
		int z = (int)(size[0]+rnd.nextDouble()*(size[1]-size[0]));
		GormPlugin.log("Size: "+x+","+z);

		int y;
		double h = Noise.noise2Dfractal(loc.x, loc.z, 1, 2, 3, 0.5); //0-1 noise
		double variance = Noise.noise2Dfractal(loc.x,loc.z,2,1,3,0.5);

		// TODO this is a blatant special case hack.
		if(parent!=null && parent.rooms.getFirst().tallNeighbourRequired() && rnd.nextFloat()<0.8) {
			// if the top room of the parent has a garden, high chance that we're a good bit taller.
			y = parent.extent.ysize() + 4 + rnd.nextInt((int)(h*10.0));
		} else {
			GormPlugin.log("variance: "+variance+", h: "+h);
			y = rnd.nextInt(5+Noise.scale(15, variance))+10;
			GormPlugin.log("ypre: "+y);
			y = Noise.scale(y,h)+5;
			GormPlugin.log("y (before scaling): "+y);
		}

		y = (int)(((double)y)*height);
		GormPlugin.log("Size: "+x+","+z+" -- height: "+y);
		return new IntVector(x,y,z);
	}
}
