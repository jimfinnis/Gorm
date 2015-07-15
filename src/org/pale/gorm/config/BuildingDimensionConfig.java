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
	private double size,height;
	private double aspect;
	private Building parent;
	private IntVector loc;
	
	public BuildingDimensionConfig(Building parent,IntVector loc,ConfigurationSection c) throws MissingAttributeException {
		this.parent=parent;
		this.loc = loc;
		ConfigurationSection cf = c.getConfigurationSection("dimensions");
		if(cf==null)
			throw new ConfigUtils.MissingAttributeException("dimensions", c);
		
		type = cf.getString("type");
		size = ConfigUtils.getRandomValueInRange(cf,"size");
		height = ConfigUtils.getRandomValueInRange(cf,"height");
		aspect = ConfigUtils.getRandomValueInRange(cf,"aspect");
	}

	public IntVector getDimensions() {
		if(type.equals("corridor")){
			return getDimsCorridor();
		} else if(type.equals("standard")){
			return getDimsStd();
		} else 
			return getDimsSimple();
	}
	
	private IntVector getDimsSimple(){
		Random rnd = Castle.getInstance().r;
		double dx,dz;
		if(rnd.nextBoolean()){
			dx = size; dz = size*aspect;
		} else {
			dx = size*aspect; dz = size;
		}
		int x = (int)dx;
		int z = (int)dz;
		return new IntVector(x,(int)height,z);
	}
	
	private IntVector getDimsCorridor(){
		Random rnd = Castle.getInstance().r;

		// get the new building's extent (i.e. location and size)

		double dx = rnd.nextInt(5) + 3;
		double dz = dx;
		if(rnd.nextBoolean())
			dz *= aspect;
		else
			dx *= aspect;
		int x = (int)dx;
		int z = (int)dz;

		int y;
		double height = Noise.noise2Dfractal(loc.x, loc.z, 1, 2, 3, 0.5); // 0-1																		  // noise
		double variance = Noise.noise2Dfractal(loc.x, loc.z, 2, 2, 3, 0.5);
		y = rnd.nextInt(5 + Noise.scale(10, variance)) + 10;
		y = Noise.scale(y, height) + 5;
		return new IntVector(x,y,z);
	}
	
	private IntVector getDimsStd(){
		Random rnd = Castle.getInstance().r;

		double dx = rnd.nextInt(10) + 5;
		double dz = dx;
		if(rnd.nextBoolean())
			dz *= aspect;
		else
			dx *= aspect;
		GormPlugin.log("Size: "+dx+","+dz);
		int x = (int)dx;
		int z = (int)dz;
		GormPlugin.log("Size: "+x+","+z);
		
		int y;
		double height = Noise.noise2Dfractal(loc.x, loc.z, 1, 2, 3, 0.5); //0-1 noise
		double variance = Noise.noise2Dfractal(loc.x,loc.z,2,1,3,0.5);

		// TODO this is a blatant special case hack.
		if(parent!=null && parent.rooms.getFirst().tallNeighbourRequired() && rnd.nextFloat()<0.8) {
			// if the top room of the parent has a garden, high chance that we're a good bit taller.
			y = parent.extent.ysize() + 4 + rnd.nextInt((int)(height*10.0));
		} else {
			y = rnd.nextInt(5+Noise.scale(15, variance))+10;
			y = Noise.scale(y,height)+5;
		}
		
		x = (int)(((double)x)*size);
		z = (int)(((double)z)*size);
		y = (int)(((double)y)*height);
		GormPlugin.log("Size: "+x+","+z);
		return new IntVector(x,y,z);
	}
}
