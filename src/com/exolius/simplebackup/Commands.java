package com.exolius.simplebackup;

import org.bukkit.ChatColor;
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
        if (sender.hasPermission("simplebackup.use")) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    plugin.doBackup();
                }
            }).start();
        } else {
        	sender.sendMessage(ChatColor.RED + "You don't have permission to execute this command.");
        }
        
        return true;
    }
}