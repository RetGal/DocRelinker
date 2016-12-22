package org.freedom.base;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import org.freedom.log.Log;

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

	List<String> handleGDS(String link, String relatedDirName) throws UnsupportedEncodingException {

		if (link == null || relatedDirName == null) {
			throw new IllegalArgumentException("link and related dir must not be null");
		}
		List<String> targetStrings = new ArrayList<>();
		// get what's between
		// "http://127.0.0.1:4664/redir?url=file%3A%2F%2F" and "%3F" (?)
		// http://127.0.0.1:4664/redir?url=file%3A%2F%2F length = 45
		String target = URLDecoder.decode(link.substring(45, link.lastIndexOf("%3F")), "utf-8");
		// set cleaned original filename including its path
		targetStrings.add(target);
		Log.info("gds target: " + target);

		StringBuilder targetFileName = new StringBuilder(target.length());
		targetFileName.append(".\\").append(relatedDirName).append("\\");
		if (target.contains("\\")) {
			// C:\Folder\Subfolder\File.doc (\)
			targetFileName.append(target.substring(target.lastIndexOf("\\") + 1));
		} else if (target.contains(":")) {
			// C:File.doc (:)
			targetFileName.append(target.substring(target.indexOf(":") + 1));
		} else {
			targetFileName.append(target);
		}
		// set the new link to the file which will be valid after the file will
		// have been moved
		targetStrings.add(targetFileName.toString());
		return targetStrings;
	}

}
