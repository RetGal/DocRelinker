package org.freedom.ui;

import java.io.File;

class Utils {

    /*
     * Get the extension of a file.
     */  
    public static String getExtension(File f) {
    	
        String s = f.getName();
        int i = s.lastIndexOf('.');

        if (i > 0 &&  i < s.length() - 1) 
        {
            return s.substring(i+1).toLowerCase();
        }
        return null;
        
    }
}
