package com.exolius.simplebackup.manager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipBackup extends BackupFileManager {

    public ZipBackup(final String backupFolder, final String backupPrefix, final String fileNameDateFormat, final Logger logger) {
        super(backupFolder, backupPrefix, fileNameDateFormat, logger);
    }

    @Override
    public String createBackup(final Iterable<File> worldFolders) throws IOException {
        if (!this.backupFolder.exists()) {
            this.backupFolder.mkdirs();
        }
        final Date date = new Date();
        final File backupFile = new File(this.backupFolder, this.getFileName(date));
        final ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(backupFile));
        try {
            for (final File worldFolder : worldFolders) {
                this.logger.info("Backing up " + worldFolder);
                this.zipFiles(worldFolder.getParentFile().toURI(), worldFolder, zip);
            }
        } finally {
            try {
                zip.close();
            } catch (final IOException e) {
                this.logger.log(Level.FINE, e.getMessage(), e);
            }
        }
        return backupFile.getAbsolutePath();
    }

    @Override
    public void deleteBackup(final Date date) {
        final File backupFile = new File(this.backupFolder, this.getFileName(date));
        this.logger.info("Deleting backup " + backupFile.getPath());
        backupFile.delete();
    }

    @Override
    protected String getFileName(final Date date) {
        return this.backupPrefix + this.formatDate(date) + ".zip";
    }

    private void zipFiles(final URI root, final File source, final ZipOutputStream zip) throws IOException {
        if (source.isDirectory()) {
            for (final String file : source.list()) {
                this.zipFiles(root, new File(source, file), zip);
            }
        } else {
            final ZipEntry entry = new ZipEntry(root.relativize(source.toURI()).getPath());
            zip.putNextEntry(entry);
            InputStream in = null;
            try {
                in = new FileInputStream(source);
                final byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) > 0) {
                    zip.write(buffer, 0, bytesRead);
                }
            } catch (final IOException e) {
                this.logger.warning("Unable to backup file: " + source.getAbsolutePath() + "(" + e.getMessage() + ")");
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        }
    }
}
