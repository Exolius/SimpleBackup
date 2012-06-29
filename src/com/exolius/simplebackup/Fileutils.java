package com.exolius.simplebackup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileUtils {
	/**
	 * This function will copy files or directories from one location to
	 * another. note that the source and the destination must be mutually
	 * exclusive. This function can not be used to copy a directory to a sub
	 * directory of itself. The function will also have problems if the
	 * destination files already exist.
	 * 
	 * @param originalFile
	 *            -- A File object that represents the source for the copy
	 * @param newFile
	 *            -- A File object that represnts the destination for the copy.
	 * @throws IOException
	 *             if unable to copy.
	 * @author <a href='http://www.dreamincode.net/code/snippet1443.htm'>NickDMax</a>
	 */
	public synchronized static void copyFiles(File originalFile, File newFile)
			throws IOException {
		// Check to ensure that the source is valid
		if (!originalFile.exists()) {
			throw new IOException("copyFiles: Can not find source: " + originalFile.getAbsolutePath() + ".");
		} else if (!originalFile.canRead()) {
			/* 
			 * check to ensure we have rights
			 * to the source...
			 */
			throw new IOException("copyFiles: No right to source: " + originalFile.getAbsolutePath() + ".");
		} 
		// is this a directory copy?
		if (originalFile.isDirectory()) {
			if (!newFile.exists()) { 
				/*
				 *  does the destination already exist?
				 *	if not we need to make it exist if possible (note this is mkdirs not mkdir)
				 */
				if (!newFile.mkdirs()) {
					throw new IOException(
							"copyFiles: Could not create direcotry: "
									+ newFile.getAbsolutePath() + ".");
				}
			}
			// get a listing of files...
			String list[] = originalFile.list();
			// copy all the files in the list.
			for (int i = 0; i < list.length; i++) {
				File dest1 = new File(newFile, list[i]);
				File src1 = new File(originalFile, list[i]);
				copyFiles(src1, dest1);
			}
		} else {
			// This was not a directory, so lets just copy the file
			FileInputStream fin = null;
			FileOutputStream fout = null;
			// Buffer 4K at a time (you can change this).
			byte[] buffer = new byte[4096]; 
			int bytesRead;
			try {
				// open the files for input and output
				fin = new FileInputStream(originalFile);
				fout = new FileOutputStream(newFile);
				// while bytesRead indicates a successful read, lets write...
				while ((bytesRead = fin.read(buffer)) >= 0) {
					fout.write(buffer, 0, bytesRead);
				}
			} catch (IOException e) { 
				// Error copying file...
				IOException wrapperException = new IOException("copyFiles: Unable to copy file: " + originalFile.getAbsolutePath() + "to" + newFile.getAbsolutePath() + ".");
				wrapperException.initCause(e);
				wrapperException.setStackTrace(e.getStackTrace());
				throw wrapperException;
			} finally { 
				// Ensure that the files are closed (if they were open).
				if (fin != null) {
					fin.close();
				}
				if (fout != null) {
					fout.close();
				}
			}
		}
	}
}
