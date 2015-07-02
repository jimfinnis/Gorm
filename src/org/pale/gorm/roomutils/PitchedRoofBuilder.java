package org.pale.gorm.roomutils;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.pale.gorm.Castle;
import org.pale.gorm.Direction;
import org.pale.gorm.Extent;
import org.pale.gorm.IntVector;
import org.pale.gorm.MaterialDataPair;
import org.pale.gorm.MaterialManager;
import org.pale.gorm.Turtle;

/**
 * An example of a RoofBuilder.
 * @author white
 *
 */
public class PitchedRoofBuilder extends RoofBuilder {

	@Override
	public int buildRoof(MaterialManager mgr,Extent buildingExtent) {
		// simple ridged roof
		// get extent
		Extent roof = new Extent(buildingExtent);
		if(Castle.getInstance().r.nextFloat()<0.5)
			roof=roof.expand(1, Extent.X|Extent.Z); // so we get eaves sometimes
		
		roof.miny = roof.maxy; // we'll work out maxy in a bit, maybe
		// which is the longest edge?
		Direction dir;
		int longEdge;
		int shortEdge;
		int startx;
		if(roof.xsize() > roof.zsize()){
			dir = Direction.SOUTH;
			startx = roof.maxx;
			longEdge = roof.xsize();
			shortEdge = roof.zsize();
		}else{
			dir = Direction.EAST;
			startx = roof.minx;
			longEdge = roof.zsize();
			shortEdge = roof.xsize();
		}
		int maxy=0;
		
		MaterialDataPair mat = mgr.getRoofSteps();
		MaterialDataPair fillMat = MaterialDataPair.fromSteps(mat.m);
		Turtle t = new Turtle(mgr,Castle.getInstance().getWorld(),new IntVector(0,0,0),dir);
		t.setMaterial(mat);
		t.setModeFlag(Turtle.NOTINDOORS);
		IntVector base1 = new IntVector(startx,roof.miny,roof.minz);
		IntVector base2 = base1.add(dir.vec.scale(shortEdge-1));
		
		for(int i=0;i<shortEdge/2;i++){
			IntVector pos = base1.add(dir.vec.scale(i)).add(0,i,0);
			if(pos.y>maxy)
				maxy=pos.y;
			t.moveAbsolute(pos);
			t.clrModeFlag(Turtle.BACKSTAIRS);
			for(int j=0;j<longEdge;j++){
				t.write();
				roofFillDown(t,roof.miny,fillMat);
				t.right();
			}
			t.setModeFlag(Turtle.BACKSTAIRS);
			pos = base2.subtract(dir.vec.scale(i)).add(0,i,0);
			t.moveAbsolute(pos);
			for(int j=0;j<longEdge;j++){
				t.write();
				roofFillDown(t,roof.miny,fillMat);
				t.right();
			}
		}
		
		// need a centre ridge, the roof size is odd.
		if(shortEdge%2 != 0){
			t.clrModeFlag(Turtle.BACKSTAIRS);
			t.setMaterial(mgr.getPrimary());
			IntVector pos = base1.add(dir.vec.scale(shortEdge/2)).add(0,shortEdge/2,0);
			t.moveAbsolute(pos);
			for(int j=0;j<longEdge;j++){
				t.write();
				roofFillDown(t,roof.miny,fillMat);
				if(j==0 || j==longEdge-1){ // put torches on the ends
					t.up();
					Block b = t.get();
					b.setType(Material.TORCH);
					t.down();
				}
				t.right();
			}
		}
		
		return 1 + (maxy - buildingExtent.maxy); 

	}

	/**
	 * Make the turtle drop down and fill from the current point
	 * @param t
	 */
	private void roofFillDown(Turtle t,int y,MaterialDataPair mp) {
		t = new Turtle(t); // work on a local copy of the turtle
		t.setMaterial(mp);
		for(;;){
			t.down();
			if(t.getPos().y>=y){
				t.write();
			} else
				break;
		}
	}

}
