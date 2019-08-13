package com.exolius.simplebackup;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import com.exolius.simplebackup.manager.CopyBackup;
import com.exolius.simplebackup.manager.ZipBackup;
import com.exolius.simplebackup.util.DeleteSchedule;

public class SimpleBackup extends JavaPlugin {
    private double interval;
    private Double startHour;

    private boolean broadcast = true;
    private boolean disableZipping = false;
    private boolean backupEmpty = false;
    private boolean selfPromotion = false;

    private String message = "[SimpleBackup]";
    private String dateFormat = "yyyy-MM-dd-HH-mm-ss";
    private String backupFile = "backups/";
    private String customMessage = "Backup starting";
    private String customMessageEnd = "Backup completed";
    private String backupCommand = "";

    private List<String> backupWorlds;
    private List<String> additionalFolders;
    private IBackupFileManager backupFileManager;
    private DeleteSchedule deleteSchedule;

    protected FileConfiguration config;
    private final LoginListener loginListener = new LoginListener();
    private final BackupHooks backupHooks= new BackupHooks();

    /*----------------------------------------
     This is ran when the plugin is disabled
     -----------------------------------------*/
    @Override
    public void onDisable() {
        this.getServer().getScheduler().cancelTasks(this);
        this.getLogger().info("Disabled SimpleBackup");
    }

    /*----------------------------------------
     This is ran when the plugin is enabled
     -----------------------------------------*/
    @Override
    public void onEnable() {
        // When plugin is enabled, load the "config.yml"
        this.loadConfiguration();

        if (!this.backupEmpty) {
            this.getServer().getPluginManager().registerEvents(this.loginListener, this);
        }

        //Plugin commands
        this.getCommand("sbackup").setExecutor(new Commands(this));

        // Shameless self promotion in the source code :D
        if (this.selfPromotion) {
            this.getLogger().info("Developed by Exolius");
        }

        // Set the backup interval, 72000.0D is 1 hour, multiplying it by the value interval will change the backup cycle time
        final long ticks = (long) (72000 * this.interval);

        if (ticks > 0) {
            final long delay = this.startHour != null ? this.syncStart(this.startHour) : ticks;
            // Add the repeating task, set it to repeat the specified time
            this.getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
			    // When the task is run, start the map backup
			    if (SimpleBackup.this.backupEmpty || Bukkit.getServer().getOnlinePlayers().size() > 0 || SimpleBackup.this.loginListener.someoneWasOnline()) {
			        SimpleBackup.this.doBackup();
			    } else {
			        SimpleBackup.this.getLogger().info("Skipping backup (no one was online)");
			    }
			}, delay, ticks);
            this.getLogger().info("Backup scheduled starting in " + delay / 72000. + " hours, repeat interval: " + this.interval + " hours");
        }

        // After enabling, print to console to say if it was successful
        this.getLogger().info("Enabled.");
    }

    /*---------------------------------------------------------
     This loads the configuration from the plugins config.yml.
     ---------------------------------------------------------*/
    public void loadConfiguration() {
        // Set the config object
        this.config = this.getConfig();

        // Set default values for variables
        this.interval = this.config.getDouble("backup-interval-hours");
        this.broadcast = this.config.getBoolean("broadcast-message");
        this.backupFile = this.config.getString("backup-file");
        this.backupWorlds = this.config.getStringList("backup-worlds");
        this.additionalFolders = this.config.getStringList("backup-folders");
        this.dateFormat = this.config.getString("backup-date-format");
        this.backupEmpty = this.config.getBoolean("backup-empty-server");
        this.message = this.config.getString("backup-message");
        this.customMessage = this.config.getString("custom-backup-message");
        this.customMessageEnd = this.config.getString("custom-backup-message-end");
	this.backupCommand = this.config.getString("backup-completed-hook");
        this.disableZipping = this.config.getBoolean("disable-zipping");
        this.selfPromotion = this.config.getBoolean("self-promotion");
        final String startTime = this.config.getString("start-time");
        final List<String> intervalsStr = this.config.getStringList("delete-schedule.intervals");
        final List<String> frequenciesStr = this.config.getStringList("delete-schedule.interval-frequencies");
        final String backupPrefix = this.config.getString("backup-prefix", "");

        //Save the configuration file
        this.config.options().copyDefaults(true);
        this.saveConfig();

        if (this.disableZipping) {
            this.backupFileManager = new CopyBackup(this.backupFile, backupPrefix, this.dateFormat, this.getLogger());
        } else {
            this.backupFileManager = new ZipBackup(this.backupFile, backupPrefix, this.dateFormat, this.getLogger());
        }

        this.deleteSchedule = new DeleteSchedule(intervalsStr, frequenciesStr, this.backupFileManager, this.getLogger());
        final Collection<File> folders = this.foldersForBackup();
        final Collection<World> worlds = this.worldsForBackup();
        if (worlds.size() < this.backupWorlds.size()) {
            this.getLogger().warning("Not all listed worlds are recognized.");
        }
        if (folders.size() < this.additionalFolders.size()) {
            this.getLogger().warning("Not all listed folders are recognized.");
        }
        this.getLogger().info("Worlds " + worlds.stream().map(World::getName).collect(Collectors.toList()) + " scheduled for backup.");
        if (!folders.isEmpty()) {
            this.getLogger().info("Folders " + folders + " scheduled for backup.");
        }
        if (startTime != null) {
            try {
                final Date parsedTime = new SimpleDateFormat("HH:mm").parse(startTime);
                this.startHour = this.hoursOf(parsedTime);
            } catch (final ParseException ignored) {
                this.getLogger().warning("Can't parse time " + startTime);
            }
        }
    }

    /*------------------------
     This runs the map backup
     -------------------------*/
    public synchronized void doBackup() {
        // Begin backup of worlds
        // Broadcast the backup initialization if enabled
        if (this.broadcast) {
        	this.getServer().getScheduler().runTask(this, () -> SimpleBackup.this.getServer().broadcastMessage(ChatColor.BLUE + SimpleBackup.this.message + " " + SimpleBackup.this.customMessage));
        }
        // Loop through all the specified worlds and save them
        final List<File> foldersToBackup = new ArrayList<>();
        for (final World world : this.worldsForBackup()) {
            world.setAutoSave(false);
            try {
                this.getServer().getScheduler().callSyncMethod(this, () -> {
				    world.save();
				    return null;
				}).get();
                foldersToBackup.add(world.getWorldFolder());
            } catch (final Exception e) {
                this.getLogger().log(Level.WARNING, e.getMessage(), e);
            }
        }
        // additional folders, e.g. "plugins/"
        foldersToBackup.addAll(this.foldersForBackup());

        // zip/copy world folders
	String backupFile = null;
        try {
            backupFile = this.backupFileManager.createBackup(foldersToBackup);
        } catch (final IOException e) {
            this.getLogger().log(Level.WARNING, e.getMessage(), e);
        }

        // re-enable auto-save
        for (final World world : this.worldsForBackup()) {
            world.setAutoSave(true);
        }

        // delete old backups
        try {
            this.deleteSchedule.deleteOldBackups();
        } catch (final IOException e) {
            this.getLogger().log(Level.WARNING, e.getMessage(), e);
        }

        // Broadcast the backup completion if enabled
        if (this.broadcast) {
        	this.getServer().getScheduler().runTask(this, () -> SimpleBackup.this.getServer().broadcastMessage(ChatColor.BLUE + SimpleBackup.this.message + " " + SimpleBackup.this.customMessageEnd));
        }
	if(backupFile != null) {
	   this.loginListener.notifyBackupCreated();
	   this.backupHooks.notifyBackupCreated(this.backupCommand, backupFile);
	}
    }

    private Collection<File> foldersForBackup() {
        final List<File> result = new ArrayList<>();
        for (final String additionalFolder : this.additionalFolders) {
            final File f = new File(".", additionalFolder);
            if (f.exists()) {
                result.add(f);
            }
        }
        return result;
    }

    private Collection<World> worldsForBackup() {
        final List<World> worlds = new ArrayList<>();
        for (final World world : this.getServer().getWorlds()) {
            if (this.backupWorlds.isEmpty() || this.backupWorlds.contains(world.getName())) {
                worlds.add(world);
            }
        }
        return worlds;
    }

    private double hoursOf(final Date parsedTime) {
        final Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTime(parsedTime);
        final int hours = calendar.get(Calendar.HOUR_OF_DAY);
        final int minutes = calendar.get(Calendar.MINUTE);
        final int seconds = calendar.get(Calendar.SECOND);

        return hours + minutes / 60. + seconds / 3600.;
    }

    private long syncStart(final double startHour) {
        final double now = this.hoursOf(new Date());
        double diff = now - startHour;
        if (diff < 0) {
            diff += 24;
        }
        final double intervalPart = diff - Math.floor(diff / this.interval) * this.interval;
        final double remaining = this.interval - intervalPart;
        return (long) (remaining * 72000);
    }

}
