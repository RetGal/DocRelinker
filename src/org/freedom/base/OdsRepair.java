package org.freedom.base;

import org.freedom.log.Log;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.output.support.AbstractXMLOutputProcessor;
import org.jdom2.output.support.FormatStack;
import org.jdom2.output.support.XMLOutputProcessor;

import java.io.*;

class OdsRepair extends DocumentRelinker {

	public OdsRepair(File sourceXML, File targetXML) throws IllegalArgumentException {
		super(sourceXML, targetXML);
	}

	private String targetDirName;

	public void relink(String sourceDirName) {
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
	}

	private void walkThrough(Element element) throws UnsupportedEncodingException {
		for (Element el : element.getChildren()) {
			el.getAttributes().stream().filter(at -> at.getName().equals("formula") || at.getName().equals("href") || at.getName().equals("name")).filter(at -> (at.getValue().startsWith("of:") || at.getValue().startsWith("smb:") || at.getValue().startsWith("file:") || at.getValue().startsWith("'file:")) && at.getValue().contains("01-PCTS-Master.ods")).forEach(at -> {
				// handle regular file links
				String targetString = handleLink(at.getValue(), targetDirName);
				// ---- Modify XML data ----
				at.setValue(targetString);
			});
			if (el.getChildren().size() > 0) {
				walkThrough(el);
			}
		}
	}

	/**
	 * handles regular ODS links
	 */
	private String handleLink(String link, String targetDirName) {
		if (link == null || targetDirName == null) {
			throw new IllegalArgumentException("link and target dir must not be null");
		}

		StringBuilder fixed = new StringBuilder();
		if (link.contains("://")) {
			link = link.replace("davs://files.", "file://");
			link = link.replace("of:=['smb://", "of:=['file://");
			link = link.replace("smb://", "of:file://");
			LinkHelper helper = new LinkHelper(link, 0, "01-PCTS-Master.ods", fixed);
			fixed = fixLink(helper);
		}
		// set cleaned original filename including its path
		Log.debug("Fixed: " + fixed);
		return fixed.toString();
	}

	private class LinkHelper {
		public String link;
		public int offset;
		public String targetFileName;
		public StringBuilder fixed;

		public LinkHelper(String link, int offset, String targetFileName, StringBuilder fixed) {
			this.link = link;
			this.offset = offset;
			this.targetFileName = targetFileName;
			this.fixed = fixed;
		}
	}

	private StringBuilder fixLink(LinkHelper helper) {
		if (helper.link.indexOf("file://", helper.offset) >= 0) {
			int a = helper.link.indexOf("file://", helper.offset);
			int o = helper.link.indexOf(helper.targetFileName, a);
			helper.fixed.append(helper.link.substring(helper.offset, a));
			helper.fixed.append(targetDirName);
			helper.fixed.append(File.separator);
			a = helper.link.indexOf("'", o);
			if (a >= 0) {
				helper.fixed.append(helper.link.substring(o, a));
				helper.offset = a;
			} else {
				helper.fixed.append(helper.link.substring(o, helper.link.length()));
				helper.offset = helper.link.length();
			}
		} else {
			int a = helper.link.indexOf("'", helper.offset);
			int o = helper.link.indexOf(helper.targetFileName, a);
			if (o >= 0) {
				helper.fixed.append(helper.link.substring(a, o));
				a = helper.link.indexOf("'", o);
				helper.fixed.append(helper.link.substring(o , a));
				helper.offset = a;
			} else {
				helper.fixed.append(helper.link.substring(a, helper.link.length()));
				return helper.fixed;
			}
		}

		if (helper.link.indexOf("'", helper.offset) >= 0) {
			fixLink(helper);
		}
		return helper.fixed;
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
