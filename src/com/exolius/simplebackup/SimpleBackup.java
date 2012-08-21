package com.exolius.simplebackup;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.logging.Level;

public class SimpleBackup extends JavaPlugin {
    private double interval;

    private boolean broadcast = true;
    private boolean disableZipping = false;
    private boolean backupEmpty = false;
    private boolean selfPromotion = false;

    private String message = "[SimpleBackup]";
    private String dateFormat = "yyyy-MM-dd-HH-mm-ss";
    private String backupFile = "backups/";
    private String customMessage = "Backup starting";
    private String customMessageEnd = "Backup completed";

    private List<String> backupWorlds;
    private List<String> additionalFolders;
    private IBackupFileManager backupFileManager;
    private DeleteSchedule deleteSchedule;

    protected FileConfiguration config;
    private final LoginListener loginListener = new LoginListener();

    /*----------------------------------------
     This is ran when the plugin is disabled
     -----------------------------------------*/
    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);
        getLogger().info("Disabled SimpleBackup");
    }

    /*----------------------------------------
     This is ran when the plugin is enabled
     -----------------------------------------*/
    @Override
    public void onEnable() {
        // When plugin is enabled, load the "config.yml"
        loadConfiguration();

        if (!backupEmpty) {
            getServer().getPluginManager().registerEvents(loginListener, this);
        }

        // Set the backup interval, 72000.0D is 1 hour, multiplying it by the value interval will change the backup cycle time
        long ticks = (long) (72000 * this.interval);

        // Add the repeating task, set it to repeat the specified time
        this.getServer().getScheduler().scheduleAsyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                // When the task is run, start the map backup
                if (backupEmpty || getServer().getOnlinePlayers().length > 0 || loginListener.someoneWasOnline()) {
                    doBackup();
                } else {
                    getLogger().info("Skipping backup (no one was online)");
                }
            }
        }, ticks, ticks);

        //Plugin commands
        getCommand("sbackup").setExecutor(new Commands(this));

        // Shameless self promotion in the source code :D
        if (selfPromotion) {
            getLogger().info("Developed by Exolius");
        }

        // After enabling, print to console to say if it was successful
        getLogger().info("Enabled. Backup interval: " + this.interval + " hours");
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
        additionalFolders = config.getStringList("backup-folders");
        dateFormat = config.getString("backup-date-format");
        backupEmpty = config.getBoolean("backup-empty-server");
        message = config.getString("backup-message");
        customMessage = config.getString("custom-backup-message");
        customMessageEnd = config.getString("custom-backup-message-end");
        disableZipping = config.getBoolean("disable-zipping");
        selfPromotion = config.getBoolean("self-promotion");
        List<String> intervalsStr = config.getStringList("delete-schedule.intervals");
        List<String> frequenciesStr = config.getStringList("delete-schedule.interval-frequencies");

        //Save the configuration file
        config.options().copyDefaults(true);
        saveConfig();

        if (disableZipping) {
            backupFileManager = new CopyBackup(backupFile, dateFormat, getLogger());
        } else {
            backupFileManager = new ZipBackup(backupFile, dateFormat, getLogger());
        }
        this.deleteSchedule = new DeleteSchedule(intervalsStr, frequenciesStr, backupFileManager, getLogger());
        if (backupWorlds.isEmpty()) {
            // If "backupWorlds" is empty then fill it with the worlds
            for (World world : getServer().getWorlds()) {
                backupWorlds.add(world.getName());
            }
        }
        Collection<File> folders = foldersForBackup();
        Collection<World> worlds = worldsForBackup();
        if (worlds.isEmpty()) {
            getLogger().warning("Can't find any of the listed worlds " + backupWorlds);
            if (folders.isEmpty()) {
                throw new RuntimeException("Nothing to backup, no backups will be created");
            } else {
                getLogger().info("Only " + folders + " will be included in the backup");
            }
        } else {
            if (worlds.size() < backupWorlds.size()) {
                getLogger().warning("Not all listed worlds are recognized");
            }
            if (folders.size() < additionalFolders.size()) {
                getLogger().warning("Not all listed folders are recognized");
            }
            getLogger().info("Worlds " + worlds + " scheduled for backup");
            if (!folders.isEmpty()) {
                getLogger().info("Folders " + folders + " scheduled for backup");
            }
        }
    }

    /*------------------------
     This runs the map backup
     -------------------------*/
    public synchronized void doBackup() {
        // Begin backup of worlds
        // Broadcast the backup initialization if enabled
        if (broadcast) {
            getServer().broadcastMessage(ChatColor.BLUE + message + " " + customMessage);
        }
        // Loop through all the specified worlds and save them
        List<File> foldersToBackup = new ArrayList<File>();
        for (final World world : worldsForBackup()) {
            world.setAutoSave(false);
            try {
                getServer().getScheduler().callSyncMethod(this, new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        world.save();
                        return null;
                    }
                }).get();
                foldersToBackup.add(world.getWorldFolder());
            } catch (Exception e) {
                getLogger().log(Level.WARNING, e.getMessage(), e);
            }
        }
        // additional folders, e.g. "plugins/"
        foldersToBackup.addAll(foldersForBackup());

        // zip/copy world folders
        try {
            backupFileManager.createBackup(foldersToBackup);
        } catch (IOException e) {
            getLogger().log(Level.WARNING, e.getMessage(), e);
        }

        // re-enable auto-save
        for (World world : worldsForBackup()) {
            world.setAutoSave(true);
        }

        // delete old backups
        try {
            deleteSchedule.deleteOldBackups();
        } catch (IOException e) {
            getLogger().log(Level.WARNING, e.getMessage(), e);
        }

        // Broadcast the backup completion if enabled
        if (broadcast) {
            getServer().broadcastMessage(ChatColor.BLUE + message + " " + customMessageEnd);
        }
        loginListener.notifyBackupCreated();
    }

    private Collection<File> foldersForBackup() {
        List<File> result = new ArrayList<File>();
        for (String additionalFolder : additionalFolders) {
            File f = new File(".", additionalFolder);
            if (f.exists()) {
                result.add(f);
            }
        }
        return result;
    }

    private Collection<World> worldsForBackup() {
        List<World> worlds = new ArrayList<World>();
        for (World world : getServer().getWorlds()) {
            if (backupWorlds.contains(world.getName())) {
                worlds.add(world);
            }
        }
        return worlds;
    }

}
