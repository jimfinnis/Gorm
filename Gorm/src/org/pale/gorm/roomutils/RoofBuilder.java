package org.pale.gorm.roomutils;

import org.pale.gorm.Extent;
import org.pale.gorm.MaterialManager;

/**
 * An interface for things which build roofs. I've not made this a static class, as I've done with
 * things like WindowMaker and ExitDecorator, because roof builders could get quite complicated.
 * @author white
 *
 */
public interface RoofBuilder {
	void buildRoof(MaterialManager mgr,Extent roomExtent);
}
