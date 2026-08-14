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

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

import com.equo.chromium.ChromiumBrowser;

class WindowlessCompatibility {
	private static ChromiumBrowser browser = null;
	private static String logMessages = "";

	public static void main(String[] args) {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			System.out.println(System.lineSeparator() + "--------- Aplication status ---------" + System.lineSeparator()
					+ logMessages + "--------- End Aplication status ---------" + System.lineSeparator());
		}));
		printMessage(new String[] { "Init Aplication" }, "Done");
		printMessage(new String[] { "Before OnAfterCreated" }, "Done");

		printMessage(new String[] { "Start browser" }, "Done");
		browser = ChromiumBrowser.windowless("about:/version");
		browser.isCreated().thenRun(() -> {
			printMessage(new String[] { "OnAfterCreated" }, "Done");
			printMessage(new String[] { "Before OnLoadEnd" }, "Done");
			pollForLoadEnd();
		});

		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				printMessage(new String[] { "Process terminated by timeout" }, "Error");
				System.exit(1);
			}
		}, 8000);

		ChromiumBrowser.startBrowsers();
	}

	private static void pollForLoadEnd() {
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				String text;
				try {
					text = browser.text().get();
				} catch (InterruptedException | ExecutionException e) {
					printMessage(new String[] { "Error getting text from browser. " + e.getMessage() }, "Error");
					System.exit(1);
					return;
				}
				if (!text.contains("User Agent")) {
					pollForLoadEnd();
					return;
				}
				printMessage(new String[] { "OnLoadEnd", "Get browser text" }, "Done");
				printMessage(new String[] { "Browser text is not empty" }, "Done");
				printMessage(new String[] { "After getting browser text" }, "Done");
				browser.close();
				printMessage(new String[] { "OnBeforeExit with exitProcess=100" }, "Done");
				System.exit(100);
			}
		}, 200);
	}

	private static void printMessage(String[] messages, String type) {
		for (String message : messages) {
			System.out.println("Step: " + message);
			logMessages += "Step: " + message + ". " + type + System.lineSeparator();
		}
	}
}
