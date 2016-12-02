package org.freedom.base;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class Relinker {

	private static File mainDocument;
	private static File targetDirectory;
	private static final String RELATED = "related";
	private static String relatedDir;
	private static String tempDir;
	private static Set<String> relatedDocuments;

    public Relinker(File document, File path) throws IOException {
		setMainDocument(document);
		setTargetDirectory(path);
	}

	public static void main(String[] args) throws IOException {

		while (true) {
			try {
				String documentName = Utils.getUserInput("main document").trim();
				// double quotes appear under windows if the user drags a file
				// to the command window
				documentName = Utils.stripDoublequotes(documentName);
				if (!documentName.isEmpty()) {
					setMainDocument(new File(documentName));
					String targetPath = Utils.getUserInput("target path").trim();
					// double quotes appear under windows if the user drags a file
					// to the command window
					targetPath = Utils.stripDoublequotes(targetPath);
					if (!targetPath.isEmpty()) {
						setTargetDirectory(new File(targetPath));
						break;
					}
				}
			} catch (IOException e) {
				System.out.println(e.getMessage());
			}
		}
		process();

	}

	public static void setMainDocument(File amainDocument) throws IOException {

		if (!amainDocument.exists() || !amainDocument.canRead()) {
			throw new IOException("File '" + amainDocument + "' is not readable");
		}
		// else
		mainDocument = amainDocument;
	}

	public File getMainDocument() {

		return mainDocument;
	}

	public static void setTargetDirectory(File atargetDirectory) throws IOException {

		if (!atargetDirectory.exists() || !atargetDirectory.canWrite()) {
			throw new IOException("Folder '" + atargetDirectory + "' is not writable");
		}
		// else
		targetDirectory = atargetDirectory;
	}

	public File getTargetDirectory() {

		return targetDirectory;
	}

	public static void process() throws IOException {

		if (mainDocument == null || targetDirectory == null) {
			throw new IllegalArgumentException("The main document and the target directory must be set first");
		}

		tempDir = targetDirectory + File.separator + ".~";

		cleanUp();
		// create a brand new temporary folder
		File f = new File(tempDir);
		f.mkdir();

		// deflate the document
		Utils.unzip(mainDocument.getAbsolutePath(), tempDir);

        List<File> originalXML = new LinkedList<File>();
        List<File> backupXML = new LinkedList<File>();

		if (mainDocument.getName().endsWith("docx")) {
			originalXML.add(new File(tempDir + File.separator + "word" + File.separator + "_rels" + File.separator
					+ "document.xml.rels"));
			// linked documents from endnotes reside in another file
			File endnotes = new File(tempDir + File.separator + "word" + File.separator + "_rels" + File.separator
					+ "endnotes.xml.rels");
			if (endnotes.exists() && endnotes.isFile() && endnotes.canRead()) {
				originalXML.add(endnotes);
			}
		} else if (mainDocument.getName().endsWith("odt")) {
			originalXML.add(new File(tempDir + File.separator + "content.xml"));
		}

		if (!originalXML.get(0).exists()) {
			throw new IllegalArgumentException(
					"Unexpected file format (" + originalXML.get(0).getName() + " not found)");
		}

		relatedDocuments = new HashSet<String>();

		for (int i = 0; i < originalXML.size(); i++) {
			backupXML.add(new File(originalXML.get(i).getAbsolutePath() + ".bak"));

			// create a backup of the file within the document which holds the
			// links to the related documents (used as working copy)
			Utils.copyFile(originalXML.get(i), backupXML.get(i));

			// alter the links inside the document, collect the related
			// documents
			replaceAbsoluteLinks(backupXML.get(i), originalXML.get(i));

			// delete the working copy
			backupXML.get(i).delete();
		}

		// re-create the document
		System.out.println("Re-creating " + mainDocument.getName());
		// Utils.zipDirectory(new File(targetPath),
		// targetPath+separator+documentFileName);
		Utils.zip(new File(tempDir), new File(tempDir + File.separator + mainDocument.getName()));

		// move the (re-)created document out of the temporary folder
		f = new File(tempDir + File.separator + mainDocument.getName());
		f.renameTo(new File(targetDirectory.getAbsolutePath() + File.separator + mainDocument.getName()));

		// delete the temporary folder including its content
		System.out.println("Removing " + tempDir);
		cleanUp();

		// create related folder
		relatedDir = targetDirectory.getAbsolutePath() + File.separator + RELATED;
		f = new File(relatedDir);
		if (!f.exists() || !f.isDirectory()) {
			f.mkdir();
		}

		// copy all related documents to the related folder inside the target
		System.out.println("Copying the related documents to " + relatedDir);
		copyRelatedDocuments();

	}

	private static void replaceAbsoluteLinks(File backupXML, File originalXML) {

		if (mainDocument.getName().endsWith("docx")) {
			// read document.xml.rels.bak, manipulate its content and save as document.xml.rels
			DocxRelinker xmlRelinker = new DocxRelinker(backupXML, originalXML);
			// a set containing all related documents
			relatedDocuments.addAll(xmlRelinker.relink(relatedDir));
		} else if (mainDocument.getName().endsWith("odt")) {
			// read cobtent.xml.bak, manipulate its content and save as content.xml
			OdtRelinker xmlRelinker = new OdtRelinker(backupXML, originalXML);
			// a set containing all related documents
			relatedDocuments.addAll(xmlRelinker.relink(relatedDir));
		}

	}

	private static void copyRelatedDocuments() throws IOException {

		System.out.println("Total number of related documents: " + relatedDocuments.size());
		for (String relatedDoc : relatedDocuments) {
			StringBuilder targetFullPath = new StringBuilder(relatedDir);
			targetFullPath.append(File.separator);
			if (relatedDoc.contains(File.separator)) {
				targetFullPath.append(relatedDoc.substring(relatedDoc.lastIndexOf(File.separator) + 1));
			} else if (relatedDoc.contains("\\")) {
				targetFullPath.append(relatedDoc.substring(relatedDoc.lastIndexOf("\\") + 1));
			} else if (relatedDoc.contains("%5C")) {
				targetFullPath.append(relatedDoc.substring(relatedDoc.lastIndexOf("%5C") + 3));
			} else {
				targetFullPath.append(relatedDoc);
			}

			// strings from links may contain special chars like %20
			File related = new File(URLDecoder.decode(relatedDoc, "utf-8"));
			File destination = new File(URLDecoder.decode(targetFullPath.toString(), "utf-8"));

			System.out.println("related :" + related);
			System.out.println("destination :" + destination);

			System.out.println();
			if (related.exists()) {
				Utils.copyFile(related, destination);
			} else {
				System.err.println("Related document '" + relatedDoc + "' related '" + related + "' not found");
			}

		}

	}

	private static void cleanUp() throws IOException {

		// cleanup pre exsiting temporary files
		File f = new File(tempDir);
		if (f.exists() && f.isDirectory()) {
			Utils.deleteTempData(f);
		}

	}

}