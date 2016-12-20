package org.freedom.base;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.output.support.AbstractXMLOutputProcessor;
import org.jdom2.output.support.FormatStack;
import org.jdom2.output.support.XMLOutputProcessor;

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
						walkThrough(element);
					}
				}
			}
			// ---- Write result ----
			XMLOutputter outp = new XMLOutputter(ODTCOMPLIANT);
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

	private void walkThrough(Element element) {

		for (Element el : element.getChildren()) {
			for (Attribute at : el.getAttributes()) {
				if (at.getName().equals("href")) {
					if (at.getValue().startsWith("http://127.0.0.1:4664/redir?url=file%3A%2F%2F")) {
						// handle Google Desktop Search
						String targetLink = at.getValue();
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
						// ---- Modify XML data ----
						at.setValue(targetFileName.toString());

					} else if (!at.getValue().startsWith("http") && !at.getValue().startsWith("Pictures")
							&& !at.getValue().startsWith("mailto:") && !at.getValue().startsWith("javascript:")) {
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
				walkThrough(el);
			}
		}

	}

	/**
	 * In order to be ODT compliant, single quotes in attribute values must be
	 * escaped
	 */
	private static final XMLOutputProcessor ODTCOMPLIANT = new AbstractXMLOutputProcessor() {

		@Override
		protected void printAttribute(final Writer out, final FormatStack fstack, final Attribute attribute)
				throws IOException {

			if (!attribute.isSpecified() && fstack.isSpecifiedAttributesOnly()) {
				return;
			}
			write(out, " ");
			write(out, attribute.getQualifiedName());
			write(out, "=");
			write(out, "\"");
			String value = Format.escapeAttribute(fstack.getEscapeStrategy(), attribute.getValue());
			// do any ' escaping
			value = value.replaceAll("'", "&apos;");
			write(out, value);
			write(out, "\"");
		}

	};

}
