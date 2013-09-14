package org.pale.gorm.roomutils;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.pale.gorm.Castle;
import org.pale.gorm.Extent;
import org.pale.gorm.IntVector;
import org.pale.gorm.Turtle;

public class PitchedRoofBuilder implements RoofBuilder {

	@Override
	public void buildRoof(Extent roomExtent) {
		// simple ridged roof
		// get extent
		Extent roof = new Extent(roomExtent);
		if(Castle.getInstance().r.nextFloat()<0.5)
			roof=roof.expand(1, Extent.X|Extent.Z); // so we get eaves sometimes
		
		roof.miny = roof.maxy + 1; // we'll work out maxy in a bit, maybe
		// which is the longest edge?
		IntVector.Direction dir;
		int longEdge;
		int shortEdge;
		int startx;
		if(roof.xsize() > roof.zsize()){
			dir = IntVector.Direction.SOUTH;
			startx = roof.maxx;
			longEdge = roof.xsize();
			shortEdge = roof.zsize();
		}else{
			dir = IntVector.Direction.EAST;
			startx = roof.minx;
			longEdge = roof.zsize();
			shortEdge = roof.xsize();
		}
		
		Turtle t = new Turtle(Castle.getInstance().getWorld(),new IntVector(0,0,0),dir);
		t.setMaterial(Material.WOOD_STAIRS);
		IntVector base1 = new IntVector(startx,roof.miny,roof.minz);
		IntVector base2 = base1.add(dir.vec.scale(shortEdge-1));
		
		for(int i=0;i<shortEdge/2;i++){
			IntVector pos = base1.add(dir.vec.scale(i)).add(0,i,0);
			t.moveAbsolute(pos);
			t.clrModeFlag(Turtle.BACKSTAIRS);
			for(int j=0;j<longEdge;j++){
				t.write();
				roofFillDown(t,roof.miny,Material.WOOD);
				t.right();
			}
			t.setModeFlag(Turtle.BACKSTAIRS);
			pos = base2.subtract(dir.vec.scale(i)).add(0,i,0);
			t.moveAbsolute(pos);
			for(int j=0;j<longEdge;j++){
				t.write();
				roofFillDown(t,roof.miny,Material.WOOD);
				t.right();
			}
		}
		
		// need a centre ridge, the roof size is odd.
		if(shortEdge%2 != 0){
			t.clrModeFlag(Turtle.BACKSTAIRS);
			t.setMaterial(Material.WOOD);
			IntVector pos = base1.add(dir.vec.scale(shortEdge/2)).add(0,shortEdge/2,0);
			t.moveAbsolute(pos);
			for(int j=0;j<longEdge;j++){
				t.write();
				roofFillDown(t,roof.miny,Material.WOOD);
				if(j==0 || j==longEdge-1){ // put torches on the ends
					t.up();
					Block b = t.get();
					b.setType(Material.TORCH);
					t.down();
				}
				t.right();
			}
		}

	}

	/**
	 * Make the turtle drop down and fill from the current point
	 * @param t
	 */
	private void roofFillDown(Turtle t,int y,Material m) {
		t = new Turtle(t); // work on a local copy of the turtle
		t.setMaterial(m);
		for(;;){
			t.down();
			if(t.getPos().y>=y){
				t.write();
			} else
				break;
		}
	}

}
