package com.exolius.simplebackup;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.bukkit.ChatColor;
import org.bukkit.World;

public class Backup implements Runnable {

	SimpleBackup plugin;

	Backup(SimpleBackup plugin) {
		this.plugin = plugin;
	}

	@Override
	public void run() {
		// Begin backup of worlds
		// Broadcast the backup initialization if enabled
		if (plugin.broadcast) {
			plugin.getServer().broadcastMessage(ChatColor.BLUE + plugin.message + " " + plugin.customMessage);
		}
		// Loop through all the specified worlds and save them
		for (World world : plugin.getServer().getWorlds()) {
			if (plugin.backupWorlds.contains(world.getName())) {
				File targetFolder = new File(plugin.backupFile, world.getName());
				world.setAutoSave(false);
				try {
					world.save();
					SimpleBackup.log.info("[SimpleBackup] Backing up " + world.getWorldFolder());
					if (plugin.disableZipping) {
						FileUtils.copyFiles(world.getWorldFolder(), new File(targetFolder, this.formatFileName()));
					} else {
						ZipFile.zipFiles(world.getWorldFolder(), new File(targetFolder, this.formatFileName() + ".zip"));
					}
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					world.setAutoSave(true);
				}
				PluginUtils.deleteOldBackups(targetFolder, plugin.dateFormat, plugin.deleteScheduleIntervals, plugin.deleteScheduleFrequencies);
			}
		}
		// Broadcast the backup completion if enabled
		if (plugin.broadcast) {
			plugin.getServer().broadcastMessage(
					ChatColor.BLUE + plugin.message + " Backup completed.");
		}
	}

	public synchronized String formatFileName() {
		// Set the naming format of the backed up file, based on the config
		// values
		Date date = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat(plugin.dateFormat);
		return formatter.format(date);
	}
}
