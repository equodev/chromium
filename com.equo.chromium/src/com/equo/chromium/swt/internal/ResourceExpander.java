package com.equo.chromium.swt.internal;

import java.io.File;
import java.util.Arrays;
import java.util.Optional;

public class ResourceExpander {

	public static void setExecutable(File newFile) {
		String[] executables = new String[] {"", "so"};
		if (Arrays.asList(executables).contains(getExtension(newFile.getName())) && !newFile.canExecute()) {
			try {
				newFile.setExecutable(true);
//				Runtime.getRuntime ().exec (new String []{"chmod", permision, path}).waitFor(); //$NON-NLS-1$
			} catch (Throwable e) {
				System.err.println(e.getMessage());
			};
		}
	}
	
	public static String getExtension(String filename) {
		Optional<String> ext = Optional.ofNullable(filename).filter(f -> f.contains("."))
				.map(f -> f.substring(filename.lastIndexOf(".") + 1));
		return ext.isPresent() ? ext.get() : "";
	}
}
