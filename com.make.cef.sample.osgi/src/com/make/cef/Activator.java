package com.make.cef;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	public static BundleContext Default;

	@Override
	public void start(BundleContext context) throws Exception {
		Default = context;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
	}

}
