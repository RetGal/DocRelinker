package org.freedom.base;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.Set;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;

class OdtRelinker extends DocumentRelinker {

	public OdtRelinker(File sourceXML, File targetXML) throws IllegalArgumentException {

		super(sourceXML, targetXML);

	}

	private Set<String> relatedDocuments;
	private String relatedDirName;

	public Set<String> relink(String relatedDirName) {

		relatedDocuments = new HashSet<>();
		this.relatedDirName = relatedDirName;

		try {

			System.out.println("Processing XML");
			// ---- Read XML file ----
			SAXBuilder builder = new SAXBuilder();
			Document doc = builder.build(getSourceXML()); // <XmlFile>
			Element root = doc.getRootElement();

			// ---- Walk through XML data ----
			int rootChildren = root.getChildren().size();
			for (int i = 0; i < rootChildren; i++) {
				if (root.getChildren().get(i).getName().equals("body")) {
					for (Element element : root.getChildren().get(i).getChildren()) {
						recursive(element);
					}
				}
			}
			// ---- Write result ----
			XMLOutputter outp = new XMLOutputter();
			// outp.setFormat(Format.getPrettyFormat());
			// outp.setFormat(Format.getCompactFormat());
			// outp.setFormat(Format.getRawFormat());
			// ---- Write the complete result document to XML file ----
			FileOutputStream fos = new FileOutputStream(getTargetXML());
			outp.output(doc, fos);
			fos.close();

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return relatedDocuments;
	}

	private void recursive(Element element) {

		for (Element el : element.getChildren()) {
			for (Attribute at : el.getAttributes()) {
				if (at.getName().equals("href")) {
					if (!at.getValue().startsWith("http")) {
						String targetLink = at.getValue();
						String target;
						if (targetLink.contains("file:///")) {
							target = targetLink.substring(8, targetLink.length());
						} else {
							target = targetLink;
						}
						relatedDocuments.add(target);
						StringBuilder targetFileName = new StringBuilder();

						if (target.contains("\\")) {
							// windows paths
							targetFileName.append("..\\").append(relatedDirName).append("\\");
							if (target.contains("\\")) {
								targetFileName.append(target.substring(target.lastIndexOf("\\") + 1));
							}
						} else if (target.contains("/")) {
							// *nix paths
							targetFileName.append("../").append(relatedDirName).append("/");
							if (target.contains("/")) {
								targetFileName.append(target.substring(target.lastIndexOf("/") + 1));
							}
						} else {
							// no paths
							targetFileName.append(target);
						}
						// ---- Modify XML data ----
						at.setValue(targetFileName.toString());
					}
				}
			}
			if (el.getChildren().size() > 0) {
				recursive(el);
			}
		}

	}

}
