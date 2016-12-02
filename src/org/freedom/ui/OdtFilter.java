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
		String extension = Utils.getExtension(f);
		return extension != null && extension.equals(ODT);

	}

}
