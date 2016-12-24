package org.freedom.base;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.freedom.log.Log;
import org.freedom.base.Relinker.DocType;
import static org.freedom.base.Relinker.DocType.*;

class Utils {

	/**
	 * Obtain user input from command line
	 * 
	 * @param whatever
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
	 * Strips (double) quotes at both ends of a String
	 */
	public static String stripQuotes(String path) {

		//return path.replaceAll("^[\"']+|[\"']+$","");
		return path.replaceAll("^(['\"])(.*)\\1$", "$2");
	}

	public static void copyFile(File sourceFile, File destinationFile) throws IOException {

		Path source = Paths.get(sourceFile.getPath());
		Path destination = Paths.get(destinationFile.getPath());
		Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
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

		try (InputStream in = new FileInputStream(file)) {
			copy(in, out);
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
				assert files != null;
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
	 * Guess what - this one extracts a zipped file
	 * 
	 * @param zipFile  The file to be unzipped
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

		Log.info("Extracting " + zipFile + " into " + outputFolder);
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
			// Log.info("OK!");
			ze = zis.getNextEntry();
		}
		zis.closeEntry();
		zis.close();

		Log.info("Extraction completed");

	}

	/**
	 * Zipps specified directory and all its subdirectories (excludes files with
	 * .odt or .docx suffix) Adds uncompressed mimetype as first file if docType
	 * is ODT
	 * 
	 * @param directory Specified directory
	 * @param zipFile Output ZIP file name
	 * @param docType DOCX or ODT
	 * @throws IOException
	 */

	public static void zip(File directory, File zipFile, DocType docType) throws IOException {

		final int waste = directory.getAbsolutePath().length() + 1;
		Deque<File> queue = new LinkedList<>();
		queue.push(directory);

		ZipOutputStream zout = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)));

		zout.setMethod(ZipOutputStream.STORED);

		if (docType.equals(ODT)) {
			zout = writeMimeType(queue, zout);
		}

		zout.setMethod(ZipOutputStream.DEFLATED);
		zout.setLevel(-1);

		while (!queue.isEmpty()) {
			directory = queue.pop();
			for (File kid : directory.listFiles()) {
				// we need relative paths inside the zip file
				String name = kid.getAbsolutePath().substring(waste);
				// always use / as separator in zip files
				if (File.separatorChar != '/') {
					name = name.replace(File.separatorChar, '/');
				}
				// Log.info(name);
				if (kid.isDirectory()) {
					queue.push(kid);
					name = name.endsWith("/") ? name : name + "/";
					// Log.info("Zip adding dir "+name);
					zout.putNextEntry(new ZipEntry(name));
					zout.closeEntry();
				} else if ((!docType.equals(ODT)
						|| (!kid.getName().endsWith(".odt") && !kid.getName().equals("mimetype")))
						&& (!docType.equals(DOCX) || !kid.getName().endsWith(".docx"))) {
					Log.debug("Zip adding file " + name);
					zout.putNextEntry(new ZipEntry(name));
					copy(kid, zout);
					zout.closeEntry();
				}
			}
		}
		zout.close();
	}

	private static ZipOutputStream writeMimeType(Deque<File> queue, ZipOutputStream zout) throws IOException {

		for (File node : queue) {
			for (File file : node.listFiles()) {
				// mimetype must be the first entry and not compressed
				if (file.getName().equals("mimetype")) {
					Log.debug("Zip adding uncompressed mimetype");

					ZipEntry entry = new ZipEntry(file.getName());
					entry.setMethod(ZipOutputStream.STORED);

					Path path = Paths.get(file.getPath());
					byte[] data = Files.readAllBytes(path);
					int len = data.length;

					entry.setSize(len);
					entry.setCompressedSize(len);
					CRC32 crc32 = new CRC32();
					crc32.update(data, 0, len);
					entry.setCrc(crc32.getValue());

					zout.putNextEntry(entry);
					zout.write(data);
					zout.closeEntry();
					return zout;
				}
			}
		}
		return zout;
	}

}
