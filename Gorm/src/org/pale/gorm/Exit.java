package org.pale.gorm;

/**
 * Exits lead from a room to another. The exit is always in a wall of the source room, but there
 * may be a bridge before you get to the exit room.
 * Each exit actually has two of these structures - one owned by the source, one by the destination.
 * @author white
 *
 */
public class Exit {
	private Extent e;
	private Direction d;
	/**
	 * The exit is always colinear with a wall in the source room 
	 */
	private Building.Room source;
	/**
	 * The destination may have a bridge going to it.
	 */
	private Building.Room destination;
	
	public Exit(Extent e,Direction d,Building.Room src,Building.Room dest){
		this.e = new Extent(e);
		this.source = src;
		this.destination = dest;
		this.d = d;
	}
	
	public Exit(Exit e){
		this(e.e,e.d,e.source,e.destination);
	}
	

	public Extent getExtent() {
		return e;
	}

	public Direction getDirection() {
		return d;
	}
	
	public Building.Room getSource(){
		return source;
	}
	
	public Building.Room getDestination(){
		return destination;
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
