package org.freedom.base;

import java.io.File;
import java.util.Set;

abstract class DocumentRelinker {

	private final File sourceXML;
	private final File targetXML;

	DocumentRelinker(File sourceXML, File targetXML) throws IllegalArgumentException {

		if (sourceXML == null || sourceXML.length() == 0) {
			throw new IllegalArgumentException("source XML must be specified");
		}
		if (targetXML == null || targetXML.length() == 0) {
			throw new IllegalArgumentException("target XML must be specified");
		}
		this.sourceXML = sourceXML;
		this.targetXML = targetXML;
	}

	public abstract Set<String> relink(String relatedDir);

	File getSourceXML() {
		return sourceXML;
	}

	File getTargetXML() {
		return targetXML;
	}
}
