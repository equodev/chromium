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

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;

import org.cef.browser.CefRequestContext;

import com.github.cliftonlabs.json_simple.JsonObject;
/*
 * This class is used for configure proxy using java system properties.
 */
public class Net {
	private static Net net = null;
	private String proxyConfig = "";

	public static void config() {
		if (net == null) {
			net = new Net();
		}
		net.setInBrowser();
	}

	private void setInBrowser() {
		// Use native proxy
		if (Boolean.getBoolean("java.net.useSystemProxies")) {
			setPreference("SYSTEM", "");
			return;
		}

		String preference = System.getProperty("chromium.proxy_pac_script", "");
		if (!preference.isEmpty()) {
			setPreference("pac_script", preference);
			return;
		}

		//Read proxy properties
		ArrayList<NetConfig> netConfigs = new ArrayList<NetConfig>();
		for (String scheme: new String[] {"http","https","socks"}) {
			if (Boolean.getBoolean(scheme + ".proxySet")) {
				String netHost = System.getProperty(scheme + ".proxyHost", "");
				String netPort = System.getProperty(scheme + ".proxyPort", "");
				if (!netHost.isEmpty() && !netPort.isEmpty()) {
					NetConfig netConfig = new NetConfig(scheme + ":", netHost + ":" + netPort, System.getProperty(scheme + ".nonProxyHosts", ""));
					netConfigs.add(netConfig);
				}
			}
		}

		if (!netConfigs.isEmpty()) {
			//Create script pac
			String pacFileContent = createPacFileContent(netConfigs);
			String base64PacFile = base64Encode(pacFileContent);

			setPreference("pac_script", "data:application/x-javascript-config;base64," + base64PacFile);
		} else if ("false".equals(System.getProperty("java.net.useSystemProxies", ""))) {
			setPreference("direct", "");
		}
	}

	private void setPreference(String mode, String preference) {
		CefRequestContext cefRequestContext = CefRequestContext.getGlobalContext();

		//Do not set the same preference
		if ((proxyConfig.isEmpty() || !proxyConfig.equals(mode + preference)) && cefRequestContext != null) {
			JsonObject dictionary = new JsonObject();
			dictionary.put("mode", mode);
			if ("pac_script".equals(mode)) {
				dictionary.put("pac_url", preference);
			}
			cefRequestContext.setPreference("proxy", dictionary.toJson());
			proxyConfig = mode + preference;
		}
	}

	private String base64Encode(String value) {
		try {
			return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8.toString()));
		} catch (UnsupportedEncodingException ex) {
			throw new RuntimeException(ex);
		}
	}

	private String createPacFileContent(ArrayList<NetConfig> netConfigs) {
		String stringFile = "function FindProxyForURL(url, host) {\n";
		for (NetConfig netConfig: netConfigs) {
			String scheme =  netConfig.scheme;
			String bypassList = netConfig.nonProxyHosts;
			String handleBypassList = "";

			if (!bypassList.isEmpty()) {
				handleBypassList = "        for (let address of \"" + bypassList + "\".split(\"|\")) {\n"
								+ "            if (url.substring(" + (scheme.length() + 2) + ").startsWith(address)) {\n"
								+ "                return \"DIRECT\";\n"
								+ "            }\n"
								+ "        }\n";
			}
			String schemeCondition = "socks:".equals(scheme) ?
					"    if (url.substring(0,4) ===  \"http\") {\n" :
						"    if (url.substring(0," + scheme.length() + ") ===  \"" + scheme + "\") {\n";
			stringFile+= schemeCondition
						+          handleBypassList
						+ "        return \"PROXY " + netConfig.address + "\";\n"
						+ "    }\n";
		}
		stringFile+= "}";
		return stringFile;
	}

	private class NetConfig {
		String scheme;
		String address;
		String nonProxyHosts;

		NetConfig(String scheme, String address, String nonProxyHosts){
			this.scheme = scheme;
			this.address = address;
			this.nonProxyHosts = nonProxyHosts;
		}
	}
}
