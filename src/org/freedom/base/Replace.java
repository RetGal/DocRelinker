package org.freedom.base;

import org.freedom.log.Log;
import org.jdom2.Content;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static java.lang.System.exit;

public class Replace {

	private static String masterDocumentName = "01-PCTS-Master.ods";
	private static File masterDocument;
	private static String sourceDirectoryName;
	private static String tempDirectoryName;
	private static final String BACKUP = "backup";
	private static File backupDirectory;
	private static String fileExtension = ".ods";
	private static File[] sourceDocuments;
	private static Integer position = null;

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
					String newMasterDocumentName = Utils.getUserInput("the name of the master document containing the sheet to be inserted (located within " + sourceDirectoryName + ")\ndefault: " + masterDocumentName).trim();
					if (!newMasterDocumentName.isEmpty()) {
						masterDocumentName = newMasterDocumentName;
					}
					masterDocument = new File(sourceDirectoryName + File.separator + masterDocumentName);
					if (!masterDocument.exists() || !masterDocument.canRead()) {
						Log.error("File '" + masterDocumentName + "' can not be found");
						exit(1);
					}
					sourceDocuments = listDocuments(Utils.stripQuotes(sourceDirectoryName));
					String backupDirectoryName = sourceDirectoryName + File.separator + BACKUP;
					backupDirectory = new File(backupDirectoryName);
					backupDirectory.mkdir();
					if (!backupDirectory.exists() || !backupDirectory.canWrite()) {
						Log.error("Folder '" + backupDirectoryName + "' is not writable");
						exit(1);
					}
					String positionString = Utils.getUserInput("the position of the sheet to be replaced (first sheet = 1)\ndefault: last sheet").trim();
					if (!positionString.isEmpty()) {
						try {
						position = Integer.parseInt(positionString);
						}
						catch (java.lang.NumberFormatException e) {
							Log.error("The position must be a positive integer");
							exit(1);	
						}
						if (position.intValue() < 1) {
							Log.error("The position must be a positive integer");
							exit(1);	
						}
					}
					break;
				}
			}
		}	
	}

	private static File[] listDocuments(String sourceDirectory) {
		File dir = new File(sourceDirectory);
		return dir.listFiles((file, name) -> name.toLowerCase().endsWith(fileExtension) && !name.toLowerCase().equals(masterDocumentName.toLowerCase()));
	}

	public static void process() throws IOException {
		tempDirectoryName = sourceDirectoryName + File.separator + "tmp";

		List<Content> masterSheet = extractMasterSheet(position);

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
			replaceSheet(backupXML, originalXML, masterSheet, position);

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
	
	private static List<Content> extractMasterSheet(Integer position) throws IOException {
		
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
		
		File f = new File(tempDirectoryName);
		f.mkdir();
		
		// deflate the document
		Utils.unzip(masterDocument.getAbsolutePath(), tempDirectoryName);
		
		File originalXML = new File(tempDirectoryName + File.separator + "content.xml");
		if (!originalXML.exists() || !originalXML.canRead()) {
			throw new IllegalArgumentException("Unexpected file format (originalXML not found)");
		}
		
		File backupXML = new File(originalXML.getAbsolutePath() + ".bak");
		
		// create a backup of the file within the document which holds the
		// links to the related documents (used as working copy)
		Utils.copyFile(originalXML, backupXML);
		
		OdsReplace odsReplace = new OdsReplace(backupXML, originalXML);
		List<Content> masterSheet = odsReplace.extractSheet(sourceDirectoryName, position);
		
	
		// delete the temporary folder including its content
		cleanUp();
		
		return masterSheet;
		
	}

	private static void replaceSheet(File backupXML, File originalXML, List<Content> newSheet, Integer position) {
			// read content.xml.bak, replace a sheet and save as content.xml
			OdsReplace odsReplace = new OdsReplace(backupXML, originalXML);
			odsReplace.replace(sourceDirectoryName, newSheet, position);
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