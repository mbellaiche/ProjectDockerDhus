package com.gael.testdocker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;

public class Utils {
	public static void createTar(final String tarName, String pathName) throws IOException {
		
		List<File> pathEntries = new ArrayList<File>();
		pathEntries.add(new File(pathName));
		
        OutputStream tarOutput = new FileOutputStream(new File(tarName));

        ArchiveOutputStream tarArchive = new TarArchiveOutputStream(tarOutput);

        List<File> files = new ArrayList<File>();

        for (File file : pathEntries) {
            files.addAll(recurseDirectory(file));
        }

        for (File file : files) {

            TarArchiveEntry tarArchiveEntry = new TarArchiveEntry(file, file.getName());
            tarArchiveEntry.setSize(file.length());
            tarArchive.putArchiveEntry(tarArchiveEntry);
            FileInputStream fileInputStream = new FileInputStream(file);
            IOUtils.copy(fileInputStream, tarArchive);
            fileInputStream.close();
            tarArchive.closeArchiveEntry();
        }

        tarArchive.finish();
        tarOutput.close();
    }

	public static List<File> recurseDirectory(final File directory) {
	
	    List<File> files = new ArrayList<File>();
	
	    if (directory != null && directory.isDirectory()) {
	
	        for (File file : directory.listFiles()) {
	
	            if (file.isDirectory()) {
	                files.addAll(recurseDirectory(file));
	            } else {
	                files.add(file);
	            }
	        }
	    }
	
	    return files;
	}
	
	public static void deleteFile(String nameFile)
	{
		File f = new File(nameFile);
		if (f.exists())
		{
			f.delete();
		}
	}
}
