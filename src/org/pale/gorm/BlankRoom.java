package org.pale.gorm;


import org.pale.gorm.Building;
import org.pale.gorm.Extent;
import org.pale.gorm.GormPlugin;
import org.pale.gorm.MaterialManager;
import org.pale.gorm.Room;

/**
 * A completely blank room
 * @author white
 *
 */
public class BlankRoom extends Room {

	public BlankRoom(MaterialManager mgr,Extent e, Building b) {
		super("blank",mgr, e, b);
		GormPlugin.log("Blank room: "+e.toString());
	}

	@Override
	public Extent build(MaterialManager mgr, Extent buildingExtent) {
		addSignHack();
		return null; // we don't modify the building extent
	}

	@Override
	public void furnish(MaterialManager mgr) {
	}
	


}
