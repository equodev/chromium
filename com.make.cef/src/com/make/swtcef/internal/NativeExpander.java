package com.make.swtcef.internal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class NativeExpander {
	
	private static final String JAVA_HOME = System.getProperty("java.home");
	
	public static String expand() {
		Properties osProps = new Properties();
		new Detector().detect(osProps, Collections.<String>emptyList());
		String bundleFolder = osProps.get(Detector.DETECTED_NAME) + "-" + osProps.get(Detector.DETECTED_ARCH);

		System.out.println("bundleFolder: " + bundleFolder);
		String propsFile = "/" + bundleFolder + "/files.properties";

		try {
			InputStream files = NativeExpander.class.getResourceAsStream(propsFile);
			if (files == null) {
				throw new RuntimeException("Could not load " + propsFile);
			}
			Properties props = new Properties();
			props.load(files);

			String cefVersion = props.getProperty("cefVersion");
			Path cefPath = Paths.get(System.getProperty("user.home"), ".swtcef", cefVersion);
			System.out.println("swtcef path: " + cefPath);

			if (shouldCopy(cefPath.resolve(bundleFolder), props)) {
				cefPath = Files.createDirectories(cefPath);
				for (String propName : props.stringPropertyNames()) {
					String filePath = props.getProperty(propName);
//					System.out.println(propName + ":" + filePath);
					if (!"cefVersion".equals(propName) && !"checksum".equals(propName)) {
						InputStream is = NativeExpander.class.getResourceAsStream("/" + filePath);
						if (is == null) {
							throw new RuntimeException("Could not load " + "/" + filePath);
						}
						copy(cefPath, filePath, is);
					}
				}
				System.out.println("Expanded CEF natives to " + cefPath);
		
				if (isWindows7() && (JAVA_HOME != null) && (!JAVA_HOME.isEmpty())){
					fixWin7Dll(cefPath.resolve(bundleFolder));
				}
				
				if (isLinux()){
					fixLinux(cefPath.resolve(bundleFolder));
				}
			}
			return cefPath.resolve(bundleFolder).toString();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}

	private static boolean shouldCopy(Path cefBundlePath, Properties props) {
		if (!Files.exists(cefBundlePath))
			return true;
		Path checksumPath = cefBundlePath.resolve("checksum");
		if (!Files.exists(checksumPath))
			return true;
		try {
			List<String> lines = Files.readAllLines(checksumPath);
			if (lines.isEmpty())
				return true;
			String prevCheckSum = lines.get(0);
			String checksum = props.getProperty("checksum");
			return !checksum.equals(prevCheckSum);
		} catch (IOException e) {
			return true;
		}
	}
	
	private static void fixLinux(Path cefrustPath){
		Path targetPath = Paths.get(JAVA_HOME, "bin");
		if (Files.exists(targetPath) && Files.isWritable(targetPath)) {
			copyPasteFileTo("icudtl.dat", cefrustPath, targetPath);
			copyPasteFileTo("natives_blob.bin", cefrustPath, targetPath);
		}
	}

	private static void copyPasteFileTo(String fileName, Path sourcePath, Path targetPath){
		Path originalFilePath = sourcePath.resolve(fileName);
		if (Files.exists(originalFilePath)){
			Path source = originalFilePath.toAbsolutePath();
			Path target = targetPath.resolve(fileName);
			try {
				Files.copy(source, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private static boolean isLinux() {
		return System.getProperty(Detector.DETECTED_NAME).contains("linux");
	}

	private static boolean isWindows() {
		return System.getProperty("os.name").toLowerCase().contains("windows");
	}

	private static boolean isWindows7(){
		return System.getProperty("os.name").equalsIgnoreCase("Windows 7");
	}

	private static void fixWin7Dll(Path path){
		Path dll = Paths.get(JAVA_HOME, "bin", "msvcr120.dll");
		if (Files.exists(dll)){
			Path source = dll.toAbsolutePath();
			Path target = path.resolve("msvcr120.dll");
			try {
				Files.copy(source, target);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private static void copy(Path cefPath, String filePath, InputStream is) throws IOException {
		Path pathFile = cefPath.resolve(filePath);
		Files.createDirectories(pathFile.getParent());
		File ex = pathFile.toFile();
		try (FileOutputStream os = new FileOutputStream(ex)) {
			ReadableByteChannel srcChannel = Channels.newChannel(is);

			for (long pos = 0L; is.available() > 0; pos += os.getChannel().transferFrom(srcChannel, pos,
					(long) Math.max(4096, is.available()))) {
				;
			}
			if (pathFile.endsWith("cefrust_subp")) {
				chmod("755", ex.toString());
			}
		} finally {
			is.close();
		}
	}
	
	static void chmod(String permision, String path) {
		if (isWindows()) return;
		try {
			Runtime.getRuntime ().exec (new String []{"chmod", permision, path}).waitFor(); //$NON-NLS-1$
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
}