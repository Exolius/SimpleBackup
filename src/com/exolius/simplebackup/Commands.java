package com.exolius.simplebackup;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class Commands implements CommandExecutor {
    private SimpleBackup plugin;

    public Commands(SimpleBackup plugin) {
        this.plugin = plugin;
    }

    /*-------------------------------------------------------
    This is ran when the plugin command is sent by a player
    --------------------------------------------------------*/
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("sbackup")) {
            if (sender.hasPermission("simplebackup.use")) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        plugin.doBackup();
                    }
                }).start();
            }
            return true;
        }
        return false;
    }
}