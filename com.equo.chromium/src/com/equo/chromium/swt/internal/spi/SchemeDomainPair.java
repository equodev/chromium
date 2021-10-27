package com.equo.chromium.swt.internal.spi;

public class SchemeDomainPair {

	private String scheme;
	private String domain;

	private SchemeDomainPair(String scheme, String domain) {
		this.scheme = scheme;
		this.domain = domain;
	}

	public static SchemeDomainPair of(String scheme, String domain) {
		return new SchemeDomainPair(scheme, domain);
	}

	public String getScheme() {
		return scheme;
	}

	public String getDomain() {
		return domain;
	}

}
