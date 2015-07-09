package org.pale.gorm;

import java.lang.Class;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public final class GormPlugin extends JavaPlugin {

	public static void log(String msg) {
		getInstance().getLogger().info(msg);
	}

	private Builder builder = null;
	private static BukkitTask task = null;

	/**
	 * Make the plugin a weird singleton.
	 */
	static GormPlugin instance = null;

	/**
	 * Use this to get plugin instances - don't play silly buggers creating new
	 * ones all over the place!
	 */
	public static GormPlugin getInstance() {
		if (instance == null)
			throw new RuntimeException(
					"Attempt to get plugin when it's not enabled");
		return instance;
	}

	@Override
	public void onDisable() {
		instance = null;
		getLogger().info("Gorm has been disabled");
	}

	public GormPlugin(){
		super();
		if(instance!=null)
			throw new RuntimeException("oi! only one instance! use GormPlugin.getInstance()");
	}

	@Override
	public void onEnable() {
		instance = this;
		//saveDefaultConfig();
		getLogger().info("Gorm has been enabled");
		ConfigUtils.load();
		getLogger().info("Brick : "+Material.getMaterial("BRICK").getId());
	}


	public boolean getIsDungeon() {
            //		return getConfig().getBoolean("dungeon");
            return true;
	}

	private boolean playerCheck(CommandSender sender) {
		if (sender instanceof Player)
			return true;
		else {
			sender.sendMessage("players only for that command");
			return false;
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command,
                  String label, String[] args) {
            String cn = command.getName();
		if (cn.equalsIgnoreCase("build")) {
			if (!playerCheck(sender))
				return false;
			Player p = (Player) sender;

			buildRandomRoom(p);
			return true;
		} else if (cn.equalsIgnoreCase("startgorm")) {
			if (!playerCheck(sender))
				return false;
			final Player p = (Player) sender;

			if (task == null) {
				BukkitRunnable r = new BukkitRunnable() {

					@Override
					public void run() {
                                            buildRandomRoom(p);
					}

				};
				task = r.runTaskTimer(this, 0, 60); // about every s
			}
			return true;
		} else if (cn.equalsIgnoreCase("stopgorm")) {
			stopGormProcess();
			return true;
		} else if (cn.equalsIgnoreCase("razegorm")) {
			Castle.getInstance().raze();
			return true;
		} else if (cn.equalsIgnoreCase("gt")) {
			if (!playerCheck(sender))
				return false;
			Player p = (Player) sender;
			gt(p, args[0]);
			return true;
		} else if (cn.equalsIgnoreCase("shb")) {
			if (!playerCheck(sender))
				return false;
			Player p = (Player) sender;
			showBlocks(p);
			return true;
		} else if(cn.equalsIgnoreCase("deni")) {
			if (!playerCheck(sender))
				return false;
			Player p = (Player) sender;
			deni(p);
		} else if (cn.equalsIgnoreCase("mke")) {
			if (!playerCheck(sender))
				return false;
			Player p = (Player) sender;
			makeExitManually(p);
			return true;
		} else if(cn.equalsIgnoreCase("gormconf")){
			ConfigUtils.load();
		} else if (cn.equalsIgnoreCase("flatten")) {
			if (!playerCheck(sender))
				return false;
			Player p = (Player) sender;
			flatten(p);
			return true;
		} else if (cn.equalsIgnoreCase("roominfo")){
			if (!playerCheck(sender))
				return false;
			Player p = (Player) sender;
			roomInfo(p);
			return true;
		}
		return false;
	}

	private void testRoom(final Room r){
		final Castle c = Castle.getInstance();
		r.getExtent().runOnAllLocations(new Extent.LocationRunner(){

			@Override
			public void run(int x, int y, int z) {
//				if(r.isBlocked(new Extent(x,y,z))){
					c.getWorld().getBlockAt(x, y, z).setType(Material.EMERALD_BLOCK);
//				}

			}});

	}

	private void showBlocks(Player p) {
		// IntVector pos = new IntVector(p.getLocation());
		Castle c = Castle.getInstance();
		c.setWorld(p.getWorld());

		IntVector pos = new IntVector(p.getLocation());

		for(Room r: c.getRooms()){
			if(r.getExtent().contains(pos))
				testRoom(r);

		}
	}


	private void roomInfo(Player p){
		IntVector pos = new IntVector(p.getLocation());
		Castle c = Castle.getInstance();
		Room r = c.getRoomAt(pos);
		if(null==r){
			p.sendMessage("no room at current location");
			return;
		}
		p.sendMessage("Room class: "+r.getClass().getSimpleName());
		p.sendMessage("Building: "+r.getBuilding().getClass().getSimpleName());
	}

	private void makeExitManually(Player p) {
		IntVector pos = new IntVector(p.getLocation());
		Castle c = Castle.getInstance();
		Room thisRoom,thatRoom=null;

		thisRoom = c.getRoomAt(pos);
		if(thisRoom==null){
			log("no room at current location");
			return;
		}
		IntVector d= IntVector.yawToDir(p.getLocation().getYaw()).vec;
		for(int i=0;i<100;i++){
			pos = pos.add(d);
			thatRoom = c.getRoomAt(pos);
			if(thatRoom!=null)break;
		}
		if(thatRoom==null){
			log("no room along current direction");
			return;
		}

		if(thisRoom.getExtent().intersects(thatRoom.getExtent())){
			thisRoom.makeExitBetweenRooms(thatRoom);
		} else
			log("rooms do not intersect");
	}

	private void deni(Player player){
		Map<Villager.Profession,Integer> deniCounts = Castle.getInstance().getDenizenCounts();
		for(Villager.Profession p: deniCounts.keySet()){
			player.sendMessage(p.toString()+": "+Integer.toString(deniCounts.get(p)));
		}
	}

	private void flatten(Player p) {
		IntVector pos = new IntVector(p.getLocation());
		int size = 50;
		Material[] m = { Material.LAPIS_BLOCK, Material.DIAMOND_BLOCK,
				Material.GOLD_BLOCK, Material.IRON_BLOCK };

		World w = p.getWorld();
		for (int dx = -size; dx <= size; dx++) {
			for (int dz = -size; dz <= size; dz++) {
				int x = dx + pos.x;
				int z = dz + pos.z;

				int y = w.getHighestBlockYAt(x, z);
				while (y >= pos.y) {
					Block b = w.getBlockAt(x, y, z);
					b.setType(Material.AIR);
					b.setData((byte) 0);
					y--;
				}

				Block b = w.getHighestBlockAt(x, z);
				int reg = getReg(x, z);
				b.setType(m[reg]);
			}
		}
	}

	private int getReg(double x, double z) {
		return (Noise.fuzzyBooleanNoise(x, z, 1, 1, 0.5, 0.1) ? 2 : 0)
				+ (Noise.fuzzyBooleanNoise(x, z, 2, 1, 0.5, 0.1) ? 1 : 0);
	}

	private void gt(Player p, String str) {
            Location loc = p.getLocation();

            float f = loc.getYaw();
            Direction dir = IntVector.yawToDir(f);
            IntVector pos = new IntVector(p.getTargetBlock((HashSet<Byte>)null, 100).getLocation());
            MaterialManager mgr = new MaterialManager(pos.getBlock().getBiome());
            Turtle t = new Turtle(mgr, p.getWorld(), pos, dir);
            t.run(str);

	}

	/**
	 * Build a random room near a given player
	 *
	 * @param p
	 *            only used to start the build process if there is no existing
	 *            room.
	 */
	private void buildRandomRoom(Player p) {
		if (builder == null) {
			builder = new Builder(p.getWorld());
		}
		builder.build(p.getLocation());
	}

	public static void stopGormProcess() {
		if (task != null) {
			task.cancel();
			task = null;
		}
	}
}
