package org.freedom.base;

import static java.lang.Math.toIntExact;

import org.freedom.log.Log;
import org.jdom2.Attribute;
import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.output.support.AbstractXMLOutputProcessor;
import org.jdom2.output.support.FormatStack;
import org.jdom2.output.support.XMLOutputProcessor;

import java.io.*;
import java.util.List;

class OdsReplace extends DocumentRelinker {

	public OdsReplace(File sourceXML, File targetXML) throws IllegalArgumentException {
		super(sourceXML, targetXML);
	}

	private String targetDirName;

	public void replace(String sourceDirName, List<Content> newSheet, Integer position) {
		this.targetDirName = sourceDirName;
		try {
			Log.debug("Processing " + getSourceXML().getName());
			// ---- Read XML file ----
			SAXBuilder builder = new SAXBuilder();
			Document doc = builder.build(getSourceXML()); // <XmlFile>
			Element root = doc.getRootElement();

			// ---- Walk through XML data ----
			int rootChildren = root.getChildren().size();
			for (int i = 0; i < rootChildren; i++) {
				if (root.getChildren().get(i).getName().equals("body")) {
					for (Element element : root.getChildren().get(i).getChildren()) {
						if (element.getName().equals("spreadsheet")) {
							long numberOfTables = element.getChildren().stream().filter(el -> el.getName().equals("table")).count();
							int index = position == null ? toIntExact(numberOfTables)-1 : position > 0 ? position-1 : position;
							element.getChildren().get(index).removeContent();
							element.getChildren().get(index).addContent(newSheet);
						}
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
	}
	
	public List<Content> extractSheet(String sourceDirName, Integer position) {
		this.targetDirName = sourceDirName;
		try {
			Log.debug("Processing " + getSourceXML().getName());
			// ---- Read XML file ----
			SAXBuilder builder = new SAXBuilder();
			Document doc = builder.build(getSourceXML()); // <XmlFile>
			Element root = doc.getRootElement();

			// ---- Walk through XML data ----
			int rootChildren = root.getChildren().size();
			for (int i = 0; i < rootChildren; i++) {
				if (root.getChildren().get(i).getName().equals("body")) {
					for (Element element : root.getChildren().get(i).getChildren()) {
						if (element.getName().equals("spreadsheet")) {
							long numberOfTables = element.getChildren().stream().filter(el -> el.getName().equals("table")).count();
							int index = position == null ? toIntExact(numberOfTables)-1 : position > 0 ? position-1 : position;
							return element.getChildren().get(index).cloneContent();
						}
					}
				}
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
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
