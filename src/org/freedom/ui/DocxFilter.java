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

		String extension = getExtension(f);
		return extension != null && extension.equals(DOCX);

	}
	
    /*
     * Get the extension of a file.
     */  
    private static String getExtension(File f) {
    	
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');

        if (i > 0 &&  i < s.length() - 1) 
        {
            ext = s.substring(i+1).toLowerCase();
        }
        return ext;
    }

}
