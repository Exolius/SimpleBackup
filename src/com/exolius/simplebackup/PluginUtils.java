package com.exolius.simplebackup;

import java.io.File;
import java.text.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PluginUtils {

	private enum dates {
			h, d, w, m, y;
	}
	
	
    public static class DateModification {
        private int field;
        int amount;

        public DateModification(int field, int amount) {
            this.field = field;
            this.amount = amount;
        }

        public void moveForward(Calendar cal) {
            if (amount == 0) {
                throw new UnsupportedOperationException();
            }
            cal.add(field, amount);
        }

        public void moveBack(Calendar cal) {
            if (amount == 0) {
                throw new UnsupportedOperationException();
            }
            cal.add(field, -amount);
        }

        public static DateModification fromString(String firstDate) {
            if ("0".equals(firstDate)) {
                return new DateModification(Calendar.DATE, 0);
            }
            Matcher matcher = Pattern.compile("(\\d+)([hdwmyHDWMY])").matcher(firstDate);
            if (matcher.matches()) {
                String timesCount = matcher.group(1);
                String dateString = matcher.group(2);
                int count;
                try {
                    count = Integer.parseInt(timesCount);
                } catch (NumberFormatException e) {
                    return null;
                }
                int unit;
                switch (dates.valueOf(dateString)) {
                case h:
                	unit = Calendar.HOUR;
                	break;
                case d:
                	unit = Calendar.DATE;
                	break;
                case w:
                	unit = Calendar.WEEK_OF_YEAR;
                	break;
                case m:
                	unit = Calendar.MONTH;
                	break;
                case y:
                	unit = Calendar.YEAR;
                	break;
                default:
                	return null;
                }
                return new DateModification(unit, count);
                
            }
            return null;
        }
    }
    
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