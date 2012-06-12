package com.exolius.simplebackup;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class SimpleBackup extends JavaPlugin {
    double interval;

    public boolean broadcast = true;
    public boolean disableZipping = false;
    public boolean deleteOldMaps = false;

    public String message = "[SimpleBackup]";
    public String dateFormat = "yyyy-MM-dd-HH-mm-ss";
    public String backupFile = "backups/";
    public String customMessage = "Backup starting";

    List<String> backupWorlds;

    protected FileConfiguration config;

    public int daysToDelete = 2;

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
                doBackup();
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
        interval = config.getDouble("backup-interval-hours", 1.0D);
        broadcast = config.getBoolean("broadcast-message", true);
        backupFile = config.getString("backup-file", backupFile);
        backupWorlds = config.getStringList("backup-worlds");
        message = config.getString("backup-message", message);
        dateFormat = config.getString("backup-date-format", dateFormat);
        customMessage = config.getString("custom-backup-message", customMessage);
        disableZipping = config.getBoolean("disable-zipping", disableZipping);

        //These haven't been implemented yet
        //deleteOldMaps = config.getBoolean("delete-old-maps", deleteOldMaps);
        //daysToDelete = config.getInt("days-to-delete", daysToDelete);

        // Generate the default config.yml
        config.set("backup-worlds", backupWorlds);
        config.set("backup-interval-hours", Double.valueOf(interval));
        config.set("broadcast-message", Boolean.valueOf(broadcast));
        config.set("backup-file", backupFile);
        config.set("backup-message", message);
        config.set("backup-date-format", dateFormat);
        config.set("custom-backup-message", customMessage);
        config.set("disable-zipping", disableZipping);

        //These haven't been implemented yet
        //config.set("delete-old-maps", deleteOldMaps);
        //config.set("days-to-delete", daysToDelete);

        //Save the configuration file
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
    public void doBackup() {
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
            }
        }

        // Broadcast the backup completion if enabled
        if (broadcast) {
            getServer().broadcastMessage(ChatColor.BLUE + message + " Backup completed.");
        }

        // Old map file deletion goes here
    }

    private void zipFiles(File sourceFolder, File destinationFile) throws IOException {
        if (destinationFile.getParentFile().exists()) {
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

    //Prints debugging information, only used for development and is never ran
    //with a plugin release.
    public void printDebug() {
        // Debugging code, prints loaded variables to console
        // To see if the loading works after modification
        System.out.println(backupWorlds);
        System.out.println(interval);
        System.out.println(broadcast);
        System.out.println(backupFile);
        System.out.println(message);
        System.out.println(dateFormat);
    }

}
