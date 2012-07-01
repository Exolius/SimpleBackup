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
    public boolean selfPromotion = false;

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
        
        new LoadConfig(this);// When plugin is enabled, load the "config.yml"
        
        long ticks = (long) (72000 * this.interval); // Set the backup interval, 72000.0D is 1 hour, multiplying it by the value interval will change the backup cycle time

        
        log.info("[SimpleBackup] Enabled. Backup interval: " + this.interval + " hours"); // After enabling, print to console to say if it was successful

        
        getServer().getScheduler().scheduleAsyncRepeatingTask(this, new Scheduler(this), ticks, ticks); // Add the repeating task, set it to repeat the specified time

        // Metrics Start
		try {
			MetricsLite metrics = new MetricsLite(this);
			metrics.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// Metrics End
        
        getCommand("sbackup").setExecutor(new Commands(this)); //Plugin commands

        
        if (selfPromotion) {
        	log.info("[SimpleBackup] Developed by Exolius"); // Shameless self promotion in the source code :D
        }
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
