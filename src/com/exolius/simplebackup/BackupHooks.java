package com.exolius.simplebackup;

public class BackupHooks {

    public void notifyBackupCreated(String command, String filename) {
       if(!command.isEmpty()) {
	  try {
	    ProcessBuilder pb = new ProcessBuilder(command, filename);
	    pb.start();
	  } catch(Exception ex) {
	    
	  }
       }
    }
}
