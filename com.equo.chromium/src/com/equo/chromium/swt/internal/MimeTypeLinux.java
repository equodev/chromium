package com.equo.chromium.swt.internal;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class MimeTypeLinux {

	public static String[] getExtensions(Vector<String> mimeTypes){
		Map<String, Map<String, List<String>>> mimeTable = getMimeTypesFromSystem();

		List<String> extensionsArray = new ArrayList<String>();
		for (String fullMimeType : mimeTypes) {
			String af = fullMimeType;
			if (fullMimeType.startsWith(".")) {
				af = fullMimeType.replaceFirst(".", "*.");
				extensionsArray.add(af);
			} else if (!mimeTable.isEmpty()) {
				/**
				 * mimeTable is empty if there was an error trying to get it, the accepFilter is
				 * discarded
				 */
				String mimeType = fullMimeType.split("/")[0];
				if (mimeTable.containsKey(mimeType)) {
					Map<String, List<String>> mimeFound = mimeTable.get(mimeType);
					if (fullMimeType.contains("/*")) {
						for (List<String> mimeList : mimeFound.values()) {
							extensionsArray.addAll(mimeList);
						}
					} else {
						String subMimeType = fullMimeType.split("/")[1];
						if (mimeFound.containsKey(subMimeType)) {
							extensionsArray.addAll(mimeFound.get(subMimeType));
						}
					}
				}
			} 
		}
		extensionsArray.sort(null);
		return extensionsArray.toArray(new String[0]);
	}

	private static Map<String, Map<String, List<String>>> getMimeTypesFromSystem() {
		// Obtain all mimetypes from linux OS
		Map<String, Map<String, List<String>>> mimeTable = new HashMap<>();
		try {
			Path path = Paths.get("/usr/share/mime/globs");
			for (String line : Files.readAllLines(path)) {
				String[] lineSplit = line.split(":");
				if (lineSplit.length > 1) {
					String fullMimeType = line.split(":")[0];
					String mimeType = fullMimeType.split("/")[0];
					String subMimeType = fullMimeType.split("/")[1];
					String extension = line.split(":")[1];
					Map<String, List<String>> mimeAdd = mimeTable.computeIfAbsent(mimeType, m -> new HashMap<String, List<String>>());
					List<String> extensionAdd = mimeAdd.computeIfAbsent(subMimeType, m -> new ArrayList<String>());
					extensionAdd.add(extension);
				}
			}
		} catch (Exception e) {
		}
		return mimeTable;
	}
}
