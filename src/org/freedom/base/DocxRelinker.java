package org.freedom.base;

import java.util.*;
import java.io.*;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.*;
import org.jdom2.output.*;

class DocxRelinker extends DocumentRelinker {

	public DocxRelinker(File sourceXML, File targetXML) throws IllegalArgumentException {

		super(sourceXML, targetXML);

	}

	public Set<String> relink(String relatedDirName) {

		Set<String> relatedDocuments = new HashSet<>();

		try {

			System.out.println("Processing XML");
			// ---- Read XML file ----
			SAXBuilder builder = new SAXBuilder();
			Document doc = builder.build(getSourceXML()); // <XmlFile>
			Element root = doc.getRootElement();

			// ---- Walk through XML data ----
			int children = root.getChildren().size();
			for (int i = 0; i < children; i++) {
				if (root.getChildren().get(i).getAttribute("TargetMode") != null) {
					String targetLink = root.getChildren().get(i).getAttributeValue("Target");
					System.out.println("targetLink :" + targetLink);
					if (targetLink.startsWith("file:///")) {
						String target = targetLink.substring(8, targetLink.length());
						relatedDocuments.add(target);
						System.out.println("target :" + target);

						StringBuilder targetFileName = new StringBuilder(target.length());
						targetFileName.append(".\\").append(relatedDirName).append("\\");
						if (target.contains("\\")) {
							// \ C:\Folder\Subfolder\File.doc
							targetFileName.append(target.substring(target.lastIndexOf("\\") + 1));
						} else if (target.contains(":")) {
							// : C:\File.doc
							targetFileName.append(target.substring(target.indexOf(":") + 2));
						} else {
							targetFileName.append(target);
						}
						System.out.println("TargetFileName: " + targetFileName.toString());
						// ---- Modify XML data ----
						root.getChildren().get(i).setAttribute("Target", targetFileName.toString());
					}
					// google desktop search links
					// http://127.0.0.1:4664/redir?url=file%3A%2F%2FC%3A%5CDokumente+und+Einstellungen%5Ctest%5CEigene+Dateien%5CPolitik%5CArtikel%5CUSA%5CNew+Deal%5CNew%5FMarshall%5FDeal%2Edoc%3Fevent%5Fid%3D288858%26schema%5Fid%3D6%26q%3DNew%2BMarshall%2BDeal&src=1&schema=6&s=sI_A0XO090c8ay7ZDv5Y6FEyevk
					// http://127.0.0.1:4664/redir?url=file%3A%2F%2FC%3A%5C
					// http://127.0.0.1:4664/redir?url=file://C:\
					else if (targetLink.startsWith("http://127.0.0.1:4664/redir?url=file%3A%2F%2F")) {
						// get what's between
						// "http://127.0.0.1:4664/redir?url=file%3A%2F%2F" and "%3F" (?)
						// http://127.0.0.1:4664/redir?url=file%3A%2F%2F length = 45
						String target = targetLink.substring(45, targetLink.lastIndexOf("%3F"));
						relatedDocuments.add(target);
						System.out.println("gds target :" + target);

						StringBuilder targetFileName = new StringBuilder(target.length());
						targetFileName.append(".\\").append(relatedDirName).append("\\");
						if (target.contains("%5C")) {
							// C:\Folder\Subfolder\File.doc (\)
							targetFileName.append(target.substring(target.lastIndexOf("%5C") + 3));
						} else if (target.contains("%3A")) {
							// C:File.doc (:)
							targetFileName.append(target.substring(target.indexOf("%3A") + 3));
						} else {
							targetFileName.append(target);
						}
						System.out.println("TargetFileName: " + targetFileName.toString());
						// ---- Modify XML data ----
						root.getChildren().get(i).setAttribute("Target", targetFileName.toString());
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
}
