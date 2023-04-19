package com.equo.chromium.internal;

import static java.util.Arrays.asList;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

public class ResourceExpander {

	public static void setExecutable(File newFile) {
		String[] executables = new String[] {"", "so"};
		if (asList(executables).contains(getExtension(newFile.getName())) && !newFile.canExecute()) {
			try {
				newFile.setExecutable(true);
//				Runtime.getRuntime ().exec (new String []{"chmod", permision, path}).waitFor();
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
	
	public static Path findResource(Path extractTo, String resource, boolean replace) {
		Path path = extractTo.resolve(resource);
		if (Files.exists(path) && !replace) {
			return path;
		} else {
			try {
				Files.createDirectories(path.getParent());
				if (extract(path, resource)) {
					if (Files.exists(path)) {
						return path;
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Failed to extract "+resource+ "from jar");
			}
		}
		throw new UnsatisfiedLinkError("Could not find resource " + resource);
	}

	static boolean extract(Path extractToFilePath, String resource) throws IOException {
		try (InputStream is = ResourceExpander.class.getResourceAsStream("/" + resource)) {
			if (is != null) {
				Files.copy(is, extractToFilePath, StandardCopyOption.REPLACE_EXISTING);
				setExecutable(extractToFilePath.toFile());
				return true;
			}
		}
		return false;
	}

	static Path extractFromJar(String chromiumPath, String arch, String subdir, Class<?> fragmentClass) throws IOException {
		Path extractPath = chromiumPath.isEmpty() ? Paths.get(System.getProperty("user.home"), ".equo", "chromium") : Paths.get(chromiumPath);
		extractPath = extractPath.resolve(arch);
		URL url = fragmentClass.getResource("/"+subdir+"/chromium.properties");
		if (url != null) {
			try (InputStream is = url.openStream()) {
				Properties props = new Properties();
				props.load(is);
				String ver = props.getProperty("version");
				boolean replace = true;
				Path oldFile = extractPath.resolve(subdir).resolve("chromium.properties");
				if (Files.exists(oldFile)) {
					Properties oldProps = new Properties();
					try (BufferedReader oldis = Files.newBufferedReader(oldFile)) {
						oldProps.load(oldis);
						String oldVer = oldProps.getProperty("version");
						if (Objects.equals(ver, oldVer))
							replace = false;
					}
				}
				for (String prop : props.stringPropertyNames()) {
					if (prop.length() == 2) { // XX keys only
						String propValue = props.getProperty(prop);
						findResource(extractPath, propValue, replace);
					}
				}
				if (replace) {
					try (InputStream propsIs = url.openStream()) {
						Files.copy(propsIs, oldFile, StandardCopyOption.REPLACE_EXISTING);
					}
				}
			}
			return extractPath;
		}
		return null;
	}
}
