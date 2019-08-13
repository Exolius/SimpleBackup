package com.exolius.simplebackup;

import java.io.File;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;

public abstract class BackupFileManager implements IBackupFileManager {

    protected File backupFolder;
    protected String backupPrefix;
    protected SimpleDateFormat fileNameDateFormat;
    protected Logger logger;

    protected BackupFileManager(final String backupFolder, final String backupPrefix, final String fileNameDateFormat, final Logger logger) {
        this.backupFolder = new File(backupFolder);
        this.backupPrefix = backupPrefix;
        this.fileNameDateFormat = new SimpleDateFormat(fileNameDateFormat);
        this.logger = logger;
    }

    protected String formatDate(final Date date) {
        return this.fileNameDateFormat.format(date);
    }

    @Override
    public SortedSet<Date> backupList() {
        final File[] files = this.backupFolder.listFiles();
        if (files == null) {
            return new TreeSet<>();
        }
        final SortedSet<Date> backups = new TreeSet<>();
        for (final File file : files) {
            final Date date = this.fileNameDateFormat.parse(file.getName(), new ParsePosition(0));
            if (date != null && file.getName().equals(this.getFileName(date))) {
                backups.add(date);
            }
        }
        return backups;
    }

    protected abstract String getFileName(Date date);
}
