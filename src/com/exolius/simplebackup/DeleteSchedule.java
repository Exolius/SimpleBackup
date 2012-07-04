package com.exolius.simplebackup;

import java.io.File;
import java.io.IOException;
import java.text.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeleteSchedule {

    private List<DateModification> intervals;
    private List<DateModification> frequencies;
    private IBackupFileManager backupFileManager;

    public DeleteSchedule(List<String> intervalsStr, List<String> frequenciesStr, IBackupFileManager backupFileManager, Logger logger) {
        List<DateModification> intervals = new ArrayList<DateModification>();
        List<DateModification> frequencies = new ArrayList<DateModification>();
        for (int i = 0; i < intervalsStr.size(); i++) {
            String is = intervalsStr.get(i);
            DateModification interval = DateModification.fromString(is);
            if (interval == null) {
                logger.warning("Can't parse interval " + is);
                if (i < frequenciesStr.size()) {
                    frequenciesStr.remove(i);
                }
            } else {
                intervals.add(interval);
            }
        }
        for (String fs : frequenciesStr) {
            DateModification f = DateModification.fromString(fs);
            if (f == null) {
                logger.warning("Can't parse frequency " + fs);
            }
            frequencies.add(f);
        }
        this.intervals = intervals;
        this.frequencies = frequencies;
        this.backupFileManager = backupFileManager;
    }

    public static class DateModification {
        private int field;
        private int amount;

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

        public static DateModification fromString(String s) {
            if ("0".equals(s)) {
                return new DateModification(Calendar.DATE, 0);
            }
            Matcher matcher = Pattern.compile("(\\d+)([hdwmyHDWMY])").matcher(s);
            if (matcher.matches()) {
                String countStr = matcher.group(1);
                String unitStr = matcher.group(2);
                int count;
                try {
                    count = Integer.parseInt(countStr);
                } catch (NumberFormatException ignored) {
                    return null;
                }
                int unit;
                if ("h".equalsIgnoreCase(unitStr)) {
                    unit = Calendar.HOUR;
                } else if ("d".equalsIgnoreCase(unitStr)) {
                    unit = Calendar.DATE;
                } else if ("w".equalsIgnoreCase(unitStr)) {
                    unit = Calendar.WEEK_OF_YEAR;
                } else if ("m".equalsIgnoreCase(unitStr)) {
                    unit = Calendar.MONTH;
                } else if ("y".equalsIgnoreCase(unitStr)) {
                    unit = Calendar.YEAR;
                } else {
                    return null;
                }
                return new DateModification(unit, count);
                
            }
            return null;
        }
    }
    
    public void deleteOldBackups() throws IOException {
        if (intervals.isEmpty() || frequencies.isEmpty()) {
            return;
        }
        // collect files
        SortedSet<Date> oldFiles = backupFileManager.backupList();
        if (oldFiles.isEmpty()) {
            return;
        }

        // delete unwanted files in each interval
        Calendar intervalEnd = Calendar.getInstance();
        intervalEnd.setTime(oldFiles.last());
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

    private SortedSet<Date> filter(SortedSet<Date> oldFiles, Calendar from, Calendar to) {
        SortedSet<Date> result = new TreeSet<Date>(oldFiles);
        Iterator<Date> it = result.iterator();
        while (it.hasNext()) {
            Date date = it.next();
            if (from != null && date.before(from.getTime()) || !date.before(to.getTime())) {
                it.remove();
            }
        }
        return result;
    }

    private void deleteExtraBackups(SortedSet<Date> files, DateModification desiredFrequency) throws IOException {
        if (files.isEmpty()) {
            return;
        }
        Calendar nextDate = null;
        for (Date date : files) {
            if (desiredFrequency.amount == 0) {
                backupFileManager.deleteBackup(date);
            } else if (nextDate == null) {
                nextDate = Calendar.getInstance();
                nextDate.setTime(date);
                desiredFrequency.moveForward(nextDate);
            } else if (!date.before(nextDate.getTime())) {
                do {
                    desiredFrequency.moveForward(nextDate);
                } while (!date.before(nextDate.getTime()));
            } else {
                backupFileManager.deleteBackup(date);
            }
        }
    }

}
