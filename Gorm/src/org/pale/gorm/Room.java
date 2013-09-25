package org.pale.gorm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A room is a level of a building. Typically it's indoors, but the roof can also constitute a room.
 * @author white
 *
 */
public abstract class Room {
	/**
	 * The extent of the room including walls, floor and ceiling - which will overlap
	 * with adjacent rooms.
	 */
	protected Extent e;
	protected Building b;

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
	 * actually make the room's walls and contents (the building's outer walls should already
	 * exist.) Note that roof gardens may modify the building's extent; the new building extent
	 * is returned.
	 */
	public abstract Extent build(MaterialManager mgr,Extent buildingExtent);
	
	
}