package com.exolius.simplebackup;

import com.exolius.simplebackup.DateModification;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
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

	private boolean isBackingUp = false;

	/*----------------------------------------
	 This is ran when the plugin is disabled
	 -----------------------------------------*/
	public void onDisable() {
		getServer().getScheduler().cancelTasks(this);
		log.info("[SimpleBackup] Disabled SimpleBackup");
	}

	/*----------------------------------------
	 This is ran when the plugin is enabled
	 -----------------------------------------*/
	public void onEnable() {
		// Time a startup
		final long start = System.currentTimeMillis();
		new LoadConfig(this);
		long ticks = (long) (72000 * this.interval);
<<<<<<< HEAD
		// After enabling, print to console to say if it was successful
		log.info("[SimpleBackup] Enabled, developed by Exolius. Backup interval "
				+ this.interval + " hours");
		// Set executor
		getCommand("sbackup").setExecutor(new Commands(this));
=======
		log.info("[SimpleBackup] Enabled, developed by Exolius. Backup interval " + this.interval + " hours");
		getCommand("sbackup").setExecutor(new Commands(this)); // Set executor
>>>>>>> Re did more of your code
		try {
			MetricsLite metrics = new MetricsLite(this);
			metrics.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
		getServer().getScheduler().scheduleAsyncRepeatingTask(this, new Scheduler(this), ticks, ticks); // Uses bukkit's poorly made schedule system. TODO: Use a better system
		log.info("[SimpleBackup] By Exolius started in " + (System.currentTimeMillis() - start) / 1000.0D + " seconds."); 
	}

	public synchronized void addNewBackup() {
		if (!isBackingUp()) {
			new Backup(this).start();
			setBackingUp(true);
		}
	}

	public boolean isBackingUp() {
		return isBackingUp;
	}

	public void setBackingUp(boolean isBackingUp) {
		this.isBackingUp = isBackingUp;
	}

}
