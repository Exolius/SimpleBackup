package com.exolius.simplebackup;

/**
 * Class created to clean up code. 
 * This is just a runnable that will be called by bukket.
 * It is moved into here to clean up the SimpleBackup.java.
 * @author gravypod
 *
 */
public class Scheduler implements Runnable {

	SimpleBackup plugin;
	
	Scheduler(SimpleBackup plugin) {
		this.plugin = plugin;
	}
	
	public void run() {
		plugin.addNewBackup();		
	}

}
