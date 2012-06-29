package com.exolius.simplebackup;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * Commands file
 * @author gravypod
 *
 */
public class Commands implements CommandExecutor {
	SimpleBackup plugin;

	/**
	 * Start up the CommandExecutor for SimpleBackup!
	 * 
	 * @param plugin
	 */
	Commands(SimpleBackup plugin) {
		this.plugin = plugin;
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label,
			String[] args) {
		if (cmd.getName().equalsIgnoreCase("sbackup")) {
			if (sender.isOp()) {
				ThreadPool.addTask(new Backup(plugin));
			}
			return true;
		}
		return false;
	}
}
