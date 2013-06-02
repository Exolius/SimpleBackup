package com.exolius.simplebackup;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.SortedSet;

public interface IBackupFileManager {
    String createBackup(Iterable<File> worldFolders) throws IOException;
    SortedSet<Date> backupList();
    void deleteBackup(Date date) throws IOException;
}
