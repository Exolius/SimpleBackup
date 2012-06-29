package com.exolius.simplebackup;

import com.exolius.simplebackup.PluginUtils.DateModification;

import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class SimpleBackup extends JavaPlugin {
	static Logger log = Logger.getLogger("Minecraft");

	double interval;

	public boolean broadcast = true;
	public boolean disableZipping = false;

	public String message = "[SimpleBackup]";
	public String dateFormat = "yyyy-MM-dd-HH-mm-ss";
	public String backupFile = "backups/";
	public String customMessage = "Backup starting";

	List<String> backupWorlds;
	List<DateModification> deleteScheduleIntervals;
	List<DateModification> deleteScheduleFrequencies;

	protected FileConfiguration config;

	/*----------------------------------------
	 This is ran when the plugin is disabled
	 -----------------------------------------*/
	@Override
	public void onDisable() {
		getServer().getScheduler().cancelTasks(this);
		log.info("[SimpleBackup] Disabled SimpleBackup");
	}

	/*----------------------------------------
	 This is ran when the plugin is enabled
	 -----------------------------------------*/
	@Override
	public void onEnable() {
		//Time a startup
		long start = System.currentTimeMillis();
		// When plugin is enabled, load the "config.yml"
		loadConfiguration();
		// Set the backup interval, 72000.0D is 1 hour, multiplying it by the
		// value interval will change the backup cycle time
		long ticks = (long) (72000 * this.interval);
		// After enabling, print to console to say if it was successful
		log.info("[SimpleBackup] Enabled, developed by Exolius. Backup interval " + this.interval + " hours");
		// Set executor
		getCommand("sbackup").setExecutor(new Commands(this));
		/** Starts a thread pool so we can add tasks to run! */
		new ThreadPool();
		try {
			MetricsLite metrics = new MetricsLite(this);
		    metrics.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// Add the repeating task, set it to repeat the specified time
		this.getServer().getScheduler().scheduleAsyncRepeatingTask(this, new Runnable() {
			public void run() {
				// When the task is run, start the map backup
				addNewBackup();
			}
		}, ticks, ticks);
		log.info("[SimpleBackup] By Exolius started in " + (System.currentTimeMillis() - start) / 1000.0D + " seconds.");
	}

	/*---------------------------------------------------------
	 This loads the configuration from the plugins config.yml.
	 ---------------------------------------------------------*/
	public void loadConfiguration() {
		// Set the config object
		config = getConfig();

		// Set default values for variables
		interval = config.getDouble("backup-interval-hours");
		broadcast = config.getBoolean("broadcast-message");
		backupFile = config.getString("backup-file");
		backupWorlds = config.getStringList("backup-worlds");
		message = config.getString("backup-message");
		dateFormat = config.getString("backup-date-format");
		customMessage = config.getString("custom-backup-message");
		disableZipping = config.getBoolean("disable-zipping");
		List<String> intervalsStr = config.getStringList("delete-schedule.intervals");
		List<String> frequenciesStr = config.getStringList("delete-schedule.interval-frequencies");
		List<DateModification> intervals = new ArrayList<DateModification>();
		List<DateModification> frequencies = new ArrayList<DateModification>();
		for (int i = 0; i < intervalsStr.size(); i++) {
			String is = intervalsStr.get(i);
			DateModification interval = DateModification.fromString(is);
			if (interval == null) {
				log.info("[SimpleBackup] Can't parse interval " + is);
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
				log.info("[SimpleBackup] Can't parse frequency " + fs);
			}
			frequencies.add(f);
		}
		deleteScheduleIntervals = intervals;
		deleteScheduleFrequencies = frequencies;

		// Save the configuration file
		config.options().copyDefaults(true);
		saveConfig();
		// If "backupWorlds" is empty then fill it with the worlds
		if (backupWorlds.isEmpty()) {
			for (World world : getServer().getWorlds()) {
				backupWorlds.add(world.getName());
			}
		}
	}

	public synchronized void addNewBackup() {
		ThreadPool.addTask(new Backup(this));
	}
}
