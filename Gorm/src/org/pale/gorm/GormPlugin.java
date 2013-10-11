package org.pale.gorm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public final class GormPlugin extends JavaPlugin {

	public static void log(String msg) {
		logger.info(msg);
	}

	private static Logger logger;
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
		loadConfiguration();
		getLogger().info("Gorm has been enabled");

		logger = getLogger();
		logger.info("configtest: " + Boolean.toString(getIsDungeon()));
	}

	/**
	 * Set up configuration file if one does not exist
	 */
	public void loadConfiguration() {
		this.getConfig().addDefault("dungeon", true);
		ArrayList<Integer> lootGrade1 = new ArrayList<Integer>(Arrays.asList(
				256, 257, 258, 261, 264, 265, 266, 267, 276, 277, 278, 279,
				282, 283, 284, 285, 286, 297, 302, 303, 304, 305, 306, 307,
				308, 309, 310, 311, 312, 313, 314, 315, 316, 317, 320, 322,
				329, 345, 347, 354, 364, 366, 368, 369, 370, 372, 378, 379,
				380, 381, 384, 386, 388, 395, 417, 418, 419, 420, 421, 2256,
				2257, 2258, 2259, 2260, 2261, 2262, 2263, 2264, 2265, 2266,
				2267));
		this.getConfig().addDefault("loot.grade1", lootGrade1);
		ArrayList<Integer> lootGrade2 = new ArrayList<Integer>(Arrays.asList(
				261, 265, 267, 272, 273, 274, 275, 282, 289, 291, 292, 297,
				298, 299, 300, 301, 302, 303, 304, 305, 306, 307, 308, 309,
				329, 357, 368, 386, 391, 392, 395, 400, 420, 421, 2256, 2257,
				2258, 2259, 2260, 2261, 2262, 2263, 2264, 2265, 2266, 2267));
		this.getConfig().addDefault("loot.grade2", lootGrade2);
		ArrayList<Integer> lootGrade3 = new ArrayList<Integer>(Arrays.asList(
				268, 269, 270, 271, 281, 282, 290, 297, 298, 299, 300, 301,
				302, 303, 304, 305, 334, 357, 360, 365, 395, 2256, 2257, 2258,
				2259, 2260, 2261, 2262, 2263, 2264, 2265, 2266, 2267));
		this.getConfig().addDefault("loot.grade3", lootGrade3);
		ArrayList<Integer> lootGrade4 = new ArrayList<Integer>(Arrays.asList(
				265, 268, 269, 270, 271, 272, 273, 274, 275, 281, 282, 291,
				297, 298, 299, 300, 301, 334, 357, 360, 365, 395, 2256, 2257,
				2258, 2259, 2260, 2261, 2262, 2263, 2264, 2265, 2266, 2267));
		this.getConfig().addDefault("loot.grade4", lootGrade4);
		this.getConfig().options().copyDefaults(true);
		this.saveConfig();
	}

	public boolean getIsDungeon() {
		return getConfig().getBoolean("dungeon");
	}

	public ArrayList<Integer> getLoot(int grade) {
		switch (grade) {
		case 1:
			return (ArrayList<Integer>) getConfig().get("loot.grade1");
		case 2:
			return (ArrayList<Integer>) getConfig().get("loot.grade2");
		case 3:
			return (ArrayList<Integer>) getConfig().get("loot.grade3");
		default:
			return (ArrayList<Integer>) getConfig().get("loot.grade4");
		}
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
		if (command.getName().equalsIgnoreCase("build")) {
			if (!playerCheck(sender))
				return false;
			Player p = (Player) sender;

			buildRandomRoom(p);
			return true;
		} else if (command.getName().equalsIgnoreCase("startgorm")) {
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
		} else if (command.getName().equalsIgnoreCase("stopgorm")) {
			stopGormProcess();
			return true;
		} else if (command.getName().equalsIgnoreCase("razegorm")) {
			Castle.getInstance().raze();
			return true;
		} else if (command.getName().equalsIgnoreCase("gt")) {
			if (!playerCheck(sender))
				return false;
			Player p = (Player) sender;
			gt(p, args[0]);
			return true;
		} else if (command.getName().equalsIgnoreCase("test")) {
			if (!playerCheck(sender))
				return false;
			Player p = (Player) sender;
			test(p);
			return true;
		} else if (command.getName().equalsIgnoreCase("flatten")) {
			if (!playerCheck(sender))
				return false;
			Player p = (Player) sender;
			flatten(p);
			return true;
		}
		return false;
	}

	private void test(Player p) {
		// IntVector pos = new IntVector(p.getLocation());
		Castle c = Castle.getInstance();
		c.setWorld(p.getWorld());

		Extent e = new Extent(p.getLocation());

		for (Room q : Castle.getInstance().getRooms())
			GormPlugin.log("   loop: room " + q.id + " has " + q.exits.size());
		GormPlugin.log("Done!");
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

		IntVector pos = new IntVector(p.getTargetBlock(null, 100).getLocation());
		GormPlugin.log("Direction : " + dir.toString() + " from yaw "
				+ Float.toString(f));
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
