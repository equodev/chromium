/****************************************************************************
**
** Copyright (C) 2026 Equo
**
** This file is part of Equo Chromium.
**
** Commercial License Usage
** Licensees holding valid commercial Equo licenses may use this file in
** accordance with the commercial license agreement provided with the
** Software or, alternatively, in accordance with the terms contained in
** a written agreement between you and Equo. For licensing terms
** and conditions see https://www.equo.dev/terms.
**
** GNU General Public License Usage
** Alternatively, this file may be used under the terms of the GNU
** General Public License version 3 as published by the Free Software
** Foundation. Please review the following
** information to ensure the GNU General Public License requirements will
** be met: https://www.gnu.org/licenses/gpl-3.0.html.
**
****************************************************************************/
package com.equo.chromium.internal;

import static java.util.Arrays.asList;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.cef.OS;

public class ResourceExpander {

	public static final String DEFAULT_EXECUTABLE_NAME = OS.isMacintosh() ? "equochro Helper" : "equochro_helper";
	public static final String CUSTOM_EXECUTABLE_NAME = System.getProperty("chromium.executable.name",
			DEFAULT_EXECUTABLE_NAME);

	public static void setExecutable(File newFile) {
		String[] executables = new String[] { "", "so" };
		if (asList(executables).contains(getExtension(newFile.getName())) && !newFile.canExecute()) {
			try {
				newFile.setExecutable(true);
//				Runtime.getRuntime ().exec (new String []{"chmod", permision, path}).waitFor();
			} catch (Throwable e) {
				System.err.println(e.getMessage());
			}
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
				throw new RuntimeException("Failed to extract " + resource + "from jar");
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

	static Path extractFromJar(String chromiumPath, String arch, String subdir, URI location)
			throws IOException {
		Path extractPath = chromiumPath.isEmpty() ? Paths.get(System.getProperty("user.home"), ".equo", "chromium")
				: Paths.get(chromiumPath);
		extractPath = extractPath.resolve(arch);
		try (JarFile jarFile = new JarFile(new File(location))) {
			JarEntry entry = jarFile.getJarEntry(subdir + "/chromium.properties");
			if (entry != null) {
				try (InputStream is = jarFile.getInputStream(entry)) {
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
							if (Objects.equals(ver, oldVer)) {
								replace = false;
							}
						}
					}
					for (String prop : props.stringPropertyNames()) {
						 if (prop.matches("\\d+")) {
							String propValue = props.getProperty(prop);
							Path path = findResource(extractPath, propValue, replace);
							if (!CUSTOM_EXECUTABLE_NAME.equals(DEFAULT_EXECUTABLE_NAME) && isHelperExecutable(path)) {
								createExecutableWithCustomName(path);
							}
						}
					}
					if (replace) {
						try (InputStream propsIs = jarFile.getInputStream(entry)) {
							Files.copy(propsIs, oldFile, StandardCopyOption.REPLACE_EXISTING);
						}
					}
				}
				return extractPath;
			}
		}
		return null;
	}

	public static boolean isHelperExecutable(Path path) {
		String pathString = path.toString();
		if (OS.isMacintosh()) {
			String macPattern = ".*/equochro Helper( \\(.*\\))?\\.app/Contents/MacOS/equochro Helper( \\(.*\\))?";
			return pathString.matches(macPattern);
		} else {
			if (OS.isLinux()) {
				return DEFAULT_EXECUTABLE_NAME.equals(path.getFileName().toString());
			} else {
				return (DEFAULT_EXECUTABLE_NAME + ".exe").equals(path.getFileName().toString());
			}
		}
	}

	public static Path createExecutableWithCustomName(Path originalFile) {
		String pathString = originalFile.toString();
		int lastIndex = pathString.lastIndexOf(DEFAULT_EXECUTABLE_NAME);

		if (lastIndex != -1) {
			String newPathString = pathString.substring(0, lastIndex) + CUSTOM_EXECUTABLE_NAME
					+ pathString.substring(lastIndex + DEFAULT_EXECUTABLE_NAME.length());
			Path newPath = Paths.get(newPathString);

			try {
				if (OS.isMacintosh()) {
					Files.createSymbolicLink(newPath, originalFile);
				} else {
					Files.move(originalFile, newPath, StandardCopyOption.REPLACE_EXISTING);
				}
				return newPath;
			} catch (java.nio.file.FileAlreadyExistsException e) {
				return newPath;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return originalFile;
	}
}
