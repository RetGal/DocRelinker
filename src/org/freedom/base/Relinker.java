package org.freedom.base;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class Relinker {

	public enum DocType {ODT, DOCX}
	
	private static File mainDocument;
	private static File targetDirectory;
	private static final String RELATED = "related";
	private static String relatedDir;
	private static String tempDir;
	private static Set<String> relatedDocuments;
	private static DocType docType;

	public Relinker(File document, File path) throws IOException {
		setMainDocument(document);
		setTargetDirectory(path);
	}

	public static void main(String[] args) throws IOException {

		while (true) {
			try {
				String documentName = Utils.getUserInput("main document").trim();
				// double quotes appear under windows if the user drags a file
				// to the command window while under linux they are being surrounded by quotes
				documentName = Utils.stripQuotes(documentName);
				if (!documentName.isEmpty()) {
					setMainDocument(new File(documentName));
					String targetPath = Utils.getUserInput("target path").trim();
					targetPath = Utils.stripQuotes(targetPath);
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
		
		System.out.println("Working directory: " +System.getProperty("user.dir"));

		tempDir = targetDirectory + File.separator + ".~";
		relatedDir = targetDirectory.getAbsolutePath() + File.separator + RELATED;

		cleanUp();
		// create a brand new temporary folder
		File f = new File(tempDir);
		f.mkdir();

		// deflate the document
		Utils.unzip(mainDocument.getAbsolutePath(), tempDir);

		List<File> originalXML = new LinkedList<>();
		List<File> backupXML = new LinkedList<>();

		if (mainDocument.getName().endsWith(".docx")) {
			docType = DocType.DOCX;
			originalXML.add(new File(tempDir + File.separator + "word" + File.separator + "_rels" + File.separator
					+ "document.xml.rels"));
			// linked documents from endnotes reside in another file
			File endnotes = new File(tempDir + File.separator + "word" + File.separator + "_rels" + File.separator
					+ "endnotes.xml.rels");
			if (endnotes.exists() && endnotes.isFile() && endnotes.canRead()) {
				originalXML.add(endnotes);
			}
		} else if (mainDocument.getName().endsWith(".odt")) {
			docType = DocType.ODT;
			originalXML.add(new File(tempDir + File.separator + "content.xml"));
		}

		if (!originalXML.get(0).exists()) {
			throw new IllegalArgumentException(
					"Unexpected file format (" + originalXML.get(0).getName() + " not found)");
		}

		relatedDocuments = new HashSet<>();

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
		Utils.zip(new File(tempDir), new File(tempDir + File.separator + mainDocument.getName()), docType);

		// move the (re-)created document out of the temporary folder
		f = new File(tempDir + File.separator + mainDocument.getName());
		f.renameTo(new File(targetDirectory.getAbsolutePath() + File.separator + mainDocument.getName()));

		// delete the temporary folder including its content
		System.out.println("Removing " + tempDir);
		cleanUp();

		// create related folder
		f = new File(relatedDir);
		if (!f.exists() || !f.isDirectory()) {
			f.mkdir();
		}

		// copy all related documents to the related folder inside the target
		System.out.println("Copying the related documents to " + relatedDir);
		copyRelatedDocuments();

	}

	private static void replaceAbsoluteLinks(File backupXML, File originalXML) {

		if (docType.equals(DocType.DOCX)) {
			// read document.xml.rels.bak, manipulate its content and save as document.xml.rels
			DocxRelinker xmlRelinker = new DocxRelinker(backupXML, originalXML);
			// a set containing all related documents
			relatedDocuments.addAll(xmlRelinker.relink(RELATED));
		} else if (docType.equals(DocType.ODT)) {
			// read cobtent.xml.bak, manipulate its content and save as content.xml
			OdtRelinker xmlRelinker = new OdtRelinker(backupXML, originalXML);
			// a set containing all related documents
			relatedDocuments.addAll(xmlRelinker.relink(RELATED));
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
			String relatedDocStr = URLDecoder.decode(relatedDoc, "utf-8");
			// maybe relative ?
			File related = relatedDocStr.startsWith(".."+File.separator) ? fixRelative(relatedDocStr) : new File(relatedDocStr);
			File destination = new File(URLDecoder.decode(targetFullPath.toString(), "utf-8"));

			System.out.println("Related: " + related);
			System.out.println("Destination: " + destination);

			if (related.exists()) {
				Utils.copyFile(related, destination);
			} else {
				System.err.println("Related document '" + relatedDoc + "' related '" + related + "' not found");
			}

		}

	}
	
	static private File fixRelative(String relatedDocStr) {
		String needle = ".."+File.separator;
		int len = needle.length();
		int lastIndex = 0;
		int count = 0;
		while (lastIndex != -1) {
		    lastIndex = relatedDocStr.indexOf(needle, lastIndex);
		    if (lastIndex != -1) {
		        count++;
		        lastIndex += len;
		    }
		}
		if (count > 0) {
			lastIndex = count*len;
			ArrayList<String> parts = new ArrayList<>(Arrays.asList(mainDocument.getAbsolutePath().split(File.separator)));
			if (count <= parts.size()) {
				for (int i=0; i<count; i++) {
					parts.remove(parts.size()-1);
				}
				return new File(String.join(File.separator, parts) + File.separator + relatedDocStr.substring(lastIndex));
			}
		}
		return new File(relatedDocStr);
	}

	private static void cleanUp() {

		// cleanup pre exsiting temporary files
		File f = new File(tempDir);
		if (f.exists() && f.isDirectory()) {
			Utils.deleteTempData(f);
		}

	}

}