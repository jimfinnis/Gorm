package org.pale.gorm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.Material;

/**
 * A room is a level of a building. Typically it's indoors, but the roof can also constitute a room.
 * @author white
 *
 */
public abstract class Room implements Comparable<Room> {
	
	@Override
	public int compareTo(Room that) {
		if(this == that)return 0;
		if(this.exits.size() < that.exits.size())
			return -1;
		if(this.exits.size() > that.exits.size())
			return 1;
		return 0;
	}

	/**
	 * The extent of the room including walls, floor and ceiling - which will overlap
	 * with adjacent rooms.
	 */
	protected Extent e;
	protected Building b;
	
	/**
	 * 
	 */

	/**
	 * This is a map of exits by the room they go to
	 */
	Map<Room, Exit> exitMap = new HashMap<Room, Exit>();

	/**
	 * Our exits - these are duplicated in the other room (if there is one)
	 */
	Collection<Exit> exits = new ArrayList<Exit>();
	
	/**
	 * Our windows - these *aren't* duplicated across adjacent rooms. Careful.
	 */
	Collection<Extent> windows = new ArrayList<Extent>();
	
	
	/**
	 * This is used to appropriately 'decorate' exits between rooms.
	 * It marks which sides of the room are to be considered 'open'
	 */
	private Set<Direction> openSides = new HashSet<Direction>();
	private static int idCounter=0;
	int id=idCounter++;
	
	int getExitCount(){
		return exits.size();
	}


	protected Room(MaterialManager mgr, Extent e, Building b) {
		this.b = b;
		this.e = new Extent(e);
		b.extent = build(mgr,b.getExtent());
	}
	
	protected void setOpenSide(Direction d){
		openSides.add(d);
	}
	
	public boolean isOpen(Direction d){
		return openSides.contains(d);
	}
	
	public void setAllSidesOpen(){
		openSides.add(Direction.NORTH);
		openSides.add(Direction.SOUTH);
		openSides.add(Direction.EAST);
		openSides.add(Direction.WEST);
	}
	
	public boolean exitIntersects(Extent e){
		for(Exit x:exits){
			if(x.getExtent().intersects(e))return true;
		}
		return false;
	}
	
	public boolean windowIntersects(Extent e){
		for(Extent x:windows){
			if(x.intersects(e))return true;
		}
		return false;
	}
	
	public void addWindow(Extent e){
		windows.add(new Extent(e));
	}

	public Extent getExtent(){
		return e;
	}
	

	/**
	 * try to make an exit with another room with few exits.
	 * THIS IS GOING TO BE A MAJOR BOTTLENECK. We need to narrow down the candidates.
	 * @return
	 */
	public boolean attemptMakeExit() {
		// iterate rooms in exit count order - the caller is doing this too.
		// We keep going until we succeed, which we might not.
		for(Room that: Castle.getInstance().getRooms()){
			//GormPlugin.log("This: "+e.toString()+" That: "+that.e.toString());
			// don't try to connect a room to itself or another in the same building
			if(that==this || that.b == this.b )continue;
			if(e.intersects(that.e)){
				// if the rooms intersect, do a quick sanity check.
				Extent inter = e.intersect(that.e);
				if (inter.xsize() != 1 && inter.zsize() != 1) {
					throw new RuntimeException(
							"two rooms intersect >1 deep");
				}
				// only make a link if the two have similar floor heights, and the zone of intersection
				// is large in XZ.
				GormPlugin.log(String.format("floor diff: %d, intersect: %d",
						Math.abs(this.e.miny-that.e.miny),
								Math.max(inter.xsize(),inter.zsize())));
				
				if(Math.abs(this.e.miny-that.e.miny)<5 && 
						Math.max(inter.xsize(),inter.zsize())>5){
					if(makeRandomExit(that))
						return true;
					
				}

			}
		}
		return false;
	}
	
	/**
	 * Exits 'drop down' to the destination, so the destination needs to be lower.
	 * @param that
	 * @return
	 */
	private boolean makeRandomExit(Room that) {
		if(this.e.miny > that.e.miny)
			return makeExitBetweenRooms(that);
		else
			return that.makeExitBetweenRooms(this);
		
	}

	static final int[] offsets={0,1,-1,2,-2,3,-3,4,-4,5,-5};
	/**
	 * This is used to make a random horizontal exit to a building we already
	 * know intersects enough with us
	 * 
	 * @param r
	 */
	private boolean makeExitBetweenRooms(Room that) {
		Extent intersection = this.e.intersect(that.e);
		Direction dir;

		GormPlugin.log(String.format(
				"attempting to create exit between %d/%d and %d/%d", this.b.id,this.id,
				that.b.id,that.id));

		if (this.exitMap.containsKey(that)) {
			GormPlugin.log(String.format("exit already exists"));
			return false;
		}

		// work out the orientation of the exit
		if (intersection.xsize() > intersection.zsize()) {
			dir = that.e.minz == this.e.maxz ? Direction.SOUTH
					: Direction.NORTH;
		} else {
			dir = that.e.minx == this.e.maxx ? Direction.EAST
					: Direction.WEST;
		}

		int height = 3;
		
		// shrink the intersection along its longest axis, so we don't end
		// up sliding the exit into a wall to avoid a window.
		intersection=intersection.expand(-1,Extent.LONGESTXZ);

		// try to find somewhere in the intersection which doesn't collide with
		// a window or existing exit

		IntVector offsetVec = dir.vec.rotate(1); // get a perpendicular to slide along
		for (int tries = 0; tries < offsets.length; tries++) {
			int offset = offsets[tries];

			// create the actual hole
			IntVector centreOfIntersection = intersection.getCentre().add(offsetVec.scale(offset));
			
			if(!intersection.contains(centreOfIntersection))
				continue; // slid out of intersection

			Extent e = new Extent(centreOfIntersection.x, this.e.miny + 1,
					centreOfIntersection.z, centreOfIntersection.x,
					this.e.miny + height, centreOfIntersection.z);

			// don't allow an exit if there's a window in the way!
			Extent wideexit = e.expand(2, Extent.X | Extent.Z).expand(1,Extent.Y);
			if (this.windowIntersects(wideexit)
					|| that.windowIntersects(wideexit))
				continue;
			
			// grab an appropriate material manager
			MaterialManager mgr = new MaterialManager(e.getCentre().getBlock().getBiome());

			// create the two exit structures
			Exit src = new Exit(e, dir, this, that);
			Exit dest = new Exit(e, dir.opposite(), that, this);
			this.exits.add(src);
			this.exitMap.put(that, src); // exit 'src' leads to room
													// 'r'
			that.exits.add(dest);
			that.exitMap.put(this, dest); // exit 'dest' leads back
													// here
			// blow the hole
			Castle c = Castle.getInstance();
			c.fill(src.getExtent(), Material.AIR, 1);
			GormPlugin.log("hole blown: " + src.getExtent().toString());
			c.postProcessExit(mgr, src);
			return true;
		}
		return false;
	}

	/**
	 * actually make the room's walls and contents (the building's outer walls should already
	 * exist.) Note that roof gardens may modify the building's extent; the new building extent
	 * is returned.
	 */
	public abstract Extent build(MaterialManager mgr,Extent buildingExtent);
	
}