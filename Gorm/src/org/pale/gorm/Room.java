package org.pale.gorm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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
	 * This is used to appropriately 'decorate' exits between outside rooms.
	 */
	private boolean isOutside;
	

	protected Room(Extent e, Building b,boolean outside) {
		this.b = b;
		this.e = new Extent(e);
		this.isOutside = outside;
		build(b.getExtent());
	}
	
	public boolean getIsOutside(){
		return isOutside;
	}
	
	/**
	 * actually make the room's walls and contents (the building's outer walls should already
	 * exist.) Note that roof gardens may modify the building's extent.
	 */
	public abstract void build(Extent buildingExtent);
	
	
}