/****************************************************************************
**
** Copyright (C) 2022 Equo
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
