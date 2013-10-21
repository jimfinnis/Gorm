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
import org.pale.gorm.roomutils.ExitDecorator;

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

	/**
	 * This is a list of the extents which are blocked by furniture or exits
	 * (such as steps). Add to it with addBlock() and check with isBlocked().
	 */
	Collection<Extent> blocks = new ArrayList<Extent>();

	/**
	 * Add a new extent to the list of blocked extents.
	 * 
	 * @param e
	 */
	public void addBlock(Extent e) {
		blocks.add(new Extent(e));
	}

	/**
	 * See whether an extent inside the room is blocked by furniture or
	 * something. Such extents should not have more furniture added here! We
	 * also check the extent is inside the actual room extent!
	 * 
	 * @param e
	 * @return
	 */
	public boolean isBlocked(Extent e1) {
		if (e.contains(e1)) {
			for (Extent x : blocks) {
				if (e1.intersects(x))
					return true;
			}
			return false;
		}
		return true;
	}
	
	/**
	 * See whether an position inside the room is blocked by furniture or
	 * something. Such positions should not have more furniture added here! We
	 * also check the pos is inside the actual room extent!
	 * 
	 * @param e
	 * @return
	 */
	public boolean isBlocked(IntVector pos) {
		if (e.contains(pos)) {
			for (Extent x : blocks) {
				if (x.contains(pos))
					return true;
			}
			return false;
		}
		return true;
	} 
	 


	/**
	 * Most rooms have windows - override this value to prevent their creation
	 * 
	 * @return
	 */
	public boolean hasWindows() {
		return true;
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

	/**
	 * add carpets (if this room should be carpeted) and lights to a standard
	 * room, taking into a account the state of repair of the building.
	 * 
	 * @param hasCarpets
	 * @return carpet colour code or -1 for no carpet
	 */
	public int lightsAndCarpets(boolean hasCarpets) {
		Extent inner = e.expand(-1, Extent.ALL);
		int carpetCol;
		if (b.gradeInt() > 2) {
			carpetCol = Castle.getInstance().r.nextInt(14);
			b.carpet(inner, carpetCol);
		} else
			carpetCol = -1;
		if (b.gradeInt() > 1) {
			b.lightWalls(inner);
			b.floorLights(inner);
		}
		return carpetCol;
	}

	public Extent getExtent() {
		return e;
	}

	/**
	 * try to make an exit with another room with few exits.
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
				// if the rooms intersect, do a quick sanity check. It's OK if
				// the Y intersect
				// depth is 1 - that just means we've build a room on top or
				// under another. We should
				// skip in this case.
				Extent inter = e.intersect(that.e);
				if (inter.xsize() != 1 && inter.zsize() != 1
						&& inter.ysize() > 1)
					continue;
				// only make a link if the two have similar floor heights, and
				// the zone of intersection
				// is large in XZ.
				/*
				 * GormPlugin.log(String.format("floor diff: %d, intersect: %d",
				 * Math.abs(this.e.miny - that.e.miny), Math.max(inter.xsize(),
				 * inter.zsize())));
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
		 * GormPlugin.log(String.format(
		 * "attempting to create exit between %d/%d and %d/%d", this.b.id,
		 * this.id, that.b.id, that.id));
		 * 
		 * if (this.exitMap.containsKey(that)) {
		 * GormPlugin.log(String.format("exit already exists")); return false; }
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
		// if(0==((this.id + that.id)%3))height++;

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

			// don't allow an exit if it is hits the top of either room
			if (e.maxy + 1 >= this.e.maxy || e.maxy >= that.e.maxy)
				continue;

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

			// add the space on either side of the exit to the block lists
			Extent blockExtent = e.expand(1, dir.vec.x == 0 ? Extent.Z
					: Extent.X);
			this.addBlock(blockExtent);
			that.addBlock(blockExtent);

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
			// GormPlugin.log("hole blown: " + src.getExtent().toString());

			// make stairs and add them to the blocked list if successful
			Extent stairExtent = c.dropExitStairs(mgr, src.getExtent(),
					src.getDirection());
			if (stairExtent != null) {
				addBlock(stairExtent);
			}
			// and decorate the exit
			ExitDecorator.decorate(mgr, src);

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
	 * Hack for adding a sign to the room giving the ID. Useful for
	 * linkage/placement debugging.
	 */
	protected void addSignHack() {
		/*
		 * IntVector pos = e.getCentre(); pos.y=e.miny+1;
		 * 
		 * Block blk = Castle.getInstance().getBlockAt(pos);
		 * blk.setType(Material.SIGN_POST); Sign s = (Sign)blk.getState();
		 * s.setLine(0,"Room "+Integer.toString(id));
		 * s.setLine(1,String.format("Grade %d (%.2f)",b.gradeInt(),b.grade()));
		 * s.setLine(2,
		 * getClass().getSimpleName()+"/"+b.getClass().getSimpleName());
		 * s.update();
		 */}

	/**
	 * Force updates of the modified chunk to be sent to all players. Not sure
	 * it helps.
	 */
	public void update() {
		/*
		 * World w = Castle.getInstance().getWorld(); Set<Chunk> chunks = new
		 * HashSet<Chunk>(); chunks.add(w.getChunkAt(e.minx, e.minz));
		 * chunks.add(w.getChunkAt(e.minx, e.maxz));
		 * chunks.add(w.getChunkAt(e.maxx, e.minz));
		 * chunks.add(w.getChunkAt(e.maxx, e.maxz)); IntVector p =
		 * e.getCentre(); chunks.add(w.getChunkAt(p.x,p.z)); for(Chunk c:chunks)
		 * w.refreshChunk(c.getX(), c.getZ());
		 */
	}

	/**
	 * actually make the room's walls (the building's outer walls
	 * should already exist.) Note that roof gardens may modify the building's
	 * extent; the new building extent is returned. Furniture should be added
	 * in furnish() although carpets should be added here.
	 */
	public abstract Extent build(MaterialManager mgr, Extent buildingExtent);
	
	/**
	 * Furnish a room after all exits and windows have been made - this should be
	 * the last thing done to a room
	 */
	public abstract void furnish(MaterialManager mgr);


}