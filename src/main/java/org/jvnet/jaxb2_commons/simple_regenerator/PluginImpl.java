package org.jvnet.jaxb2_commons.simple_regenerator;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.sun.tools.xjc.BadCommandLineException;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.Outline;

/**
 * {@link Plugin} that preserves user written code using simple magic tags.
 * 
 * Limitations:<br>
 * - only a single preserved block per class<br>
 * - no support for nested classes<br>
 * - magic comments must start at first char of line<br>
 *
 * @author Juraj Vitko
 */
public class PluginImpl extends Plugin {
	
	private Pattern magicPattern;
	private final static String magicTag = "\n//--simple--preserve\n";

    public String getOptionName() {
        return "simple-preserve";
    }

    public String getUsage() {
        return "  -simple-preserve    :  preserve user written code enclosed in \"'//--simple--preserve'\" comments.";
    }
    
    public void onActivated(Options opts) throws BadCommandLineException {
    	magicPattern = Pattern.compile("\\r?\\n//--simple--preserve\\r?\\n");
    }

    public boolean run(Outline model, Options opt, ErrorHandler errorHandler) throws SAXException {
    	
    	File javaFile = null;
    	try {
    	
	        for(ClassOutline co : model.getClasses()) {
	        	javaFile = new File(opt.targetDir, co.target.fullName().replaceAll("\\.", "/") + ".java");

	        	//System.out.println("simple_regenerator: [" 
	        		//+ javaFile.getAbsolutePath() + "] " + (javaFile.canRead() ? "exists" : "non-existing"));
	        	
	        	if(!javaFile.canWrite())
	        		continue;
	        	
	        	String preservedCode = getPreservedCode(javaFile);
	        	if(preservedCode != null) {
	        		System.out.println("simple_regenerator: preserved code in: [" + javaFile.getAbsolutePath() + "]");
	        		co.implClass.direct(preservedCode);
	        	}
	        }
	
	        return true;
    	}
    	catch(Exception e) {
    		errorHandler.error(new SAXParseException("Failed to write to "+ javaFile, null, e));
    		return false;
    	}
    }
    
    protected String getPreservedCode(File f) throws Exception {
		char arr[] = new char[(int) f.length()];
		FileReader fr = new FileReader(f);
		int read = fr.read(arr);
		fr.close();
		String fc = new String(arr, 0, read);
		//make backup copy
		FileWriter fw = new FileWriter(f.getAbsolutePath() + ".backup");
		fw.write(fc);
		fw.close();
		//get the code to be preserved
		Matcher magic = magicPattern.matcher(fc);
		if(!magic.find())
			return null;
		int idx1 = magic.end();
		if(!magic.find())
			throw new Exception();
		int idx2 = magic.start();
		return magicTag + fc.substring(idx1, idx2) + magicTag;
    }
}
