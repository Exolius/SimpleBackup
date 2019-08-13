package com.exolius.simplebackup.manager;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Logger;

import com.exolius.simplebackup.util.FileUtils;

public class CopyBackup extends BackupFileManager {

    public CopyBackup(final String backupFolder, final String backupPrefix, final String fileNameDateFormat, final Logger logger) {
        super(backupFolder, backupPrefix, fileNameDateFormat, logger);
    }

    @Override
    public String createBackup(final Iterable<File> worldFolders) throws IOException {
        final Date date = new Date();
        final File destination = new File(this.backupFolder, this.getFileName(date));
        for (final File worldFolder : worldFolders) {
            this.logger.info("Backing up " + worldFolder);
            FileUtils.copyFiles(worldFolder, new File(destination, worldFolder.getName()), this.logger);
        }
        return destination.getAbsolutePath();
    }

    @Override
    public void deleteBackup(final Date date) throws IOException {
        final File backupFile = new File(this.backupFolder, this.getFileName(date));
        this.logger.info("Deleting backup " + backupFile.getPath());
        this.deleteFile(backupFile);
    }

    @Override
    protected String getFileName(final Date date) {
        return this.formatDate(date);
    }

    void deleteFile(final File f) throws IOException {
        if (f.isDirectory()) {
            for (final File c : f.listFiles()) {
                this.deleteFile(c);
            }
        }
        f.delete();
    }
}
