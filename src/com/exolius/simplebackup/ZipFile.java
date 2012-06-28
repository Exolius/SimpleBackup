package com.exolius.simplebackup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipFile {

	/**
	 * Zip files
	 * 
	 * @param sourceFolder
	 * @param destinationFile
	 * @throws IOException
	 */
	protected synchronized static void zipFiles(File sourceFolder,
			File destinationFile) throws IOException {
		if (!destinationFile.getParentFile().exists()) {
			destinationFile.getParentFile().mkdirs();
		}
		try {
			ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(destinationFile));
			zipFiles(sourceFolder.toURI(), sourceFolder, zip);
			zip.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Zip files
	 * 
	 * @param root
	 * @param source
	 * @param zip
	 * @throws IOException
	 */
	protected synchronized static void zipFiles(URI root, File source,
			ZipOutputStream zip) throws IOException {
		if (source.isDirectory()) {
			for (String file : source.list()) {
				zipFiles(root, new File(source, file), zip);
			}
		} else {
			ZipEntry entry = new ZipEntry(root.relativize(source.toURI())
					.getPath());
			zip.putNextEntry(entry);
			InputStream in = new FileInputStream(source);
			try {
				byte[] buffer = new byte[4096];
				int bytesRead;
				while ((bytesRead = in.read(buffer)) > 0) {
					zip.write(buffer, 0, bytesRead);
				}
			} finally {
				in.close();
			}
		}
	}
}
