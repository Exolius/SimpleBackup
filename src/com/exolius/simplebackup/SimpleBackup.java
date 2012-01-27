package com.exolius.simplebackup;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SimpleBackup extends JavaPlugin {

  double interval;
  boolean allWorlds;
  boolean broadcast = true;
  List<String> backupWorlds;
  final double tph = 72000.0D;

  private boolean plugins = true;
  public static String backupFile = "./backups";

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

    this.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {

    public void run() {
        doChecks();
    }
}, 60L);

    System.out.println("[SimpleBackup] Enabled. Backup interval " +
      this.interval + " hours.");
  }

  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (sender instanceof Player) {
        if(command.getName().equalsIgnoreCase("backup")){
            doChecks();
            return true;
        }
    } return false;
  }

  public void loadConfiguration()
  {
    Configuration c = getConfiguration();

    this.interval = c.getDouble("backup-interval-hours", 12.0D);
    intervalBetween = c.getInt("interval-between", intervalBetween);
    this.allWorlds = c.getBoolean("backup-all-worlds", true);
    this.broadcast = c.getBoolean("broadcast-message", true);
    this.plugins = c.getBoolean("backup-plugins", true);
    backupFile = c.getString("backup-file", "backups/");
    this.backupWorlds = c
      .getStringList("backup-worlds", new ArrayList());

    if (this.backupWorlds.size() == 0) {
      this.backupWorlds.add(((World)getServer().getWorlds().get(0)).getName());
    }
    c.setProperty("backup-worlds", this.backupWorlds);
    c.setProperty("interval-between", Integer.valueOf(intervalBetween));
    c.setProperty("backup-interval-hours", Double.valueOf(this.interval));
    c.setProperty("backup-all-worlds", Boolean.valueOf(this.allWorlds));
    c.setProperty("broadcast-message", Boolean.valueOf(this.broadcast));
    c.setProperty("backup-plugins", Boolean.valueOf(this.plugins));
    c.setProperty("backup-file", backupFile);

    c.save();
  }

  public Runnable doChecks()
  {
    return new Runnable()
    {
      public void run()
      {
        if (SimpleBackup.this.broadcast) {
          SimpleBackup.this.getServer().broadcastMessage(ChatColor.BLUE +
            "[SimpleBackup] Backup starting");
        }
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
        if (SimpleBackup.this.plugins) try { BackupThread bt = SimpleBackup.this.backupWorld(null, new File("plugins"));
            if (bt != null);
            bt.start();
            while (bt.isAlive());
          } catch (Exception e) {
            e.printStackTrace();
          }

        if (SimpleBackup.this.broadcast)
          SimpleBackup.this.getServer().broadcastMessage(ChatColor.BLUE +
            "[SimpleBackup] Backup complete");
      }
    };
  }

  public BackupThread backupWorld(World world, File file)
    throws Exception
  {
    if (world != null)
    {
      if (this.allWorlds) {
        return new BackupThread(new File(world.getName()));
      }
      if ((!this.allWorlds) && (this.backupWorlds.contains(world.getName()))) {
        return new BackupThread(new File(world.getName()));
      }

      System.out.println("[SimpleBackup] Skipping backup for " +
        world.getName());
      return null;
    }if ((world == null) && (file != null)) {
      return new BackupThread(file);
    }
    return null;
  }

  public static String format()
  {
    Date date = new Date();
    SimpleDateFormat formatter = new SimpleDateFormat("HH.mm@dd.MM.yyyy");
    return formatter.format(date);
  }
}
