package org.pale.gorm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;

/**
 * A room is a level of a building. Typically it's indoors, but the roof can
 * also constitute a room.
 * 
 * @author white
 * 
 */
public abstract class Room implements Comparable<Room> {

	@Override
	public int compareTo(Room that) {
		if (this == that)
			return 0;
		if (this.exits.size() < that.exits.size())
			return -1;
		if (this.exits.size() > that.exits.size())
			return 1;
		return 0;
	}

	/**
	 * The extent of the room including walls, floor and ceiling - which will
	 * overlap with adjacent rooms.
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
	 * This is used to appropriately 'decorate' exits between rooms. It marks
	 * which sides of the room are to be considered 'open'
	 */
	private Set<Direction> openSides = new HashSet<Direction>();
	public static int idCounter = 0;
	int id = idCounter++;

	int getExitCount() {
		return exits.size();
	}

	protected Room(MaterialManager mgr, Extent e, Building b) {
		this.b = b;
		this.e = new Extent(e);
		// it's possible that some rooms may modify the building extent - all
		// room build methods return either null or the new building extent
		Extent modifiedBldgExtent = build(mgr, b.getExtent());
		if (modifiedBldgExtent != null)
			b.extent = modifiedBldgExtent;
	}

	protected void setOpenSide(Direction d) {
		openSides.add(d);
	}

	public boolean isOpen(Direction d) {
		return openSides.contains(d);
	}

	public void setAllSidesOpen() {
		openSides.add(Direction.NORTH);
		openSides.add(Direction.SOUTH);
		openSides.add(Direction.EAST);
		openSides.add(Direction.WEST);
	}

	public boolean exitIntersects(Extent e) {
		for (Exit x : exits) {
			if (x.getExtent().intersects(e))
				return true;
		}
		return false;
	}

	public boolean windowIntersects(Extent e) {
		for (Extent x : windows) {
			if (x.intersects(e))
				return true;
		}
		return false;
	}

	public void addWindow(Extent e) {
		windows.add(new Extent(e));
	}

	public Extent getExtent() {
		return e;
	}

	/**
	 * try to make an exit with another room with few exits. THIS IS GOING TO BE
	 * A MAJOR BOTTLENECK. We need to narrow down the candidates.
	 * 
	 * @return
	 */
	public boolean attemptMakeExit() {
		// iterate rooms in exit count order - the caller is doing this too.
		// We keep going until we succeed, which we might not.
		// Annoyingly, because I'm using a set (and NOT doing that can result is
		// slowness during the add)
		// I have to convert it to a list before I can sort it.
		List<Room> rooms = new ArrayList<Room>(adjacentRooms());
		Collections.sort(rooms);
		for (Room that : rooms) {
			// GormPlugin.log("This: "+e.toString()+" That: "+that.e.toString());
			// don't try to connect a room to itself or another in the same
			// building
			if (that == this || that.b == this.b)
				continue;
			if (e.intersects(that.e)) {
				// if the rooms intersect, do a quick sanity check. It's OK if the Y intersect
				// depth is 1 - that just means we've build a room on top or under another. We should
				// skip in this case.
				Extent inter = e.intersect(that.e);
				if (inter.xsize() != 1 && inter.zsize() != 1 && inter.ysize()>1)
					continue;
				// only make a link if the two have similar floor heights, and
				// the zone of intersection
				// is large in XZ.
/*				GormPlugin.log(String.format("floor diff: %d, intersect: %d",
						Math.abs(this.e.miny - that.e.miny),
						Math.max(inter.xsize(), inter.zsize())));
*/
				if (Math.abs(this.e.miny - that.e.miny) < 5
						&& Math.max(inter.xsize(), inter.zsize()) > 5) {
					if (makeRandomExit(that))
						return true;

				}

			}
		}
		return false;
	}

	/**
	 * Exits 'drop down' to the destination, so the destination needs to be
	 * lower.
	 * 
	 * @param that
	 * @return
	 */
	private boolean makeRandomExit(Room that) {
		if (this.e.miny > that.e.miny)
			return makeExitBetweenRooms(that);
		else
			return that.makeExitBetweenRooms(this);

	}

	static final int[] offsets = { 0, 1, -1, 2, -2, 3, -3 };

	/**
	 * This is used to make a random horizontal exit to a room we already know
	 * intersects enough with us
	 * 
	 * @param that
	 *            destination room
	 */
	private boolean makeExitBetweenRooms(Room that) {
		Extent intersection = this.e.intersect(that.e);
		Direction dir;
/*
		GormPlugin.log(String.format(
				"attempting to create exit between %d/%d and %d/%d", this.b.id,
				this.id, that.b.id, that.id));

		if (this.exitMap.containsKey(that)) {
			GormPlugin.log(String.format("exit already exists"));
			return false;
		}
*/
		// work out the orientation of the exit
		if (intersection.xsize() > intersection.zsize()) {
			dir = that.e.minz == this.e.maxz ? Direction.SOUTH
					: Direction.NORTH;
		} else {
			dir = that.e.minx == this.e.maxx ? Direction.EAST : Direction.WEST;
		}

		// select a height at random depending on the room IDs, hoho.
		// This will give a degree of consistency for multiple exits
		int height = 2;
		//if(0==((this.id + that.id)%3))height++;

		// shrink the intersection along its longest axis, so we don't end
		// up sliding the exit into a wall to avoid a window.
		intersection = intersection.expand(-1, Extent.LONGESTXZ);

		// try to find somewhere in the intersection which doesn't collide with
		// a window or existing exit

		IntVector offsetVec = dir.vec.rotate(1); // get a perpendicular to slide
													// along
		for (int tries = 0; tries < offsets.length; tries++) {
			int offset = offsets[tries];

			// create the actual hole
			IntVector centreOfIntersection = intersection.getCentre().add(
					offsetVec.scale(offset));

			if (!intersection.contains(centreOfIntersection))
				continue; // slid out of intersection

			Extent e = new Extent(centreOfIntersection.x, this.e.miny + 1,
					centreOfIntersection.z, centreOfIntersection.x, this.e.miny
							+ height, centreOfIntersection.z);

			// don't allow an exit if there's a window in the way!
			Extent wideexit = e.expand(2, Extent.X | Extent.Z).expand(1,
					Extent.Y);
			if (this.windowIntersects(wideexit)
					|| that.windowIntersects(wideexit))
				continue;

			// the exit must not intersect any other exits in either building
			if (this.exitIntersects(wideexit) || that.exitIntersects(wideexit))
				continue;

			// grab an appropriate material manager
			MaterialManager mgr = new MaterialManager(e.getCentre().getBlock()
					.getBiome());

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
//			GormPlugin.log("hole blown: " + src.getExtent().toString());
			c.postProcessExit(mgr, src);
			return true;
		}
		return false;
	}

	/**
	 * Obtain the set of chunks which encompasses this room. Will cache the set.
	 * 
	 * @return
	 */
	public Set<Integer> getChunks() {
		if (chunks == null)
			chunks = e.expand(5, Extent.ALL).getChunks();
		return chunks;

	}

	private Set<Integer> chunks = null;

	/**
	 * Get the nearby rooms to this one - not necessarily intersecting
	 * 
	 * @return
	 */
	public Collection<Room> nearbyRooms() {
		Castle c = Castle.getInstance();
		Set<Room> out = new HashSet<Room>();
		for (int ch : getChunks()) {
			out.addAll(c.getRoomsByChunk(ch));
		}
		return out;
	}

	/**
	 * Get the rooms nearby which intersect
	 * 
	 * @return
	 */
	public Collection<Room> adjacentRooms() {
		Castle c = Castle.getInstance();
		Set<Room> out = new HashSet<Room>();
		for (int ch : getChunks()) {
			for (Room r : c.getRoomsByChunk(ch))
				if (r.e.intersects(e))
					out.add(r);
		}
		return out;
	}

	/**
	 * Hack for adding a sign to the room giving the ID. Useful
	 * for linkage/placement debugging.
	 */
	protected void addSignHack() {/*
		IntVector pos = e.getCentre();
		pos.y=e.miny+1;
		
		Block b = Castle.getInstance().getBlockAt(pos);
		b.setType(Material.SIGN_POST);
		Sign s = (Sign)b.getState();
		s.setLine(0,"Room "+Integer.toString(id));
		s.update();*/
	}
	
	/**
	 * Force updates of the modified chunk to be sent to all players.
	 */
	public void update(){
		World w = Castle.getInstance().getWorld();
		Set<Chunk> chunks = new HashSet<Chunk>();
		chunks.add(w.getChunkAt(e.minx, e.minz));
		chunks.add(w.getChunkAt(e.minx, e.maxz));
		chunks.add(w.getChunkAt(e.maxx, e.minz));
		chunks.add(w.getChunkAt(e.maxx, e.maxz));
		IntVector p = e.getCentre();
		chunks.add(w.getChunkAt(p.x,p.z));
		for(Chunk c:chunks)
			w.refreshChunk(c.getX(), c.getZ());
	}

	/**
	 * actually make the room's walls and contents (the building's outer walls
	 * should already exist.) Note that roof gardens may modify the building's
	 * extent; the new building extent is returned.
	 */
	public abstract Extent build(MaterialManager mgr, Extent buildingExtent);

}