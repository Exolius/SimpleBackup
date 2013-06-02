package com.exolius.simplebackup;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Logger;

public class CopyBackup extends BackupFileManager {

    public CopyBackup(String backupFolder, String fileNameDateFormat, Logger logger) {
        super(backupFolder, fileNameDateFormat, logger);
    }

    @Override
    public String createBackup(Iterable<File> worldFolders) throws IOException {
        Date date = new Date();
        File destination = new File(backupFolder, getFileName(date));
        for (File worldFolder : worldFolders) {
            logger.info("Backing up " + worldFolder);
            FileUtils.copyFiles(worldFolder, new File(destination, worldFolder.getName()), logger);
        }
        return destination.getAbsolutePath();
    }

    @Override
    public void deleteBackup(Date date) throws IOException {
        File backupFile = new File(backupFolder, getFileName(date));
        logger.info("Deleting backup " + backupFile.getPath());
        deleteFile(backupFile);
    }

    @Override
    protected String getFileName(Date date) {
        return formatDate(date);
    }

    void deleteFile(File f) throws IOException {
        if (f.isDirectory()) {
            for (File c : f.listFiles()) {
                deleteFile(c);
            }
        }
        f.delete();
    }
}
