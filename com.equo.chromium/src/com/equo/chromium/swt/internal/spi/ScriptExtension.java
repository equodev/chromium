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


package com.equo.chromium.swt.internal.spi;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

import org.cef.browser.CefMessageRouter;
import org.cef.browser.CefMessageRouter.CefMessageRouterConfig;

public interface ScriptExtension {
	public static String DISABLE_SCRIPT_EXTENSIONS_PROPERTY = "chromium.disable_script_extensions";

	public static Iterator<ScriptExtension> get() {
		ServiceLoader<ScriptExtension> serviceLoader = ServiceLoader.load(ScriptExtension.class,
				ScriptExtension.class.getClassLoader());
		return serviceLoader.iterator();
	}

	public static List<CefMessageRouter> createRouter(List<String> script) {
		final List<CefMessageRouter> scriptExtensions = new ArrayList<CefMessageRouter>();
		script.stream().forEach(nScript -> {
			CefMessageRouterConfig routerConfig = new CefMessageRouterConfig("__scriptExtension", nScript);
			scriptExtensions.add(CefMessageRouter.create(routerConfig));
		});
		return scriptExtensions;
	}

	public List<String> getScriptExtensions();

}
