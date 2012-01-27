package com.exolius.simplebackup;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SimpleBackup extends JavaPlugin {

  double interval;
  boolean broadcast = true;
  boolean plugins = false;
  public static String message = "[SimpleBackup]";
  public static String dateFormat = "yyyy-MM-DD-hh-mm-ss" ;
  final double tph = 72000.0D;
  List<String> backupWorlds;
  public static String backupFile = "backups/";
  public static int intervalBetween = 100;

  public void onDisable()
  {
    getServer().getScheduler().cancelTasks(this);
    System.out.println("[SimpleBackup] Disabled SimpleBackup");
  }

  public void onEnable()
  {
    loadConfiguration();

    double ticks = 72000.0D * this.interval;

    this.getServer().getScheduler().scheduleAsyncRepeatingTask(this, new Runnable() {

    public void run() {
        doChecks();
    }
}, 60L, (long)ticks);

    System.out.println("[SimpleBackup] Enabled. Backup interval " + this.interval + " hours");
  }

  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    return false;
  }

  public void loadConfiguration()
  {
    Configuration c = getConfiguration();

    this.interval =     c.getDouble("backup-interval-hours", 1.0D);
    intervalBetween =   c.getInt("interval-between", intervalBetween);
    this.broadcast =    c.getBoolean("broadcast-message", true);
    backupFile =        c.getString("backup-file", "backups/");
    this.backupWorlds = c.getStringList("backup-worlds", new ArrayList());
    this.message =      c.getString("backup-message",message);
    this.dateFormat =   c.getString("backup-date-format",dateFormat);

    if (this.backupWorlds.size() == 0) {
      this.backupWorlds.add(((World)getServer().getWorlds().get(0)).getName());
    }

    c.setProperty("backup-worlds", this.backupWorlds);
    c.setProperty("interval-between", Integer.valueOf(intervalBetween));
    c.setProperty("backup-interval-hours", Double.valueOf(this.interval));
    c.setProperty("broadcast-message", Boolean.valueOf(this.broadcast));
    c.setProperty("backup-file", backupFile);
    c.setProperty("backup-message", message);
    c.setProperty("backup-date-format",dateFormat);
    c.save();
  }

  public void doChecks()
  {
        SimpleBackup.this.getServer().broadcastMessage(ChatColor.BLUE + "[SimpleBackup] Backup starting");
        for (World world : SimpleBackup.this.getServer().getWorlds()) {
          try
          {
            world.save();
            world.setAutoSave(false);

            BackupThread bt = SimpleBackup.this.backupWorld(world, null);
            if (bt != null) {
              bt.start();
              while (bt.isAlive());
              world.setAutoSave(true);
            }
          } catch (Exception e) {
            e.printStackTrace();
          }
        }

        if (SimpleBackup.this.broadcast)
          SimpleBackup.this.getServer().broadcastMessage(ChatColor.BLUE + "[SimpleBackup] Backup complete");
  }

  public BackupThread backupWorld(World world, File file)
    throws Exception
  {
    if (world != null)
    {
      if ((this.backupWorlds.contains(world.getName()))) {
        return new BackupThread(new File(world.getName()));
      }

      //System.out.println("[SimpleBackup] Skipping backup for " + world.getName());
      return null;
    }if ((world == null) && (file != null)) {
      return new BackupThread(file);
    }
    return null;
  }

  public static String format()
  {
    Date date = new Date();
    SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);
    return formatter.format(date);
  }
}
