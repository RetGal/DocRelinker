package org.freedom.base;

import java.util.*;
import java.io.*;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.*;
import org.jdom2.output.*;

import org.freedom.log.Log;

class DocxRelinker extends DocumentRelinker {

	public DocxRelinker(File sourceXML, File targetXML) throws IllegalArgumentException {

		super(sourceXML, targetXML);

	}

	public Set<String> relink(String relatedDirName) {

		Set<String> relatedDocuments = new HashSet<>();

		try {

			Log.info("Processing XML");
			// ---- Read XML file ----
			SAXBuilder builder = new SAXBuilder();
			Document doc = builder.build(getSourceXML()); // <XmlFile>
			Element root = doc.getRootElement();

			// ---- Walk through XML data ----
			int children = root.getChildren().size();
			for (int i = 0; i < children; i++) {
				if (root.getChildren().get(i).getAttribute("TargetMode") != null) {
					String targetLink = root.getChildren().get(i).getAttributeValue("Target");
					Log.info("targetLink: " + targetLink);
					if (targetLink.startsWith("file:///")) {
						// handle regular file links
						List<String> targetStrings = handleLink(targetLink, relatedDirName);
						relatedDocuments.add(targetStrings.get(0));
						Log.info("TargetFileName: " + targetStrings.get(1));
						// ---- Modify XML data ----
						root.getChildren().get(i).setAttribute("Target", targetStrings.get(1));
					} else if (targetLink.startsWith("http://127.0.0.1:4664/redir?url=file%3A%2F%2F")) {
						// handle Google Desktop Search links
						List<String> targetStrings = handleGDS(targetLink, relatedDirName);
						relatedDocuments.add(targetStrings.get(0));
						Log.info("TargetFileName: " + targetStrings.get(1));
						// ---- Modify XML data ----
						root.getChildren().get(i).setAttribute("Target", targetStrings.get(1));
					}
				}
			}
			// ---- Write result ----
			XMLOutputter outp = new XMLOutputter();
			// outp.setFormat(Format.getPrettyFormat());
			outp.setFormat(Format.getCompactFormat());
			// ---- Write the complete result document to XML file ----
			FileOutputStream fos = new FileOutputStream(getTargetXML());
			outp.output(doc, fos);
			fos.close();

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return relatedDocuments;
	}

	/**
	 * handles regular DOCX links
	 */
	private List<String> handleLink(String link, String relatedDirName) {
		if (link == null || relatedDirName == null) {
			throw new IllegalArgumentException("link and related dir must not be null");
		}

		List<String> targetStrings = new ArrayList<>();
		String target = link.substring(8, link.length());
		// set cleaned original filename including its path
		targetStrings.add(target);
		Log.info("target: " + target);

		StringBuilder targetFileName = new StringBuilder(target.length());
		targetFileName.append(".\\").append(relatedDirName).append("\\");
		if (target.contains("\\")) {
			// \ C:\Folder\Subfolder\File.doc
			targetFileName.append(target.substring(target.lastIndexOf("\\") + 1));
		} else if (target.contains(":")) {
			// : C:File.doc
			targetFileName.append(target.substring(target.indexOf(":") + 1));
		} else {
			targetFileName.append(target);
		}
		// set the new link to the file which will be valid after the file will have been moved
		targetStrings.add(targetFileName.toString());

		return targetStrings;
	}

}
