package com.exolius.simplebackup;

import org.bukkit.event.*;
import org.bukkit.event.player.PlayerQuitEvent;

public class LoginListener implements Listener {
    private boolean wasOnline = true;

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        wasOnline = true;
    }

    public boolean someoneWasOnline() {
        return wasOnline;
    }
    public void notifyBackupCreated() {
        wasOnline = false;
    }
}
