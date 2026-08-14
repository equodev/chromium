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
package com.equo.chromium.swt;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.cef.browser.CefBrowser;

public class Log {

	private static final Logger logger = Logger.getLogger("com.equo.chromium");
	static {
		configureLogger();
	}

	private static void configureLogger() {
		if (Boolean.getBoolean("chromium.debug")) {
			if (isLoggerNotConfigured(logger) && System.getProperty("java.util.logging.config.file", "").isEmpty()) {
				logger.setUseParentHandlers(false);
				ConsoleHandler consoleHandler = new ConsoleHandler();
				consoleHandler.setFormatter(customFormatter());
				consoleHandler.setLevel(Level.ALL);
				logger.setLevel(Level.ALL);
				logger.addHandler(consoleHandler);
				return;
			}
		} else {
			logger.setLevel(Level.OFF);
		}
	}

	public static boolean isLoggerNotConfigured(Logger logger) {
		if (logger != null) {
			Level level = logger.getLevel();
			boolean loggerLevelOff = level == Level.OFF || level == null;
			boolean noHandlers = logger.getHandlers().length == 0;
			boolean noFilter = logger.getFilter() == null;
			return logger.getUseParentHandlers() && noHandlers && loggerLevelOff && noFilter;
		}
		return true;
	}

	private static SimpleFormatter customFormatter() {
		return new SimpleFormatter() {
			@Override
			public synchronized String format(LogRecord record) {
				int id = (record.getParameters() != null) && record.getParameters().length > 0
						? (int) record.getParameters()[0] : -1;
				return String.format("%1$tF %1$tT.%1$tL %2$s%n",
						record.getMillis(), record.getMessage() + (id != -1 ? " id:" + id : ""));
			}
		};
	}

	public static void debug(String log) {
		log(log, -1);
	}

	public static void debug(String log, CefBrowser cefBrowser) {
		log(log, (cefBrowser != null ? cefBrowser.getIdentifier() : -1));
	}
	
	public static void log(String log, int id) {
		LogRecord record = new LogRecord(Level.FINE, log);
		record.setParameters(new Object[] {id});
		logger.log(record);
	}

}
