package org.pale.gorm.rooms;

import org.pale.gorm.Building;
import org.pale.gorm.Extent;
import org.pale.gorm.Room;

/**
 * A plain, carpeted room.
 * @author white
 *
 */
public class BlankRoom extends Room {

	public BlankRoom(Extent e, Building b) {
		super(e, b);
	}

	@Override
	public Extent build(Extent buildingExtent) {
		return buildingExtent;
	}

}
