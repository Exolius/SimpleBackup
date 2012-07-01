package com.exolius.simplebackup;

import java.io.File;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class PluginUtils {
	
    public static void deleteOldBackups(File backupFile, String dateFormat, List<DateModification> intervals, List<DateModification> frequencies) {
        if (intervals.isEmpty() || frequencies.isEmpty()) {
            return;
        }
        // collect files
        SortedMap<Date, File> oldFiles = collectFiles(backupFile, dateFormat);
        if (oldFiles.isEmpty()) {
            return;
        }

        // delete unwanted files in each interval
        Calendar intervalEnd = Calendar.getInstance();
        intervalEnd.setTime(oldFiles.lastKey());
        intervals.get(0).moveBack(intervalEnd);
        for (int i = 1; i <= intervals.size(); i++) {
            Calendar intervalStart = null;
            if (i < intervals.size()) {
                intervalStart = (Calendar) intervalEnd.clone();
                intervals.get(i).moveBack(intervalStart);
            }
            if (i <= frequencies.size() && frequencies.get(i - 1) != null) {
                deleteExtraBackups(filter(oldFiles, intervalStart, intervalEnd), frequencies.get(i - 1));
            }
            intervalEnd = intervalStart;
        }

    }

    private static SortedMap<Date, File> collectFiles(File backupFile, String dateFormat) {
        File[] files = backupFile.listFiles();
        if (files == null) {
            return null;
        }
        SortedMap<Date, File> oldFiles = new TreeMap<Date, File>();
        SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);
        for (File file : files) {
            Date date = formatter.parse(file.getName(), new ParsePosition(0));
            if (date != null) {
                oldFiles.put(date, file);
            }
        }
        return oldFiles;
    }

    private static SortedMap<Date, File> filter(SortedMap<Date, File> oldFiles, Calendar from, Calendar to) {
        SortedMap<Date, File> result = new TreeMap<Date, File>(oldFiles);
        Iterator<Date> it = result.keySet().iterator();
        while (it.hasNext()) {
            Date date = it.next();
            if (from != null && date.before(from.getTime()) || !date.before(to.getTime())) {
                it.remove();
            }
        }
        return result;
    }

    private static void deleteExtraBackups(SortedMap<Date, File> files, DateModification desiredFrequency) {
        if (files.isEmpty()) {
            return;
        }
        Calendar nextDate = null;
        for (Date date : files.keySet()) {
            if (desiredFrequency.amount == 0) {
                deleteBackup(files.get(date));
            } else if (nextDate == null) {
                nextDate = Calendar.getInstance();
                nextDate.setTime(date);
                desiredFrequency.moveForward(nextDate);
            } else if (!date.before(nextDate.getTime())) {
                do {
                    desiredFrequency.moveForward(nextDate);
                } while (!date.before(nextDate.getTime()));
            } else {
                deleteBackup(files.get(date));
            }
        }
    }

    private static void deleteBackup(File file) {
        System.out.println("[SimpleBackup] Deleting backup " + file.getPath());
        file.delete();
    }

}