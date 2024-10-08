package com.equo.chromium;

import java.util.concurrent.CompletableFuture;

public interface CompatibleWithHost {

	/**
	 * Configure the compatibility check for the windowless toolkit.
	 */
	public CompatibleWithHost windowless();

	/**
	 * Performs the toolkit check. Returns a completableFuture<String> completed
	 * with empty if the test was successful or exceptionally completed with error.
	 * The test details for both cases are generated in the
	 * ~/.equo/compatibility_<DATE>.log file.
	 */
	public CompletableFuture<String> check();
}
