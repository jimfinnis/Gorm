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
public class Room {
	Extent e;
	Building b;

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
	 * False by default, it's set by appropriate buildings.
	 */
	public boolean isOutside = false;

	Room(Extent e, Building b) {
		this.b = b;
		this.e = new Extent(e);
	}
	
}