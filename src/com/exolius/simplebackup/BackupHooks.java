package com.exolius.simplebackup;

public class BackupHooks {

	public void notifyBackupCreated(final String command, final String filename) {
		if (!command.isEmpty()) {
			try {
				final ProcessBuilder pb = new ProcessBuilder(command, filename);
				pb.start();
			} catch (final Exception ex) {

			}
		}
	}

}
