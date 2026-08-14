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

import static java.util.Arrays.asList;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.cef.OS;

import com.equo.chromium.ChromiumBrowser;
import com.equo.chromium.CompatibleWithHost;
import static com.equo.chromium.swt.Log.debug;

public class CompatibleWithHostImpl implements CompatibleWithHost {
	private String toolkitClass = "com.equo.chromium.internal.WindowlessCompatibility";

	public CompatibleWithHost windowless() {
		return this;
	}

	private static String getJavaExecutablePath() {
		try {
			Class<?> processHandleClass = Class.forName("java.lang.ProcessHandle");
			Method currentMethod = processHandleClass.getMethod("current");
			Object currentProcessHandle = currentMethod.invoke(null);

			Method infoMethod = currentProcessHandle.getClass().getMethod("info");
			infoMethod.setAccessible(true);
			Object processHandleInfo = infoMethod.invoke(currentProcessHandle);

			Class<?> processHandleInfoClass = Class.forName("java.lang.ProcessHandle$Info");
			Method commandMethod = processHandleInfoClass.getMethod("command");
			Object optionalCommand = commandMethod.invoke(processHandleInfo);

			if (optionalCommand instanceof java.util.Optional) {
				java.util.Optional<?> optional = (java.util.Optional<?>) optionalCommand;
				if (optional.isPresent()) {
					String command = (String) optional.get();
					return command;
				}
			}
		} catch (Exception e) {

		}

		return getJavaExecutableFromJavaHome();
	}

	private static String getJavaExecutableFromJavaHome() {
		String javaHome = System.getProperty("java.home");
		String javaExec = javaHome + File.separator + "bin" + File.separator + "java";

		if (OS.isWindows()) {
			javaExec += ".exe";
		}

		return javaExec;
	}

	private String getCp() {
		CodeSource codeSource = ChromiumBrowser.class.getProtectionDomain().getCodeSource();

		if (codeSource != null) {
			URL jarLocation = codeSource.getLocation();
			String locationPath = jarLocation.getPath().replaceAll("/$", "");
			return locationPath.endsWith("/bin") || locationPath.endsWith(".jar") ? jarLocation.getPath()
					: jarLocation.getPath() + "bin";
		} else {
			debug("- The chromium jar location could not be determined. Error.");
		}
		return null;
	}

	private boolean checkExecutionPermissions(Path path) {
		AtomicBoolean result = new AtomicBoolean(true);
		try {
			Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if (!canExecute(file.toFile())) {
						result.set(false);
					}
					return super.visitFile(file, attrs);
				}
			});
		} catch (IOException e) {
			debug("- Permission check failed with error: " + e.getMessage());
			return false;
		}
		return result.get();
	}

	private static boolean canExecute(File newFile) {
		String[] executables = new String[] { "", "exe" };
		if (asList(executables).contains(ResourceExpander.getExtension(newFile.getName()))) {
			if (!newFile.canExecute()) {
				debug("- The file '" + newFile.getName() + "' does not have execution permissions.");
				return false;
			} else {
				debug("- The file '" + newFile.getName() + "' has execution permissions.");
			}
		}
		return true;
	}

	private static CompletableFuture<String> completeWithTimeout(long timeout, TimeUnit unit) {
		CompletableFuture<String> future = new CompletableFuture<>();

		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
		scheduler.schedule(() -> {
			if (!future.isDone()) {
				future.completeExceptionally(new Throwable("The check is terminated by timeout."));
			}
		}, timeout, unit);

		return future;
	}

	public CompletableFuture<String> check() {
		CompletableFuture<String> result = completeWithTimeout(10, TimeUnit.SECONDS);

		String userHome = System.getProperty("user.home");
		File equoDir = null;
		if (userHome != null) {
			equoDir = new File(userHome, ".equo");
			if (!equoDir.exists()) {
				if (equoDir.mkdirs()) {
					debug("- Created .equo directory.");
				} else {
					debug("- Failed to create .equo directory.");
				}
			}
		}

		Path libsPath = verifyRequiredLibraries(result);
		if (result.isDone()) {
			return result;
		}

		debug(System.lineSeparator());
		debug("--------- Check execution permissions ---------");
		checkExecutionPermissions(Paths.get(libsPath.toString(), "chromium-" + Engine.CEFVERSION));
		debug("--------- Check execution permissions ---------" + System.lineSeparator());

		List<String> commandLine = generateCommandLine(libsPath);
		debug("- CommandLine to execute: " + commandLine + System.lineSeparator());
		ProcessBuilder processBuilder = new ProcessBuilder(commandLine);
		addEnvVarsToFile();

		try {

			Process process = processBuilder.start();
			new Thread(() -> logOutputStream(process.getInputStream())).start();
			new Thread(() -> logOutputStream(process.getErrorStream())).start();

			new Thread(() -> {
				try {
					int exitCode = process.waitFor();
					debug("- Process exit with code: " + exitCode);
					result.complete("");
				} catch (InterruptedException e) {
					debug("- Failure during the process. " + e.getMessage());
					result.completeExceptionally(e);
				}
			}).start();
		} catch (IOException e) {
			debug("- Failure to execute the process with error: " + e.getMessage());
			result.completeExceptionally(e);
		}

		return result;
	}

	private void logOutputStream(InputStream outputStream) {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(outputStream))) {
			String line;
			while ((line = reader.readLine()) != null) {
				debug(line);
			}
		} catch (IOException e) {
			debug("- Error reading command line's process output: " + e.getMessage());
		}
	}

	private void addEnvVarsToFile() {
		String[] envVars = {"LD_PRELOAD", "LD_LIBRARY_PATH", "GDK_BACKEND", "BREAKPAD_DUMP_LOCATION"};
		debug("--------- Environment Variables ---------");
		for (String envVar : envVars) {
			debug(envVar + "=" + getVarOrEmpty(envVar));
		}
		debug("--------- Environment Variables ---------" + System.lineSeparator());
	}

	private String getVarOrEmpty(String name) {
		String envVar = System.getenv(name);
		return envVar != null ? envVar : "";
	}

	private Path verifyRequiredLibraries(CompletableFuture<String> result) {
		Path libsPath = null;
		try {
			debug("--------- Verify required libraries  ---------");
			libsPath = Engine.findLibsPath();
			debug("--------- End verify required libraries ---------");
		} catch (UnsatisfiedLinkError e) {
			debug("- Error when verify required libraries : " + e.getMessage());
			result.completeExceptionally(e);
		}
		return libsPath;
	}

	private List<String> generateCommandLine(Path libsPath) {
		String classpath = getCp();
		debug("- Classpath: " + classpath);
		String javaBin = getJavaExecutablePath();

		List<String> jvmArgs = new ArrayList<>();
		jvmArgs.add("-Dchromium.debug=true");
		jvmArgs.add("-Dchromium.path=" + libsPath);
		if (OS.isMacintosh()) {
			jvmArgs.add("-XstartOnFirstThread");
		}

		for (String propName : Engine.getPropertyNames()) {
			String propValue = System.getProperty(propName, "");
			if (!propValue.isEmpty() && !propName.startsWith("chromium.path") && !propName.startsWith("chromium.debug")
					&& !propName.startsWith("chromium.force_windowless_swt")) {
				jvmArgs.add("-D" + propName + "=" + propValue);
			}
		}

		jvmArgs.add("-Dchromium.cef version=" + Engine
				.getPropertiesFromResource("chromium-" + Engine.CEFVERSION + "/chromium.properties").get("version"));

		List<String> command = new ArrayList<>();
		command.add(javaBin);
		command.addAll(jvmArgs);
		command.add("-cp");
		command.add(classpath);
		command.add(toolkitClass);
		return command;
	}
}
