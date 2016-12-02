package org.freedom.ui;

import java.io.File;
import javax.swing.filechooser.FileFilter;


class DocxFilter extends FileFilter {
	
	private final static String DOCX = "docx";

	@Override
	public String getDescription() {
		return "Word 2007+ Dokumente (docx)";
	}	
	
	@Override
	public boolean accept(File f) {

		if (f.isDirectory()) {
			return true;
		}

		String extension = Utils.getExtension(f);
		return extension != null && extension.equals(DOCX);

	}

}
