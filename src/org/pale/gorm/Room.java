package org.pale.gorm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Villager;
import org.bukkit.material.Ladder;
import org.pale.gorm.Extent.LocationRunner;
import org.pale.gorm.roomutils.ExitDecorator;
import org.pale.gorm.roomutils.Furniture;
import org.pale.gorm.roomutils.FurnitureItems;
import org.pale.gorm.roomutils.StairBuilder;

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

	public Building getBuilding(){
		return b;
	}


	/**
	 * the room is indoors; this is true by default - call setOutside to change
	 * it.
	 */
	private boolean indoors = true;

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

	public void spawnVillager(Villager.Profession p) {
		Location l = e.getCentre().toLocation();
		Villager v = Castle.getInstance().getWorld().spawn(l, Villager.class);
		v.setProfession(p);
		Castle.getInstance().incDenizenCount(p);
	}

	protected void spawnIronGolem() {
		Location l = e.getCentre().toLocation();
		Castle.getInstance().getWorld().spawn(l, IronGolem.class);
	}

	/**
	 * Make this room the room below a gallery. This needs to be done before the
	 * room above is made, and before furniture in this room is planted, so that
	 * the furniture is not overwritten by the columns.
	 */

	public void makeBelowGallery(MaterialManager mgr) {
		int minDim = Math.min(e.xsize(), e.zsize());
		int gallerySize = minDim / 4;
		if (gallerySize > 8)
			gallerySize = 8;

		if (gallerySize < 4)
			return; // gallery won't be big enough in this room

		// work out where to place the columns, and do so
		galleryColumnExtent = e.expand(-1, Extent.X | Extent.Z)
				.getWall(Direction.DOWN)
				.expand(-gallerySize, Extent.X | Extent.Z).addvec(0, 1, 0);

		galleryColumnExtent.maxy = e.maxy;
		Furniture.columns(this, galleryColumnExtent, mgr);

	}

	protected Room(MaterialManager mgr, Extent e, Building b) {
		this.b = b;
		this.e = new Extent(e);
		// it's possible that some rooms may modify the building extent - all
		// room build methods return either null or the new building extent
		Extent modifiedBldgExtent = build(mgr, b.getExtent());
		if (modifiedBldgExtent != null)
			b.setExtent(modifiedBldgExtent);

		// maybe make this room into a gallery base
		Castle c = Castle.getInstance();
		if (c.r.nextFloat() < 0.7 && canBeBelowGallery())
			makeBelowGallery(mgr);
		// and if the room below is a gallery base, blow the necessary hole
		// and fence it.
		// this room won't have been added, so the last room added - that at
		// the head of the list - is the one we're interested in.
		if (!b.rooms.isEmpty() && canHaveHoleInFloor()) {
			// make the fence
			Room below = b.rooms.getFirst();

			if (below.galleryColumnExtent != null) {
				World w = c.getWorld();
				Extent fence = new Extent(below.galleryColumnExtent).setY(e.miny + 1);
				
				// ugly code, mainly to deal with simpler overwriting logic - the 
				// fence must not overwrite anything already there.
				
				MaterialDataPair fenceMat = mgr.getFence();
				for(int x=fence.minx;x<=fence.maxx;x++){
					Block k = w.getBlockAt(x,fence.miny,fence.minz);
					if(k.isEmpty()){
						k.setType(fenceMat.m);
						k.setData((byte) fenceMat.d);
					}
					k = w.getBlockAt(x,fence.miny,fence.maxz);
					if(k.isEmpty()){
						k.setType(fenceMat.m);
						k.setData((byte) fenceMat.d);
					}							
				}
				for(int z=fence.minz;z<=fence.maxz;z++){
					Block k = w.getBlockAt(fence.minx,fence.miny,z);
					if(k.isEmpty()){
						k.setType(fenceMat.m);
						k.setData((byte) fenceMat.d);
					}
					k = w.getBlockAt(fence.maxx,fence.miny,z);
					if(k.isEmpty()){
						k.setType(fenceMat.m);
						k.setData((byte) fenceMat.d);
					}							
				}

				// make the hole
				/// (and also part of the underfloor)
				Extent hole = fence.setY(e.miny-1).expand(-1,
						Extent.X | Extent.Z).setHeight(4);
				GormPlugin.log("Room extent:  " + e.toString());
				GormPlugin.log("Fence extent: " + fence.toString());
				GormPlugin.log("Hole extent:  " + hole.toString());
				c.fill(hole, new MaterialDataPair(Material.AIR, 0));
				// it would be stupid to make furniture in the hole or fence.
				addBlock(hole.expand(1, Extent.X | Extent.Z));
			}

		}

	}

	public boolean canHaveHoleInFloor() {
		// all rooms can have a hole in the floor, it seems.
		return true;
	}

	public boolean canBeBelowGallery() {
		// by default, only indoors rooms can be a "below gallery", and
		// so filled with columns.
		// Other classes might override this,
		return indoors;
	}

	protected Room setOpenSide(Direction d) {
		openSides.add(d);
		return this;
	}

	public boolean isOpen(Direction d) {
		return openSides.contains(d);
	}

	public Room setAllSidesOpen() {
		openSides.add(Direction.NORTH);
		openSides.add(Direction.SOUTH);
		openSides.add(Direction.EAST);
		openSides.add(Direction.WEST);
		return this;
	}

	public Room setOutside() {
		indoors = false;
		return this;
	}

	public boolean isIndoors() {
		return indoors;
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
	 * @param mayHaveCarpets
	 *            *may* have carpets, depending on grade
	 * @return carpet colour code or -1 for no carpet
	 */
	public int lightsAndCarpets(boolean mayHaveCarpets) {
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

	/**
	 * Add generic unique items, such as a possible spawner in low-grade rooms
	 *
	 * @param mgr
	 */
	public void furnishUniques(MaterialManager mgr) {
		if (b.grade() < 0.1) // we ONLY place really scary things if the whole
			// building is lowgrade
			Furniture.place(mgr, this,
					FurnitureItems.random(FurnitureItems.uniqueScaryChoices));
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
			// don't permit connection to a room we're already connected to
			if (isConnected(that))
				continue;
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
				//
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

	public boolean isConnected(Room that) {
		return exitMap.containsKey(that);
	}

	static final int[] offsets = { 0, 1, -1, 2, -2, 3, -3 };

	/**
	 * This is used to make a random horizontal exit to a room we already know
	 * intersects enough with us. Public for debugging.
	 *
	 * @param that
	 *            destination room, whose floor is LOWER than us.
	 */
	public boolean makeExitBetweenRooms(Room that) {
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

			if (!intersection.contains(centreOfIntersection)) {
				continue; // slid out of intersection
			}

			Extent e = new Extent(centreOfIntersection.x, this.e.miny + 1,
					centreOfIntersection.z, centreOfIntersection.x, this.e.miny
					+ height, centreOfIntersection.z);

			// don't allow an exit if it is hits the top of either room
			if (e.maxy + 1 >= this.e.maxy || e.maxy >= that.e.maxy) {
				continue;
			}

			Extent wideexit = e.expand(2, Extent.X | Extent.Z).expand(1,
					Extent.Y);

			// don't allow an exit if there's a window in the way!
			/*			if (this.windowIntersects(wideexit)
					|| that.windowIntersects(wideexit)) {
				continue;
			}
			 */
			// the exit must not intersect any other exits in either building
			if (this.exitIntersects(wideexit) || that.exitIntersects(wideexit)) {
				continue;
			}

			// grab an appropriate material manager
			MaterialManager mgr = new MaterialManager(e.getCentre().getBlock()
					.getBiome());

			// add the space on either side of the exit to the block lists -
			// we actually build two extents, one protruding into the other
			// room,
			// one into this. We can't just add an exit extent across both
			// rooms,
			// because isBlocked would always trigger on that because it is
			// partially outside
			// the room!
			Extent blockExtent = e.expand(1, dir.vec.x == 0 ? Extent.Z
					: Extent.X);
			// make sure it doesn't clash with existing blocks
			if (this.isBlocked(blockExtent.intersect(this.e))
					|| that.isBlocked(blockExtent.intersect(that.e))) {
				continue;
			}

			// create the two exit structures
			Exit src = new Exit(e, dir, this, that);
			Exit dest = new Exit(e, dir.opposite(), that, this);

			Castle c = Castle.getInstance();

			// make stairs and add them to the blocked list if successful
			StairBuilder sb = new StairBuilder(mgr);

			Extent stairExtent = sb.dropExitStairs(src.getExtent(),
					src.getDirection(), that, 5);

			// if the stairs didn't get made because of a blockage, don't create
			// any exit data at all.
			if (sb.isStairsBlocked()) {
				continue;
			}

			// don't allow anything to subsequently be made in the stairs
			if (stairExtent != null)
				addBlock(stairExtent);

			// add the exits to the blocks
			this.addBlock(blockExtent);
			that.addBlock(blockExtent);

			// add the exit structures to the rooms
			this.exits.add(src);
			this.exitMap.put(that, src); // exit 'src' leads to room
			// 'r'
			that.exits.add(dest);
			that.exitMap.put(this, dest); // exit 'dest' leads back
			// here
			// blow the hole
			c.fill(src.getExtent(), Material.AIR, 1);
			// GormPlugin.log("hole blown: " + src.getExtent().toString());

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
	 * If this is not null, this room is marked as the area below a gallery and
	 * a hole should be blown in the room above. The extent itself is the extent
	 * containing the columns of the gallery, so it's 1 bigger than the hole and
	 * in the room below.
	 */
	private Extent galleryColumnExtent = null;

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
		 * IntVector pos = e.getCentre(); pos.y = e.miny + 1;
		 *
		 * Block blk = Castle.getInstance().getBlockAt(pos);
		 * blk.setType(Material.SIGN_POST); Sign s = (Sign) blk.getState();
		 * s.setLine(0, "Room " + Integer.toString(id)); s.setLine(1,
		 * String.format("Grade %d (%.2f)", b.gradeInt(), b.grade()));
		 * s.setLine(2, getClass().getSimpleName() + "/" +
		 * b.getClass().getSimpleName()); s.update();
		 */
		}

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
	 * Build an exit up to another room; sometimes stairs, sometimes a ladder.
	 *
	 * @param upper
	 */
	public void buildVerticalExitUpTo(Room upper) {
		// sometimes, try to build stairs. If it fails after a certain
		// number of tries, give up and go with the ladder.
		if (Castle.getInstance().r.nextFloat() < 0.7) {
			for (int tries = 0; tries < 10; tries++) {
				if (buildStairsUpTo(upper))
					return;
			}
		}
		buildCornerLadderUpTo(upper);
	}

	private boolean buildStairsUpTo(Room upper) {
		Castle c = Castle.getInstance();
		Random r = c.r;

		// firstly, find the length of the stairs. We need to have at least
		// 1 clearance at each end.
		int steps = upper.e.miny - e.miny;
		boolean stepsAlongXAxis;

		// select an alignment. Why -4 in these tests? Because we're not only
		// taking the 1 block clearance at the end of each staircase into
		// account,
		// but also the fact that steps are delimited by the inner extent, and
		// we're
		// working directly with the outer extent.
		if (upper.e.xsize() - 4 <= steps) {
			// X isn't valid; is Z?
			if (upper.e.zsize() - 4 <= steps)
				return false; // no room
			// only Z is valid
			stepsAlongXAxis = false;
		} else {
			// X is valid
			if (upper.e.zsize() - 4 <= steps)
				stepsAlongXAxis = true; // only X is valid
			else
				stepsAlongXAxis = r.nextBoolean(); // either
		}

		// now we have to find a position to build down from, and place the
		// stairs

		Extent ex;
		if (stepsAlongXAxis) {
			// 4 here to account for both using only the inner extent and
			// also the clearance at the end
			int startx = r.nextInt(upper.e.xsize() - (steps + 4));
			// and get a z in the inner extent somewhere.
			int z;
			z = r.nextBoolean() ? upper.e.minz + 1 : upper.e.maxz - 1;
			// int z = r.nextInt(upper.e.zsize()-2)+upper.e.minz+1;

			startx += upper.e.minx + 2;
			ex = new Extent(startx, e.miny + 1, z, startx + steps,
					upper.e.miny, z);
			if (isBlocked(ex))
				return false;

		} else {
			int startz = r.nextInt(upper.e.zsize() - (steps + 4));
			startz += upper.e.minz + 2;
			// and get an x in the inner extent somewhere.
			int x;
			x = r.nextBoolean() ? upper.e.minx + 1 : upper.e.maxx - 1;
			// int x = r.nextInt(upper.e.xsize()-2)+upper.e.minx+1;
			ex = new Extent(x, e.miny + 1, startz, x, upper.e.miny, startz
					+ steps);
			if (isBlocked(ex))
				return false;
		}

		MaterialManager mgr = new MaterialManager(e.getCentre().getBlock()
				.getBiome());
		StairBuilder sb = new StairBuilder(mgr);

		// now we need to convert the extent of the stairs
		// into the extent of an "exit" at the top of the stairs.

		// First, pick an end and a direction. Move the end so that it's just
		// above the block before the stairs start down.

		IntVector base;
		Direction d;
		if (stepsAlongXAxis) {
			if (r.nextBoolean()) {
				d = Direction.EAST;
				base = ex.getCorner(Extent.X);
			} else {
				d = Direction.WEST;
				base = ex.getCorner(0);
			}
		} else {
			if (r.nextBoolean()) {
				d = Direction.SOUTH;
				base = ex.getCorner(Extent.Z);
			} else {
				d = Direction.NORTH;
				base = ex.getCorner(0);
			}
		}

		// clear a hole big enough to provide headroom
		Extent ex2 = new Extent(base, 0, 0, 0);
		ex2.maxy += 2; // deal with carpet carpet etc.
		ex2 = ex2.union(base.add(d.vec.scale(3)));
		c.fill(ex2, Material.AIR, 0);

		// we have the corner inside the stairs extent, now move out and up one.
		base = base.subtract(d.vec).add(0, 1, 0);
		// turn this into an "exit extent"
		ex = new Extent(base, 0, 0, 0).setHeight(2);

		// c.fill(ex, Material.LAPIS_BLOCK,0);
		sb.heightCheckSubtract = 5; // so we don't end up stopping too early
		Extent result = sb.dropExitStairs(ex, d, this, 30);// allow pretty much
		// any length of
		// flight

		if (result == null) {
			// shouldn't happen, but just in case
			c.fill(ex2, Material.STONE, 0); // might look a bit weird..
			return false;
		} else {
			// add blocks - the stair extent in the lower room,
			// and the stair "hole" in the upper.
			addBlock(result);
			ex2.miny = upper.e.miny;
			ex2.maxy = upper.e.maxy;
			upper.addBlock(ex2);
			return true;
		}
	}

	/**
	 * Builds a ladder from this room up to another room, in the corner
	 *
	 * @param upper
	 */
	private void buildCornerLadderUpTo(Room upper) {
		World w = Castle.getInstance().getWorld();

		// get the inner extent of the lower room
		Extent innerLower = e.expand(-1, Extent.ALL);

		// get one corner of that room (but go up one so we don't overwrite the
		// carpet)
		IntVector ladderPos = new IntVector(innerLower.minx,
				innerLower.miny + 1, innerLower.minz);

		Ladder ladder = new Ladder();
		ladder.setFacingDirection(BlockFace.NORTH);

		// fill an area of floor around the ladder hole (deals with roof garden
		// farms!)
		// First make an extent where the ladder hole is
		Extent e = new Extent(ladderPos.x, upper.e.miny, ladderPos.z);
		// then pull it towards the centre of the room by one position in X and
		// Z,
		// to give a 2x2 hole
		IntVector cc = upper.e.getCentre();
		if (e.minx < cc.x)
			e.maxx++;
		else
			e.minx--;
		if (e.minz < cc.z)
			e.maxz++;
		else
			e.minz--;

		MaterialManager mgr = new MaterialManager(b.getExtent().getCentre()
				.getBlock().getBiome()); // *sigh*
		Castle.getInstance().fill(e, mgr.getPrimary());

		// build up, placing a ladder until we get to the floor above, and go
		// one square into the room
		// to clear the carpet too.
		for (int y = ladderPos.y; y <= upper.e.miny + 1; y++) {
			Block b = w.getBlockAt(ladderPos.x, y, ladderPos.z);
			// BlockState s = b.getState();
			// s.setData((MaterialData)ladder);
			// s.update();
			b.setType(Material.LADDER);
			b.setData(ladder.getData());
		}

		// create an extent a bit wider
		e = new Extent(ladderPos.x, ladderPos.y - 1, ladderPos.z).expand(1,
				Extent.ALL).setHeight(innerLower.ysize());
		// and block that off in the lower room, so we don't block the ladder
		addBlock(e);
		// now block off the same area in the room above
		e.miny = upper.e.miny + 1;
		e.maxy = upper.e.maxy - 1;
		upper.addBlock(e);
	}

	/**
	 * actually make the room's walls (the building's outer walls should already
	 * exist.) Note that roof gardens may modify the building's extent; the new
	 * building extent is returned. Furniture should be added in furnish()
	 * although carpets should be added here.
	 */
	public abstract Extent build(MaterialManager mgr, Extent buildingExtent);

	/**
	 * Furnish a room after all exits and windows have been made - this should
	 * be the last thing done to a room
	 */
	public abstract void furnish(MaterialManager mgr);

	/**
	 * Remove windows intersecting with an extent
	 *
	 * @param wallExtent
	 */
	public void removeWindows(Extent wallExtent) {
		Iterator<Extent> i = windows.iterator();
		while (i.hasNext()) {
			Extent e = i.next();
			if (e.intersects(wallExtent))
				i.remove();
		}
	}

	public boolean tallNeighbourRequired() {
		return false;
	}

}
