package com.equo.chromium.swt.internal.spi;

@FunctionalInterface
public interface RequestFilter {

	public void filterRequest(SchemeHandlerRequest request);

}
