package com.exolius.simplebackup;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class Commands implements CommandExecutor {

    private final SimpleBackup plugin;

    public Commands(final SimpleBackup plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command cmd, final String label, final String[] args) {
        if (sender.hasPermission("simplebackup.use")) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Commands.this.plugin.doBackup();
                }
            }).start();
            return true;
        } else {
        	return false;
        }
    }
}
