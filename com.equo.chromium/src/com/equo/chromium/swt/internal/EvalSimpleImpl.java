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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefMessageRouter;

public class EvalSimpleImpl extends AbstractEval {
	private String url;
	private CefMessageRouter router;
	private CefBrowser cefBrowser;

	public EvalSimpleImpl(CefBrowser cefBrowser, CefMessageRouter router, String url) {
		this.url = url;
		this.router = router;
		this.cefBrowser = cefBrowser;
	}

	@Override
	public Object eval(String script, CompletableFuture<Boolean> created)
			throws InterruptedException, ExecutionException {
		BiConsumer<String, String> function = (eval, url) -> cefBrowser.executeJavaScript(eval, url, 1);
		return executeEvalWithHandler(router, url, function, script, created);
	}
}
