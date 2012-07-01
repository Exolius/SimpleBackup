package com.exolius.simplebackup;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import com.exolius.simplebackup.DateModification;

/**
 * This is an edited config loading method!
 * This was done to take imports out of the main class!
 * Do not remove this header!
 * @author gravypod
 *
 */
public class LoadConfig {
	
	SimpleBackup plugin;
	
	LoadConfig(SimpleBackup plugin) {
		this.plugin = plugin;
		loadConfiguration();
	}
	
	/**
	 * This loads the configuration from the plugins config.yml.
	 */
	public void loadConfiguration() {
		FileConfiguration config = plugin.getConfig(); // Set the config object

		plugin.interval = config.getDouble("backup-interval-hours"); // Set default values for variables
		plugin.broadcast = config.getBoolean("broadcast-message");
		plugin.backupFile = config.getString("backup-file");
		plugin.backupWorlds = config.getStringList("backup-worlds");
		plugin.message = config.getString("backup-message");
		plugin.dateFormat = config.getString("backup-date-format");
		plugin.customMessage = config.getString("custom-backup-message");
		plugin.disableZipping = config.getBoolean("disable-zipping");
		List<String> intervalsStr = config.getStringList("delete-schedule.intervals");
		List<String> frequenciesStr = config.getStringList("delete-schedule.interval-frequencies");
		List<DateModification> intervals = new ArrayList<DateModification>();
		List<DateModification> frequencies = new ArrayList<DateModification>();
		for (int i = 0; i < intervalsStr.size(); i++) {
			String is = intervalsStr.get(i);
			DateModification interval = DateModification.fromString(is);
			if (interval == null) {
				SimpleBackup.log.info("[SimpleBackup] Can't parse interval " + is);
				if (i < frequenciesStr.size()) {
					frequenciesStr.remove(i);
				}
			} else {
				intervals.add(interval);
			}
		}
		for (String fs : frequenciesStr) {
			DateModification f = DateModification.fromString(fs);
			if (f == null) {
				SimpleBackup.log.info("[SimpleBackup] Can't parse frequency " + fs);
			}
			frequencies.add(f);
		}
		plugin.deleteScheduleIntervals = intervals;
		plugin.deleteScheduleFrequencies = frequencies;
		config.options().copyDefaults(true); // Save the configuration file
		plugin.saveConfig();
		if (plugin.backupWorlds.isEmpty()) { // If "backupWorlds" is empty then fill it with the worlds
			for (World world : plugin.getServer().getWorlds()) {
				plugin.backupWorlds.add(world.getName());
			}
		}
	}
}
