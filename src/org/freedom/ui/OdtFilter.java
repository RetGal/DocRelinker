package org.freedom.ui;

import java.io.File;
import javax.swing.filechooser.FileFilter;

public class OdtFilter extends FileFilter {

	private final static String ODT = "odt";

	@Override
	public String getDescription() {
		return "Open Office Writer Dokumente (odt)";
	}

	@Override
	public boolean accept(File f) {

		if (f.isDirectory()) {
			return true;
		}

		String extension = getExtension(f);
		if (extension != null) {
			if (extension.equals(ODT)) {
				return true;
			} else {
				return false;
			}
		}

		return false;
	}

	/*
	 * Get the extension of a file.
	 */
	private static String getExtension(File f) {

		String ext = null;
		String s = f.getName();
		int i = s.lastIndexOf('.');

		if (i > 0 && i < s.length() - 1) {
			ext = s.substring(i + 1).toLowerCase();
		}
		return ext;
	}

}
