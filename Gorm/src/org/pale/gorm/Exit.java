package org.pale.gorm;

/**
 * An exit has a direction and an extent. The direction is the direction of travel from the creating room;
 * really it's bidirectional. You can use the Direction.opposite() method to get the other direction.
 * @author white
 *
 */
public class Exit {
	private Extent e;
	private Direction d;
	
	public Exit(Extent e,Direction d){
		this.e = new Extent(e);
		this.d = d;
	}
	
	public Exit(Exit e){
		this(e.e,e.d);
	}
	

	public Extent getExtent() {
		return e;
	}

	public Direction getDirection() {
		return d;
	}

	/**
	 * Get an extent that runs 1 space out of the actual plane of the exit
	 * @return
	 */
	public Extent getExtendedExtent() {
		if(d.vec.x==0)
			return e.expand(1,Extent.Z);
		else
			return e.expand(1,Extent.X);
	}

}
