package com.exolius.simplebackup;

import com.exolius.simplebackup.PluginUtils.DateModification;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class SimpleBackup extends JavaPlugin {
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
        System.out.println("[SimpleBackup] Disabled SimpleBackup");
    }

    /*----------------------------------------
     This is ran when the plugin is enabled
     -----------------------------------------*/
    @Override
    public void onEnable() {
        // When plugin is enabled, load the "config.yml"
        loadConfiguration();
        // Set the backup interval, 72000.0D is 1 hour, multiplying it by the value interval will change the backup cycle time
        long ticks = (long) (72000 * this.interval);

        // Add the repeating task, set it to repeat the specified time
        this.getServer().getScheduler().scheduleAsyncRepeatingTask(this, new Runnable() {

            public void run() {
                // When the task is run, start the map backup
                doBackup();
            }
        }, ticks, ticks);

        // After enabling, print to console to say if it was successful
        System.out.println("[SimpleBackup] Enabled. Backup interval " + this.interval + " hours");
        // Shameless self promotion in the source code :D
        System.out.println("[SimpleBackup] Developed by Exolius");
    }

    /*-------------------------------------------------------
     This is ran when the plugin command is sent by a player
     --------------------------------------------------------*/
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("sbackup")) {
            if (sender.isOp()) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        doBackup();
                    }
                }).start();
            }
            return true;
        }
        return false;
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
                System.out.println("[SimpleBackup] Can't parse interval " + is);
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
                System.out.println("[SimpleBackup] Can't parse frequency " + fs);
            }
            frequencies.add(f);
        }
        deleteScheduleIntervals = intervals;
        deleteScheduleFrequencies = frequencies;

        //Save the configuration file
        config.options().copyDefaults(true);
        saveConfig();

        if (backupWorlds.isEmpty()) {
            // If "backupWorlds" is empty then fill it with the worlds
            for (World world : getServer().getWorlds()) {
                backupWorlds.add(world.getName());
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
        for (World world : getServer().getWorlds()) {
            if (backupWorlds.contains(world.getName())) {
                File targetFolder = new File(backupFile, world.getName());
                world.setAutoSave(false);
                try {
                    world.save();
                    System.out.println("[SimpleBackup] Backing up " + world.getWorldFolder());
                    if (disableZipping) {
                        FileUtils.copyFiles(world.getWorldFolder(), new File(targetFolder, formatFileName()));
                    } else {
                        zipFiles(world.getWorldFolder(), new File(targetFolder, formatFileName() + ".zip"));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    world.setAutoSave(true);
                }
                PluginUtils.deleteOldBackups(targetFolder, dateFormat, deleteScheduleIntervals, deleteScheduleFrequencies);
            }
        }

        // Broadcast the backup completion if enabled
        if (broadcast) {
            getServer().broadcastMessage(ChatColor.BLUE + message + " Backup completed.");
        }
    }

    private void zipFiles(File sourceFolder, File destinationFile) throws IOException {
        if (!destinationFile.getParentFile().exists()) {
            destinationFile.getParentFile().mkdirs();
        }
        ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(destinationFile));
        try {
            zipFiles(sourceFolder.toURI(), sourceFolder, zip);
        } finally {
            try {
                zip.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void zipFiles(URI root, File source, ZipOutputStream zip) throws IOException {
        if (source.isDirectory()) {
            for (String file : source.list()) {
                zipFiles(root, new File(source, file), zip);
            }
        } else {
            ZipEntry entry = new ZipEntry(root.relativize(source.toURI()).getPath());
            zip.putNextEntry(entry);
            InputStream in = new FileInputStream(source);
            try {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) > 0) {
                    zip.write(buffer, 0, bytesRead);
                }
            } finally {
                in.close();
            }
        }
    }

    public String formatFileName() {
        //Set the naming format of the backed up file, based on the config values
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);
        return formatter.format(date);
    }

}
