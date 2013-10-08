package org.pale.gorm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import javax.xml.crypto.dsig.CanonicalizationMethod;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Ladder;
import org.bukkit.material.MaterialData;
import org.pale.gorm.rooms.BlankRoom;
import org.pale.gorm.rooms.ChestRoom;
import org.pale.gorm.rooms.SpawnerRoom;
import org.pale.gorm.rooms.PlainRoom;
import org.pale.gorm.roomutils.WindowMaker;

/**
 * A building in the castle, consisting of rooms
 * 
 * @author white
 * 
 */
public abstract class Building {
	/**
	 * Given a parent building and a size, produce an extent for a building
	 * which can be slid around by the Builder.
	 * 
	 * @param parent
	 * @param e
	 */
	public void setInitialExtent(Building parent, int x, int y, int z) {
		Extent e = new Extent(parent.getExtent().getCentre(), x, 1, z); // y
																		// unused
		e.miny = parent.getExtent().miny; // make floors align
		extent = e.setHeight(y);

	}

	private static int idCounter = 0;
	int id = idCounter++;

	/**
	 * List of the rooms - vertical sections within the building. DO NOT add
	 * rooms directly, use addRoom()
	 */
	public LinkedList<Room> rooms = new LinkedList<Room>();

	protected void addRoomTop(Room r) {
		rooms.addFirst(r);
		Castle.getInstance().addRoom(r);
	}
	
	protected void addRoomBasement(Room r){
		rooms.addLast(r);
		Castle.getInstance().addRoom(r);		
	}

	/**
	 * The extent of the entire building
	 */
	public Extent extent;

	/**
	 * How much of the top of the building is 'roof'
	 */
	int roofDepth = 1;

	/**
	 * Return the bounding box of the building, including the walls
	 * 
	 * @return
	 */
	public Extent getExtent() {
		return extent;
	}

	/**
	 * The standard operation for single-room buildings
	 * 
	 */
	public Room makeSingleRoom(MaterialManager mgr) {
		rooms = new LinkedList<Room>(); // make sure any old ones are gone
		Room r = new BlankRoom(mgr, extent, this);
		addRoomTop(r);
		return r;
	}

	/**
	 * Fill this in - it's what actually makes the building
	 */
	public abstract void build(MaterialManager mgr);

	/**
	 * Attempt to build a number of internal floors in tall buildings. Floors
	 * are built in ascending order.
	 */
	protected void makeRooms(MaterialManager mgr) {
		Castle c = Castle.getInstance();

		// start placing floors until we run out
		for (int h = extent.miny + 1; h < extent.maxy - 4;) {
			int nexth = h + c.r.nextInt(3) + 4;
			placeFloorAt(mgr, h, nexth);

			h = nexth + 2; // because h and nexth delineate the internal space -
							// the air space - of the building
		}
	}

	/**
	 * Place a floor at height h above the building base. Floors are one deep,
	 * and are built in ascending order (i.e. the start of the 'rooms' list will
	 * have the lower floor in it, if there is one, since they are added to the
	 * head)
	 * 
	 * @param yAboveFloor
	 *            Y of the block just above the floor
	 * @param yBelowCeiling
	 *            Y of the block just below the ceiling
	 */
	private void placeFloorAt(MaterialManager mgr, int yAboveFloor,
			int yBelowCeiling) {

		// work out the extent of this room
		Extent roomExt = new Extent(extent);
		roomExt.miny = yAboveFloor - 1;
		roomExt.maxy = yBelowCeiling + 1;

		Room r = chooseRoom(mgr, roomExt, this);
		addRoomAndBuildExitDown(r, false);
		WindowMaker.buildWindows(mgr, r);
	}

	// Allows Multiple room types per tall building
	private Room chooseRoom(MaterialManager mgr, Extent roomExt, Building bld) {
		Random rnd = new Random();
		GormPlugin plugin = new GormPlugin();
		if (plugin.getIsDungeon()) {
			switch (rnd.nextInt(10)) {
			case 0:
				return new ChestRoom(mgr, roomExt, bld);
			case 1:
			case 2:
				return new SpawnerRoom(mgr, roomExt, bld);
			default:
				return new PlainRoom(mgr, roomExt, bld);
			}
		} else {
			return new PlainRoom(mgr, roomExt, bld);
		}
	}

	/**
	 * add a new room, attempting to build an exit down from this
	 * room to the one below. Assumes the room list is ordered such new rooms
	 * added are higher up.
	 * 
	 * @param newRoomExtent
	 * @param outside
	 *            is the room outside or inside
	 */
	protected void addRoomAndBuildExitDown(Room r, boolean outside) {
		Room lowerFloor = rooms.peekFirst(); // any prior floor will be the
		// first item
		addRoomTop(r); // adds to head
		if (lowerFloor != null) {
			// there is a floor below - try to build some kind of link down
			buildVerticalExit(lowerFloor, r);
		}
	}
	
	/**
	 * Add a new room, attempting to build an exit up from it to the next floor.
	 */
	protected void addRoomAndBuildExitUp(Room r, boolean outside){
		Room upperFloor = rooms.peekLast(); // any prior floor will be the last item
		addRoomBasement(r); // adds to head
		if (upperFloor != null) {
			// there is a floor below - try to build some kind of link down
			buildVerticalExit(r,upperFloor);
		}
		
	}

	/**
	 * Build a vertical exit between two rooms
	 * 
	 * @param lower
	 *            lower room
	 * @param upper
	 *            upper room
	 */
	private void buildVerticalExit(Room lower, Room upper) {
		World w = Castle.getInstance().getWorld();

		// get the inner extent of the lower room
		Extent innerLower = lower.e.expand(-1, Extent.ALL);

		// get one corner of that room (but go up one so we don't overwrite the
		// carpet)
		IntVector ladderPos = new IntVector(innerLower.minx,
				innerLower.miny + 1, innerLower.minz);

		Ladder ladder = new Ladder();
		ladder.setFacingDirection(BlockFace.NORTH);

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
	}

	/**
	 * Add carpeting to this extent - this is the interior space of the
	 * building; the air space.
	 * 
	 * @param floor
	 */
	public void carpet(Extent floor, int col) {
		Castle c = Castle.getInstance();
		floor = new Extent(floor);
		floor.maxy = floor.miny;
		c.fill(floor, Material.CARPET, col);
	}

	/**
	 * Block any glass or ironfence with primary, for darkness
	 * 
	 * @param room
	 */
	public void blockWindows(Extent room) {
		Castle c = Castle.getInstance();
		room = new Extent(room);
		MaterialManager mgr = new MaterialManager(room.getCentre().getBlock()
				.getBiome());
		c.replace(room, mgr.getPrimary(), mgr.getWindow());
		c.replace(room, mgr.getPrimary(), new MaterialDataPair(
				Material.IRON_FENCE, 0));
	}

	/**
	 * Lights around the walls if we need them
	 * 
	 * @param e
	 *            the internal air space of the building
	 */
	public void lightWalls(Extent e) {

		int y = e.miny + 4;
		if (y > e.maxy)
			y = e.maxy;
		for (int x = e.minx; x <= e.maxx; x++) {
			if (Castle.requiresLight(x, y, e.minz))
				addLight(x, y, e.minz);
			if (Castle.requiresLight(x, y, e.maxz))
				addLight(x, y, e.maxz);
		}
		for (int z = e.minz; z <= e.maxz; z++) {
			if (Castle.requiresLight(e.minx, y, z))
				addLight(e.minx, y, z);
			if (Castle.requiresLight(e.maxx, y, z))
				addLight(e.maxx, y, z);
		}
	}

	/**
	 * Lights on the floor if needed
	 * 
	 * @param e
	 */
	public void floorLights(Extent e) {
		World w = Castle.getInstance().getWorld();
		int y = e.miny;

		for (int x = e.minx + 2; x < e.maxx - 2; x += 5) {
			for (int z = e.minz + 2; z < e.maxz - 2; z += 5) {
				Block b = w.getBlockAt(x, y, z);
				if (b.getLightLevel() < 7) {
					b.setData((byte) 0);
					b.setType(Material.FENCE);
					b = w.getBlockAt(x, y + 1, z);
					b.setData((byte) 0);
					b.setType(Material.FENCE);
					b = w.getBlockAt(x, y + 2, z);
					b.setData((byte) 0);
					b.setType(Material.TORCH);
				}
			}
		}
	}

	/**
	 * Fill voids under the building in some way. If complete is true, make the
	 * block solid.
	 * 
	 * @param mgr
	 */
	protected void underfill(MaterialManager mgr, boolean complete) {
		Castle c = Castle.getInstance();
		World w = c.getWorld();
		int dx, dz;

		if (attemptNewRoomUnder(mgr))
			return;

		MaterialDataPair mat = mgr.getSecondary();

		if (complete) {
			dx = 1;
			dz = 1;
		} else {
			dx = extent.xsize() <= 8 ? extent.xsize() - 1
					: calcUnderfill(extent.xsize() - 1);
			dz = extent.zsize() <= 8 ? extent.zsize() - 1
					: calcUnderfill(extent.zsize() - 1);
		}

		for (int x = extent.minx; x <= extent.maxx; x += dx) {
			for (int z = extent.minz; z <= extent.maxz; z += dz) {
				for (int y = extent.miny - 1;; y--) {
					Block b = w.getBlockAt(x, y, z);
					if (!b.getType().isSolid() && Castle.canOverwrite(b)) { // also
																			// avoid
																			// "unwritable air"
						b.setType(mat.m);
						b.setData((byte) mat.d);
					} else {
						break;
					}
				}
			}
		}

	}

	/**
	 * Rather than underfilling, try to make a basement room. This won't work if
	 * there is another building in the way.
	 * 
	 * @param mgr
	 * @return
	 */
	private boolean attemptNewRoomUnder(MaterialManager mgr) {
		
		GormPlugin.log("attempting basement");
		Castle c = Castle.getInstance();
		// get the floor, without the walls
		Extent e = extent.getWall(Direction.DOWN).expand(-1,Extent.X|Extent.Z); 
		// decrease the floor y until we either hit a room or we're completely
		// underground

		for (;;) {
			e = e.subvec(0, 1, 0);
			if (c.intersects(e)){
				GormPlugin.log("basement failed, castle in the way");
				return false; // no can do - there's a building down there
			}
			for (int x = e.minx; x <= e.maxx; x++) {
				// no y, we assume y=miny=maxy
				for (int z = e.minz; z <= e.maxz; z++) {
					Block b = c.getWorld().getBlockAt(x,e.miny,z);
					if(!b.getType().isSolid())
						continue;
				}
			}
			// but we need to go a bit deeper than that to make sure of headroom!
			// Also put the walls back on
			e=e.subvec(0,5,0).expand(1,Extent.X|Extent.Z);
			
			// this is the total extent of the new room
			e = extent.getWall(Direction.DOWN).union(e);
			c.fill(e,mgr.getPrimary()); // make the walls
			c.fill(e.expand(-1,Extent.ALL),Material.AIR,0); // clear the contents
			
			
			// we'll succeed eventually unless the world has gone very strange
			// now we add a new room onto the bottom of the building (i.e. at the start)
			Room r = new PlainRoom(mgr, e,this);
			addRoomAndBuildExitUp(r, false);
			GormPlugin.log("basement done! "+e.toString());
			extent = extent.union(e);
			return true;
		}
	}

	int calcUnderfill(int size) {
		int dPillar = 4;
		int count = 2;
		int finalCount = 0;
		while (count < 4) {
			if ((count * (Math.ceil(size / count))) == (size)) {
				finalCount = count;
			}
			count += 1;
		}
		if (finalCount > 0) {
			dPillar = (int) (Math.ceil(size / finalCount));
		}
		return dPillar;
	}

	void addLight(int x, int y, int z) {
		Block b = Castle.getInstance().getWorld().getBlockAt(x, y, z);
		b.setData((byte) 0);
		b.setType(Material.TORCH);
	}

	public boolean contains(IntVector v) {
		return extent.contains(v);
	}

	boolean intersects(Extent e) {
		return extent.intersects(e);
	}

	public void setExtent(Extent e) {
		extent = new Extent(e);
	}

	/**
	 * Force update packet sending
	 */
	public void update() {
		for (Room r : rooms) {
			r.update();
		}
	}

	/**
	 * Tell this building to make a random exit to an adjacent building - this
	 * is for horizontal exits
	 */
	public boolean makeRandomExit() {
		Castle c = Castle.getInstance();

		// find a room with few exits. Or not many. Just iterate until an exit
		// has been made.

		for (Room r : rooms) {
			if (r.attemptMakeExit())
				return true;
		}
		return false;
	}

}
