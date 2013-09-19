package org.pale.gorm;

import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
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
	private BukkitTask task;

	@Override
	public void onDisable() {
		getLogger().info("Gorm has been disabled");
	}

	@Override
	public void onEnable() {
		getLogger().info("Gorm has been enabled");
		logger = getLogger();
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
			if (task != null) {
				task.cancel();
				task = null;
			}
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
//		IntVector pos = new IntVector(p.getLocation());
		Castle c = Castle.getInstance();
		c.setWorld(p.getWorld());
		Room r = new Room(Room.RoomType.ROOM, new Extent(0, 80, 0, 20, 90, 10));
		r.render();
		c.addRoom(r);
		Room r2 = new Room(Room.RoomType.ROOM,
				new Extent(20, 82, 0, 30, 90, 10));
		r2.render();
		c.addRoom(r2);
		Exit x = new Exit(new Extent(20, 83, 5, 20, 84, 5), Direction.WEST);
		r2.getExits().add(x);
		c.fill(x.getExtent(), Material.AIR, 1);
		c.postProcessExit(x);
	}

	private void flatten(Player p) {
		IntVector pos = new IntVector(p.getLocation());
		int size = 30;
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
			}
		}

	}

	private void gt(Player p, String str) {
		Location loc = p.getLocation();

		float f = loc.getYaw();
		Direction dir = IntVector.yawToDir(f);

		IntVector pos = new IntVector(p.getTargetBlock(null, 100).getLocation());
		GormPlugin.log("Direction : " + dir.toString() + " from yaw "
				+ Float.toString(f));
		Turtle t = new Turtle(p.getWorld(), pos, dir);
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
}
