package org.freedom.base;

import org.freedom.log.Log;

import java.io.File;
import java.io.IOException;

import static java.lang.System.exit;

public class Repair {

	private static String masterDocumentName = "01-PCTS-Master.ods";
	private static File masterDocument;
	private static String sourceDirectoryName;
	private static String tempDirectoryName;
	private static final String BACKUP = "backup";
	private static File backupDirectory;
	private static String fileExtension = ".ods";
	private static File[] sourceDocuments;

	/**
	 * CLI only
	 */
	public static void main(String[] args) throws IOException {
		scan();
		process();
	}

	/**
	 * CLI only
	 */
	private static void scan() {
		while (true) {
			sourceDirectoryName = Utils.getUserInput("source directory (the one containing the .ods files)").trim();
			if (!new File(sourceDirectoryName).exists() || !new File(sourceDirectoryName).canRead()) {
				Log.error("Folder '" + sourceDirectoryName + "' is not readable");
			} else {
				sourceDocuments = listDocuments(Utils.stripQuotes(sourceDirectoryName));
				if (sourceDocuments.length > 0) {
					String newMasterDocumentName = Utils.getUserInput("the name of the master document (located within " + sourceDirectoryName + ")\ndefault: " + masterDocumentName).trim();
					if (!newMasterDocumentName.isEmpty()) {
						masterDocumentName = newMasterDocumentName;
					}
					masterDocument = new File(sourceDirectoryName + File.separator + masterDocumentName);
					if (!masterDocument.exists() || !masterDocument.canRead()) {
						Log.error("File '" + masterDocumentName + "' can not be found");
						exit(1);
					}
					String backupDirectoryName = sourceDirectoryName + File.separator + BACKUP;
					backupDirectory = new File(backupDirectoryName);
					backupDirectory.mkdir();
					if (!backupDirectory.exists() || !backupDirectory.canWrite()) {
						Log.error("Folder '" + backupDirectoryName + "' is not writable");
						exit(1);
					}
					break;
				}
			}
		}	
	}

	private static File[] listDocuments(String sourceDirectory) {
		File dir = new File(sourceDirectory);
		return dir.listFiles((dir1, name) -> name.toLowerCase().endsWith(fileExtension) && !name.toLowerCase().equals(masterDocumentName.toLowerCase()));
	}

	public static void process() throws IOException {
		tempDirectoryName = sourceDirectoryName + File.separator + "tmp";
		// delete the temporary folder including its content
		cleanUp();

		// create a copy of each document
		for (File document : sourceDocuments) {
			File backupDocument = new File(backupDirectory +File.separator+document.getName());
			Utils.copyFile(document, backupDocument);
		}
		// create a copy of the master document
		File backupMaster = new File(backupDirectory +File.separator + masterDocumentName);
		Utils.copyFile(masterDocument, backupMaster);

		// iterate over each file within the source directory
		for (File document : sourceDocuments) {
			// create a brand new temporary folder
			File f = new File(tempDirectoryName);
			f.mkdir();

			// deflate the document
			Utils.unzip(document.getAbsolutePath(), tempDirectoryName);

			File originalXML = new File(tempDirectoryName + File.separator + "content.xml");
			if (!originalXML.exists() || !originalXML.canRead()) {
				throw new IllegalArgumentException("Unexpected file format (originalXML not found)");
			}

			File backupXML = new File(originalXML.getAbsolutePath() + ".bak");

			// create a backup of the file within the document which holds the
			// links to the related documents (used as working copy)
			Utils.copyFile(originalXML, backupXML);

			// alter the links inside the document
			repairBrokenLinks(backupXML, originalXML);

			// delete the working copy
			backupXML.delete();

			// update the document
			try {
				Utils.updateDocument(tempDirectoryName, document.getName(), "content.xml");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			// delete the temporary folder including its content
			cleanUp();
		}
		Log.info("Processed " + sourceDocuments.length + " documents");
	}

	private static void repairBrokenLinks(File backupXML, File originalXML) {
			// read content.xml.bak, manipulate its content and save as
			// content.xml
			OdsRepair odsRepair = new OdsRepair(backupXML, originalXML);
			odsRepair.relink(sourceDirectoryName);
	}

	private static void cleanUp() {
		// cleanup existing temporary files
		File f = new File(tempDirectoryName);
		if (f.exists() && f.isDirectory()) {
			Utils.deleteTempData(f);
			Log.debug("Removing " + tempDirectoryName);
		}
	}

}