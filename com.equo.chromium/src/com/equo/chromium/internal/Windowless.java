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


package com.equo.chromium.internal;

import java.util.concurrent.ExecutionException;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefMessageRouter;

import com.equo.chromium.swt.internal.AbstractEval;
import com.equo.chromium.swt.internal.EvalSimpleImpl;

public final class Windowless extends IndependentBrowser {

	private CefMessageRouter router;

	public Windowless(String url) {
		Engine.initCEF();
		createClient();
		setBrowser(getClientHandler().createBrowser(url, true, false, createRequestContext()));
		getBrowser().createImmediately();
	}
	
	@Override
	protected void createClient() {
		super.createClient();
		router = AbstractEval.createRouter();
		getClientHandler().addMessageRouter(router);
	}

	@Override
	public Object evaluate(String script) {
		CefBrowser browser = getBrowser();
		try {
			EvalSimpleImpl eval = new EvalSimpleImpl(browser, router, "");
			return eval.eval(script, getCreated());
		} catch (InterruptedException e) {
			throw new RuntimeException("Script that was evaluated failed");
		} catch (ExecutionException e) {
			throw (RuntimeException)e.getCause();
		}
	}
	
	@Override
	public boolean close() {
		getClientHandler().removeMessageRouter(router);
		router.dispose();
		return super.close();
	}

}