package org.pale.gorm;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.material.Vine;
import org.pale.gorm.Extent.LocationRunner;
import org.pale.gorm.config.BuildingDimensionConfig;
import org.pale.gorm.config.ConfigUtils;
import org.pale.gorm.config.ConfigUtils.MissingAttributeException;
import org.pale.gorm.roomutils.BoxBuilder;
import org.pale.gorm.roomutils.Gardener;
import org.pale.gorm.roomutils.RoofBuilder;
import org.pale.gorm.roomutils.WindowMaker;

/**
 * A building in the castle, consisting of rooms
 * 
 * @author white
 * 
 */
public class Building {
	public String type; //!< config type
	private Building parent;
	private ConfigurationSection c;
	

	public String getType() {
		return type;
	}


	/**
	 * Constructor - build a building from a config entry. Only generates
	 * the name and extent, doesn't do any block placement - it needs to
	 * be moved around first.
	 * @param name
	 */
	Building(Building p,String name){
		parent = p;
		type = name;
		try {
			c = Config.buildings.getConfigurationSection(name);
			if(c==null){
				throw new RuntimeException("Cannot get building config: "+name);
			}
			BuildingDimensionConfig d = new BuildingDimensionConfig(parent,parent.extent.getCentre(),c);
			IntVector v = d.getDimensions();
			setInitialExtent(parent,v.x,v.y,v.z);
		} catch(MissingAttributeException e){
			throw new RuntimeException("cannot find attribute '"+e.name+"' in building '"+type+"'");
		}
	}

	/**
	 * Constructor for a building without a parent - the first one in the system.
	 * @param p
	 * @param name
	 */
	Building(Location loc,String name){
		parent = null;
		type = name;
		try {
			c = Config.buildings.getConfigurationSection(name);
			if(c==null){
				throw new RuntimeException("Cannot get building config: "+name);
			}
			BuildingDimensionConfig d = new BuildingDimensionConfig(null,new IntVector(loc),c);
			IntVector v = d.getDimensions();
			Extent e = new Extent(new IntVector(loc),v.x,v.y,v.z);
			setInitialExtent(e);
		} catch(MissingAttributeException e){
			throw new RuntimeException("cannot find attribute '"+e.name+"' in building '"+type+"'");
		}
	}

	/**
	 * Given a parent building and a size, produce an extent for a building
	 * which can be slid around by the Builder. If there is no parent,
	 * centre the extent around the player
	 * 
	 * @param parent
	 * @param x length
	 * @param y height
	 * @param z width
	 */
	public void setInitialExtent(Building parent, int x, int y, int z) {
		Extent e;
		e = new Extent(parent.originalExtent.getCentre(), x, 1, z);
		e.miny = parent.originalExtent.miny; // make ground floors align
		setInitialExtent(e.setHeight(y));
	}


	public void setInitialExtent(Extent e){
		extent = new Extent(e);
	}

	private static int idCounter = 0;
	int id = idCounter++;

	/**
	 * If a roof has been added (NOT a roof garden - roofs are separate entities
	 * which can be overwritten) this is its height.
	 */
	protected int roofHeight = 0;

	/**
	 * List of the rooms - vertical sections within the building. DO NOT add
	 * rooms directly, use addRoom()
	 */
	public LinkedList<Room> rooms = new LinkedList<Room>();

	protected void addRoomTop(Room r) {
		rooms.addFirst(r);
		Castle.getInstance().addRoom(r);
	}

	protected void addRoomBasement(Room r) {
		rooms.addLast(r);
		Castle.getInstance().addRoom(r);
	}

	/**
	 * The extent of the entire building INCLUDING the basement
	 */
	public Extent extent;

	/**
	 * The original extent of the building before any basements etc.
	 */
	public Extent originalExtent;

	/**
	 * Return the bounding box of the building, including the walls
	 * 
	 * @return
	 */
	public Extent getExtent() {
		return extent;
	}

	/**
	 * This is used once the building is fitted into place to set the originalExtent,
	 * which records where the unmodified building is before basements etc. are added;
	 * useful for setting the floor levels of adjacent gardens etc.
	 */
	public void fixOriginalExtent(){
		originalExtent = new Extent(extent);
	}


	/**
	 * Draw into the world - call this *before* adding the building! Reads the
	 * list of build steps for this building type and executes them in order.
	 * Some may have parameters which are read from the main building config
	 * space.
	 */
	public void build(MaterialManager mgr) {
		Castle cs = Castle.getInstance();
		if(!c.isList("build")){
			throw new RuntimeException("cannot load build steps for: "+type);
		}

		try {
			for(String step: c.getStringList("build")){
				GormPlugin.log("Step: "+step);
				if(step.equalsIgnoreCase("box")){
					BoxBuilder.build(mgr, extent); // make the walls				
				}else if(step.equalsIgnoreCase("clear")){
					Extent inner = extent.expand(-1, Extent.ALL);
					cs.checkFill(inner, Material.AIR, 0); // fill the inner area
				}else if(step.equalsIgnoreCase("makerooms")){
					makeRooms(mgr); // and make the internal rooms				
				}else if(step.equalsIgnoreCase("singleroom")){
					rooms = new LinkedList<Room>(); // make sure any old ones are gone
					Room r = new BlankRoom(mgr, originalExtent, this);
					addRoomTop(r);
				}else if(step.equalsIgnoreCase("underfill")){
					double chance = c.getDouble("underfill",0.7);
					underfill(mgr, cs.r.nextDouble() < chance);
				}else if(step.equalsIgnoreCase("roof")){
					generateRoof(mgr);
				}else if(step.equalsIgnoreCase("patternfloor")){
					patternFloor(mgr,cs);
				}else if(step.equalsIgnoreCase("underfloor")){
					Extent floor = extent.getWall(Direction.DOWN);
					cs.checkFill(floor.subvec(0, 1, 0), mgr.getPrimary());
				}else if(step.equalsIgnoreCase("garden")){
					Extent floor = extent.getWall(Direction.DOWN);
					Gardener.plant(mgr,floor); // plant some things
				}else if(step.equalsIgnoreCase("floorlights")){
					floorLights(extent.expand(-1, Extent.ALL)); // light the inner region				
				}else if(step.equalsIgnoreCase("outside")){
					rooms.getFirst().setOutside();
				}else if(step.equalsIgnoreCase("allsidesopen")){
					rooms.getFirst().setAllSidesOpen();
				}else if(step.equalsIgnoreCase("farm")){
					Extent floor = extent.getWall(Direction.DOWN);
					Gardener.makeFarm(floor);				
				}else throw new RuntimeException("unknown build step '"+step+"' in building type '"+type+"'");
			}
	
			// if the building has denizens (rather than denizens defined at
			// room level) put them here.
			rooms.getFirst().makeDenizens(c);
			
		} catch(MissingAttributeException e){
			throw new RuntimeException("cannot find attribute '"+e.name+"in building '"+type+"'");
		}
	}
	
	private void patternFloor(MaterialManager mgr, Castle cs) throws MissingAttributeException{
		Extent floor = extent.getWall(Direction.DOWN).expand(-1,Extent.X|Extent.Z);
		ConfigurationSection pf = c.getConfigurationSection("patternfloor");
		if(pf==null)
			throw new RuntimeException("map 'patternfloor' not found in building: "+type);
		double chance = pf.isDouble("chance") ? pf.getDouble("chance"):0;

		if(cs.r.nextDouble()<chance){
			int n = ConfigUtils.getRandomValueInRangeInt(pf, "count");
			List<String> matnames = pf.getStringList("mats");
			if(matnames==null)throw new MissingAttributeException("mats",pf);
			MaterialDataPair mats[] = new MaterialDataPair[n];
			for(int i=0;i<n;i++){
				mats[i] = Config.makeMatDataPair(matnames.get(i));
			}
			cs.patternFill(floor, mats, n, null);
		} else {
			MaterialDataPair ground = cs.r.nextDouble()<0.2 ? mgr.getSupSecondary() : mgr.getGround();
			cs.fill(floor, ground);
		}
	}

	/**
	 * Generates a child building attached to this one
	 * 
	 * @return
	 */
	public Building createChildBuilding(Random r){
		try {
			String s = ConfigUtils.getWeightedRandom(c, "children");
			return new Building(this,s);			
		} catch (MissingAttributeException e) {
			throw new RuntimeException("missing (or wrong) children block in building: "+type);
		}
	}

	/**
	 * Attempt to build a number of internal floors in tall buildings. Floors
	 * are built in ascending order.
	 */
	protected void makeRooms(MaterialManager mgr) {
		Castle c = Castle.getInstance();

		// start placing floors until we run out
		int h = extent.miny+1;
		while(true){
			int nexth = h + c.r.nextInt(3) + 4;
			if(createRoomAt(mgr, h, nexth))
				break;

			h = nexth + 2; // because h and nexth delineate the internal space -
			// the air space - of the building
		}
	}

	/**
	 * Generate roof for a building
	 */
	protected void generateRoof(MaterialManager mgr) {
		Castle c = Castle.getInstance();
		// is the bit above the building free?
		// Calculate an extent for a putative pitched roof, the height
		// of which is half the longest edge (is that right?)
		Extent e = new Extent(extent);
		e.miny = e.maxy;
		if (e.xsize() > e.zsize())
			e = e.setHeight(e.xsize() / 2);
		else
			e = e.setHeight(e.zsize() / 2);
		if (c.intersects(e)) {
			// only building for a small roof, so skip.
			RoofBuilder.randomRoof(mgr, extent);
		} else {
			// room for a bigger roof or maybe a roof garden! Roof garden if
			// we're short.
			if (c.r.nextFloat() < 0.5 || extent.ysize() < 24) {
				buildRoofGarden(mgr, e);
			} else {
				roofHeight = RoofBuilder.randomRoof(mgr, extent);
			}
		}
	}

	/**
	 * This adds a new room on top of the existing rooms, and extends the
	 * building upwards. The new room is marked as outside. The floor is grass,
	 * and this room will intrude one block into the innerspace of the the room
	 * below for the underfloor.
	 */
	private void buildRoofGarden(MaterialManager mgr, Extent e) {
		Room r;
		if(Castle.getInstance().r.nextFloat()<0.2)
			r = new Room("rooffarm",mgr, e, this); //farm
		else
			r = new Room("roofgarden",mgr, e, this); //garden
		addRoomAndBuildExitDown(r, true);
	}

	/**
	 * Furnish rooms after building
	 */
	public void furnish(MaterialManager mgr) {
		for (Room r : rooms) {
			r.furnish(mgr);
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
	 * @param is this the last room
	 */
	private boolean createRoomAt(MaterialManager mgr, int yAboveFloor,
			int yBelowCeiling) {

		boolean rv;
		if(extent.maxy - yBelowCeiling < 5){
			yBelowCeiling = extent.maxy-2;
			rv=true;
		}
		else rv=false;

		// work out the extent of this room
		Extent roomExt = new Extent(extent);
		roomExt.miny = yAboveFloor - 1;
		roomExt.maxy = yBelowCeiling + 1;

		Room r = createRoom(mgr, roomExt);
		addRoomAndBuildExitDown(r, false);
		WindowMaker.buildWindows(mgr, r);
		return rv;
	}

	/**
	 * Get the noise determined grade value for this building, in order to
	 * determine the 'level of upkeep' of this section of the castle.
	 * 
	 * @return grade level of the current building, from 0 to 1
	 */

	public double grade() {
		IntVector centre = this.extent.getCentre();
		double grade = Noise.noise2Dfractal(centre.x, centre.z, 3, 3, 3, 0.8);
		// rebalance such that non-dungeon castles are friendlier and
		// higher-grade
		if ((GormPlugin.getInstance().getIsDungeon() == false)
				&& (grade < 0.35)) {
			grade += 0.5;
		}
		return grade;
	}

	/**
	 * Get the grade level for this building, in order to determine the 'level
	 * of upkeep' of this section of the castle.
	 * 
	 * @return grade level of the current building, from 1 to 4
	 */

	public int gradeInt() {
		double grade = this.grade();
		if (grade <= 0.35) {
			return 1;
		} else if (grade <= 0.5) {
			return 2;
		} else if (grade <= 0.65) {
			return 3;
		} else {
			return 4;
		}
	}

	/**
	 * Create a random room
	 * 
	 * @param mgr
	 * @param roomExt
	 * @return Room
	 */

	protected Room createRoom(MaterialManager mgr, Extent roomExt) {
		int grade = gradeInt();
		ConfigurationSection rc = c.getConfigurationSection("rooms");
		if(rc==null)throw new RuntimeException("no rooms section in building: "+type);
		String s;
		switch(grade){
		case 1:s="low";break;
		case 2:case 3:s="medium";break;
		default:s="high";break;
		}
		try {
			s = ConfigUtils.getWeightedRandom(rc, s);
		} catch (MissingAttributeException e) {
			throw new RuntimeException("cannot find rooms."+s+"in building '"+type+"'");
		}
		return new Room(s,mgr,roomExt,this);
	}


	/**
	 * add a new room, attempting to build an exit down from this room to the
	 * one below. Assumes the room list is ordered such new rooms added are
	 * higher up.
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
			lowerFloor.buildVerticalExitUpTo(r);
		}
	}

	/**
	 * Add a new room, attempting to build an exit up from it to the next floor.
	 */
	protected void addRoomAndBuildExitUp(Room r, boolean outside) {
		Room upperFloor = rooms.peekLast(); // any prior floor will be the last
		// item
		addRoomBasement(r); // adds to tail
		if (upperFloor != null) {
			// there is a floor below - try to build some kind of link down
			r.buildVerticalExitUpTo(upperFloor);
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
	 * Lights around the walls if we need them
	 * 
	 * @param e
	 *            the internal air space of the building
	 */
	public void lightWalls(Extent e) {
		int y = e.miny + 4;
		if (y > e.maxy)
			y = e.maxy;

		Castle c = Castle.getInstance();
		for (int x = e.minx; x <= e.maxx; x++) {
			if (Castle.requiresLight(x, y, e.minz))
				c.addLightToWall(x, y, e.minz,BlockFace.SOUTH);
			if (Castle.requiresLight(x, y, e.maxz))
				c.addLightToWall(x, y, e.maxz,BlockFace.NORTH);
		}
		for (int z = e.minz; z <= e.maxz; z++) {
			if (Castle.requiresLight(e.minx, y, z))
				c.addLightToWall(e.minx, y, z,BlockFace.EAST);
			if (Castle.requiresLight(e.maxx, y, z))
				c.addLightToWall(e.maxx, y, z,BlockFace.WEST);
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
		
		// don't bother if somehow we're on top of the castle.
		if(c.intersects(extent.getWall(Direction.DOWN)))return;

		// maybe try to build a basement down here!
		if (c.r.nextFloat()<0.7 && attemptNewRoomUnder(mgr))
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
		Extent e = extent.getWall(Direction.DOWN).expand(-1,
				Extent.X | Extent.Z);
		// decrease the floor y until we either hit a room or we're completely
		// underground

		for (;;) {
			e = e.subvec(0, 1, 0);
			if (c.intersects(e)) {
				GormPlugin.log("basement failed, castle in the way");
				return false; // no can do - there's a building down there
			}
			for (int x = e.minx; x <= e.maxx; x++) {
				// no y, we assume y=miny=maxy
				for (int z = e.minz; z <= e.maxz; z++) {
					Block b = c.getWorld().getBlockAt(x, e.miny, z);
					if (!b.getType().isSolid())
						continue;
				}
			}
			// but we need to go a bit deeper than that to make sure of
			// headroom!
			// Also put the walls back on
			e = e.subvec(0, 5, 0).expand(1, Extent.X | Extent.Z);

			// this is the total extent of the new room
			e = extent.getWall(Direction.DOWN).union(e);
			c.fill(e, mgr.getPrimary()); // make the walls
			c.fill(e.expand(-1, Extent.ALL), Material.AIR, 0); // clear the
			// contents

			// we'll succeed eventually unless the world has gone very strange
			// now we add a new room onto the bottom of the building (i.e. at
			// the start)
			Room r = new Room("basement",mgr, e, this);
			addRoomAndBuildExitUp(r, false);
			GormPlugin.log("basement done! " + e.toString());
			extent = extent.union(e); // Doesn't modify originalExtent
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
		// find a room with few exits. Or not many. Just iterate until an exit
		// has been made.

		for (Room r : rooms) {
			if (r.attemptMakeExit())
				return true;
		}
		return false;
	}

	public void ruin() {
		final Castle c = Castle.getInstance();
		if (gradeInt() > 1)
			return;

		// we also want to ruin the roof!
		Extent ruinExtent = new Extent(extent);
		ruinExtent.maxy += roofHeight;

		GormPlugin.log("RUINING " + ruinExtent.toString());

		final double chance = grade() * 1.5;
		final World w = c.getWorld();
		final Random rnd = c.r;
		final int minx = ruinExtent.minx;
		final int miny = ruinExtent.miny;
		final int minz = ruinExtent.minz;
		final double xsize = ruinExtent.xsize();
		final double ysize = ruinExtent.ysize();
		final double zsize = ruinExtent.zsize();

		// one side will be badly hit.
		Direction rd;
		final Direction ruinDir = Direction.getRandom(rnd, true);

		ruinExtent.runOnAllLocations(new LocationRunner() {
			@Override
			public void run(int x, int y, int z) {
				Block b = w.getBlockAt(x, y, z);
				if (b.getType() != Material.AIR
						&& b.getType() != Material.WATER) {
					// less chance of ruinage further down
					double yInExt = ((double) (y - miny)) / ysize;
					double heightFactor = yInExt * yInExt;

					if (y >= extent.maxy)
						heightFactor *= 2; // make roofs even worse

					// and we also work out some kind of factor across the
					// building
					double edgeFactor;
					double xInExt = ((double) (x - minx)) / xsize;
					double zInExt = ((double) (z - minz)) / zsize;
					switch (ruinDir) {
					case SOUTH:
						edgeFactor = 1.0 - (zInExt + 1.0) * 0.5;
						break;
					case NORTH:
						edgeFactor = (zInExt + 1.0) * 0.5;
						break;
					case EAST:
						edgeFactor = 1.0 - (xInExt + 1.0) * 0.5;
						break;
					case WEST:
						edgeFactor = (xInExt + 1.0) * 0.5;
						break;
					default:
						edgeFactor = 0;
					}

					if (rnd.nextDouble() < heightFactor * chance * edgeFactor
							* edgeFactor) {
						int holeh = rnd.nextInt(5) + 1;
						Extent e = new Extent(x, y, z, x, y + holeh, z)
						.intersect(extent);
						if (e != null)
							c.fill(e, Material.AIR, 0);
					} else if (rnd.nextDouble() < 0.1 && b.getType().isSolid()) {
						// if that didn't work, what about vines? The list above
						// is a list
						// of things we can slap vines onto.
						// really these should be in a random order.
						for (Direction d : Direction.values()) {
							if (d.vec.y == 0) {
								IntVector v = new IntVector(x, y, z).add(d.vec);
								Block b2 = c.getBlockAt(v);
								if (b2.getType() == Material.AIR) {
									// that'll do.
									b2.setType(Material.VINE);
									// if this is right, this is *horrible*
									BlockState bs = b2.getState();
									try {
										// loathsome code.
										Vine vine = (Vine) bs.getData();
										vine.removeFromFace(BlockFace.NORTH);
										vine.removeFromFace(BlockFace.SOUTH);
										vine.removeFromFace(BlockFace.EAST);
										vine.removeFromFace(BlockFace.WEST);
										vine.putOnFace(d.opposite().vec
												.toBlockFace());
										bs.setData(vine);
										bs.update();
									} catch(ClassCastException e){}
								}
							}
						}
					}
				}
			}
		});
	}
}
