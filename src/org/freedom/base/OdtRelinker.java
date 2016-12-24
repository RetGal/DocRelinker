package org.freedom.base;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

import org.freedom.log.Log;

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

			Log.info("Processing XML");
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

	private void walkThrough(Element element) throws UnsupportedEncodingException {

		for (Element el : element.getChildren()) {
			for (Attribute at : el.getAttributes()) {
				if (at.getName().equals("href")) {
					if (at.getValue().startsWith("http://127.0.0.1:4664/redir?url=file%3A%2F%2F")) {
						// handle Google Desktop Search links
						List<String> targetStrings = handleGDS(at.getValue(), relatedDirName);
						relatedDocuments.add(targetStrings.get(0));
						Log.info("Related file: " + targetStrings.get(1));
						// ---- Modify XML data ----
						at.setValue(targetStrings.get(1));

					} else {
						// handle regular file links
						List<String> targetStrings = handleLink(at.getValue(), relatedDirName);
						if (!targetStrings.isEmpty()) {
							relatedDocuments.add(targetStrings.get(0));
							Log.info("Related file: " + targetStrings.get(1));
							// ---- Modify XML data ----
							at.setValue(targetStrings.get(1));
						}
					}
				}
			}
			if (el.getChildren().size() > 0) {
				walkThrough(el);
			}
		}

	}

	/**
	 * handles regular ODT links
	 */
	private List<String> handleLink(String link, String relatedDirName) {
		if (link == null || relatedDirName == null) {
			throw new IllegalArgumentException("link and related dir must not be null");
		}

		List<String> targetStrings = new ArrayList<>();
		// skip internal and other non file links
		if (!link.startsWith("http") && !link.startsWith("Pictures") && !link.startsWith("mailto:")
				&& !link.startsWith("javascript:")) {
			String target;
			if (link.contains("file:///")) {
				target = link.substring(8, link.length());
			} else {
				target = link;
			}
			// set cleaned original filename including its path
			targetStrings.add(target);
			Log.debug("Target: " + target);

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
			// set the new link to the file which will be valid after the file will have been moved
			targetStrings.add(targetFileName.toString());
		}
		return targetStrings;
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
