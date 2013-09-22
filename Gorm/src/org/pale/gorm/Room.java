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
	protected Extent e;
	protected Building b;

	/**
	 * This is a map of exits by the room they go to
	 */
	Map<Room, Exit> exitMap = new HashMap<Room, Exit>();

	/**
	 * Our exits
	 */
	Collection<Exit> exits = new ArrayList<Exit>();
	
	/**
	 * This is used to appropriately 'decorate' exits between rooms.
	 * It marks which sides of the room are to be considered 'open'
	 */
	private Set<Direction> openSides = new HashSet<Direction>();
	

	protected Room(Extent e, Building b) {
		this.b = b;
		this.e = new Extent(e);
		b.extent = build(b.getExtent());
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
	
	/**
	 * actually make the room's walls and contents (the building's outer walls should already
	 * exist.) Note that roof gardens may modify the building's extent; the new building extent
	 * is returned.
	 */
	public abstract Extent build(Extent buildingExtent);
	
	
}