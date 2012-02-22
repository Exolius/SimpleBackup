package com.exolius.simplebackup;

import java.io.File;

import static java.lang.System.currentTimeMillis;
import static java.lang.System.out;

public class PluginUtils {

    //not implemented yet.
    public static void deleteOldBackups(int daysBack, String dirWay) {
        File directory = new File(dirWay);
        if (directory.exists()) {
            File[] listFiles = directory.listFiles();
            long purgeTime = currentTimeMillis() - (daysBack * 24L * 60L * 60L * 1000L);
            for (File listFile : listFiles) {
                if (listFile.lastModified() > purgeTime) {
                    out.println("[Simple Backup] Deleting oldest file:" + listFile);
                    if (listFile.isFile()) {
                        listFile.delete();
                    } else {
                        deleteOldBackups(daysBack, listFile.getAbsolutePath());
                    }
                }
            }
        }
    }

}
