package org.freedom.base;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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

	File getSourceXML() {
		return sourceXML;
	}

	File getTargetXML() {
		return targetXML;
	}
	
	List<String> handleGDS(String link, String relatedDirName) {
		
		if (link == null || relatedDirName == null) {
			throw new IllegalArgumentException("link and related dir must not be null");
		}
		List<String> targetStrings = new ArrayList<>();
		// get what's between
		// "http://127.0.0.1:4664/redir?url=file%3A%2F%2F" and "%3F" (?)
		// http://127.0.0.1:4664/redir?url=file%3A%2F%2F length = 45
		String target = link.substring(45, link.lastIndexOf("%3F"));
		// set cleaned original filename including its path
		targetStrings.add(target);
		System.out.println("gds target: " + target);

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
		// set the new link to the file which will be valid after the file will have been moved
		targetStrings.add(targetFileName.toString());
		return targetStrings;
	}
	
}
