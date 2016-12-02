package org.freedom.base;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

class Utils {

	/**
	 * Obtain user input from command line
	 * 
	 * @param whatever
	 * 
	 * @return the input from the command line
	 */
	public static String getUserInput(String whatever) {

		// prompt the user to enter whatever
		System.out.print("Enter " + whatever + " :");
		@SuppressWarnings("resource")
		Scanner scanner = new Scanner(System.in);
        return scanner.nextLine();

	}

	/**
	 * Strips double quotes at both ends of a String
	 * 
	 * @param path
	 * @return
	 */
	public static String stripDoublequotes(String path) {

		if (path != null && path.startsWith("\"") && path.endsWith("\"")) {
			path = path.substring(1, path.length() - 1);
		}
		return path;
	}

	public static void copyFile(File sourceFile, File destinationFile) throws IOException {

		InputStream is = null;
		OutputStream os = null;
		try {
			is = new FileInputStream(sourceFile);
			os = new FileOutputStream(destinationFile);
			byte[] buffer = new byte[1024];
			int length;
			while ((length = is.read(buffer)) > 0) {
				os.write(buffer, 0, length);
			}
		} finally {
			assert is != null;
			is.close();
			assert os != null;
			os.close();
		}
	}

	private static void copy(InputStream in, OutputStream out) throws IOException {

		byte[] buffer = new byte[1024];
		while (true) {
			int readCount = in.read(buffer);
			if (readCount < 0) {
				break;
			}
			out.write(buffer, 0, readCount);
		}
	}

	private static void copy(File file, OutputStream out) throws IOException {
		
		InputStream in = new FileInputStream(file);
		try {
			copy(in, out);
		} finally {
			in.close();
		}
	}

	@SuppressWarnings("unused")
	private static void copy(InputStream in, File file) throws IOException {
		
		OutputStream out = new FileOutputStream(file);
		try {
			copy(in, out);
		} finally {
			out.close();
		}
	}

	/**
	 * Deletes directory and all its subdirectories
	 * 
	 * @param folder Specified directory
	 * @throws IOException
	 */
	public static void deleteTempData(File folder) {

		if (folder.isDirectory()) {
			// directory is empty, then delete it
			if (folder.list().length == 0) {
				folder.delete();
			} else {
				// list all the directory contents
				String files[] = folder.list();
				for (String temp : files) {
					// construct the file structure
					File fileDelete = new File(folder, temp);
					// recursive delete
					deleteTempData(fileDelete);
				}
				// check the directory again, if empty then delete it
				if (folder.list().length == 0) {
					folder.delete();
				}
			}
		} else {
			// if file, then delete it
			folder.delete();
		}
	}

	/**
	 * 
	 * @param zipFile The file to be unzipped
	 * @param outputFolder Where the content shall be extracted to
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
	public static void unzip(String zipFile, String outputFolder) throws IllegalArgumentException, IOException {

		if (zipFile == null || zipFile.length() == 0) {
			throw new IllegalArgumentException("archive file must be specified");

		}
		if (outputFolder == null || outputFolder.length() == 0) {
			throw new IllegalArgumentException("output folder must be specified");
		}

		System.out.println("Extracting " + zipFile + " into " + outputFolder);
		ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
		ZipEntry ze = zis.getNextEntry();
		while (ze != null) {
			String entryName = ze.getName();
			// System.out.print("Extracting " + entryName + " -> " +
			// outputFolder + File.separator + entryName + "...");
			File f = new File(outputFolder + File.separator + entryName);
			// create all folder needed to store in correct relative path.
			f.getParentFile().mkdirs();
			FileOutputStream fos = new FileOutputStream(f);
			int len;
			byte buffer[] = new byte[1024];
			while ((len = zis.read(buffer)) > 0) {
				fos.write(buffer, 0, len);
			}
			fos.close();
			// System.out.println("OK!");
			ze = zis.getNextEntry();
		}
		zis.closeEntry();
		zis.close();

		System.out.println("Extraction completed");

	}

	/**
	 * Zipps specified directory and all its subdirectories (exclude files with .odt or .docx suffix)
	 * 
	 * @param directory Specified directory
	 * @param zipFile Output ZIP file name
	 * @throws IOException
	 */

	@SuppressWarnings("resource")
	public static void zip(File directory, File zipFile) throws IOException {

		final int waste = directory.getAbsolutePath().length() + 1;
		Deque<File> queue = new LinkedList<File>();
		queue.push(directory);
		OutputStream out = new FileOutputStream(zipFile);
		Closeable res = out;

		ZipOutputStream zout = new ZipOutputStream(out);
		res = zout;
		while (!queue.isEmpty()) {
			directory = queue.pop();
			for (File kid : directory.listFiles()) {
				// we need relative paths inside the zip file
				String name = kid.getAbsolutePath().substring(waste);
				// always use / as separator in zip files
				if (File.separatorChar != '/') {
					name = name.replace(File.separatorChar, '/');
				}
				// System.out.println(name);
				if (kid.isDirectory()) {
					queue.push(kid);
					name = name.endsWith("/") ? name : name + "/";
					// System.out.println("Zip adding dir "+name);
					zout.putNextEntry(new ZipEntry(name));
				} else {
					if (kid.getName().endsWith(".docx") || kid.getName().endsWith(".odt")) {
						continue;
					}
					// System.out.println("Zip adding file "+name);
					zout.putNextEntry(new ZipEntry(name));
					copy(kid, zout);
					zout.closeEntry();
				}
			}
		}
		res.close();
		out.close();
	}

}
