package org.pale.gorm;

import java.util.logging.Logger;

import org.bukkit.Location;
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
		} else if (command.getName().equalsIgnoreCase("test")) {
			if (!playerCheck(sender))
				return false;
			Player p = (Player) sender;
			test(p,args[0]);
			return true;
		}

		return false;
	}

	private void test(Player p, String str) {
		Location loc = p.getLocation();
		
		if(Castle.getInstance().getWorld()==null){
			p.sendMessage("no castle yet!");return;
		}
		
		float f = loc.getYaw();
		if(f<0)f+=360;
		IntVector.Direction dir;;
		if (f > 45 && f <= 135) dir = IntVector.Direction.WEST;
		else if (f > 135 && f <= 225) dir = IntVector.Direction.SOUTH;
		else if (f > 225 && f <= 315) dir = IntVector.Direction.EAST;
		else dir = IntVector.Direction.NORTH;
		IntVector pos = new IntVector(p.getTargetBlock(null,100).getLocation());
		GormPlugin.log("Direction : "+dir.toString()+" from yaw "+Float.toString(f));
		Turtle t = new Turtle(Castle.getInstance(),pos,dir);
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
