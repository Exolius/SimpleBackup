package com.exolius.simplebackup;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class BackupThread extends Thread {
    private final File world;
    public File file;
    private ZipOutputStream os;
    private String FullPath;
    private char pathSeparator,
            extensionSeparator;

    public BackupThread(File world)
            throws Exception {
        System.out.println("[SimpleBackup] Backing up " + world.getCanonicalPath());
        this.world = world;
        this.file =
                new File(SimpleBackup.backupFile + "/" + world.getName() + "/" +
                        SimpleBackup.format() + ".zip");
        if (!this.file.exists()) {
            this.file.getParentFile().mkdirs();
            this.file.createNewFile();
        }
        this.os = new ZipOutputStream(new DataOutputStream(new FileOutputStream(this.file)));
    }

    public void run() {
        File file = new File(this.world.getName());
        File[] subfiles = file.listFiles();
        loop(subfiles);
        try {
            this.os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        interrupt();
    }

    public void loop(File[] subfiles) {
        for (File file : subfiles)
            if (((file.isDirectory()) || file.getName().endsWith("/")) || (file.getName().endsWith("\\")))
                loop(file.listFiles());
            else
                try {
                    write(file);
                } catch (Exception e) {
                    e.printStackTrace();
                }
    }

    public String path() {
        int sep = FullPath.lastIndexOf(pathSeparator);
        return FullPath.substring(0, sep);
    }

    public void write(File input)
            throws Exception {
        String name = input.getPath();
        ZipEntry e = new ZipEntry(name.substring(name.indexOf('\\') + 1));
        this.os.putNextEntry(e);

        BufferedInputStream is = new BufferedInputStream(
                new DataInputStream(new FileInputStream(input)));
        int isb = 0;
        while ((isb = is.read()) >= 0) {
            this.os.write(isb);
        }
        is.close();

        this.os.closeEntry();
    }
}