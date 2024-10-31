/****************************************************************************
**
** Copyright (C) 2024 Equo
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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.CodeSource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

public class CompatibleWithHostImpl implements CompatibleWithHost {
	private File logFile = new File(Paths
			.get(System.getProperty("user.home"), ".equo", "compatibility_"
					+ LocalDateTime.now().format(DateTimeFormatter.ofPattern("uuuu-MM-dd_HH-mm-ss-SSS")) + ".log")
			.toString());
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
			addToFile(logFile, "- The chromium jar location could not be determined. Error.", true);
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
					if (!canExecute(logFile, file.toFile())) {
						result.set(false);
					}
					return super.visitFile(file, attrs);
				}
			});
		} catch (IOException e) {
			addToFile(logFile, "- Permission check failed with error: " + e.getMessage(), true);
			return false;
		}
		return result.get();
	}

	private static boolean canExecute(File logFile, File newFile) {
		String[] executables = new String[] { "", "exe" };
		if (asList(executables).contains(ResourceExpander.getExtension(newFile.getName()))) {
			if (!newFile.canExecute()) {
				addToFile(logFile, "- The file '" + newFile.getName() + "' does not have execution permissions.", true);
				return false;
			} else {
				addToFile(logFile, "- The file '" + newFile.getName() + "' has execution permissions.", true);
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

		File parentDirectory = logFile.getParentFile();
		if (!parentDirectory.exists()) {
			if (parentDirectory.mkdirs()) {
				addToFile(logFile, "- Created .equo directory.", true);
			} else {
				addToFile(logFile, "- Failed to create .equo directory.", true);
			}
		}

		Path libsPath = verifyRequiredLibraries(result);
		if (result.isDone()) {
			return result;
		}

		addToFile(logFile, System.lineSeparator(), true);
		addToFile(logFile, "--------- Check execution permissions ---------", true);
		checkExecutionPermissions(Paths.get(libsPath.toString(), "chromium-" + Engine.CEFVERSION));
		addToFile(logFile, "--------- Check execution permissions ---------" + System.lineSeparator(), true);

		List<String> commandLine = generateCommandLine(libsPath);
		addToFile(logFile, "- CommandLine to execute: " + commandLine + System.lineSeparator(), true);
		ProcessBuilder processBuilder = new ProcessBuilder(commandLine);
		addEnvVarsToFile();

		try {
			processBuilder.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile));
			processBuilder.redirectError(ProcessBuilder.Redirect.appendTo(logFile));
			Process process = processBuilder.start();
			new Thread(() -> {
				try {
					int exitCode = process.waitFor();
					addToFile(logFile, "- Process exit with code: " + exitCode, true);
					result.complete("");
				} catch (InterruptedException e) {
					addToFile(logFile, "- Failure during the process. " + e.getMessage(), true);
					result.completeExceptionally(e);
				}
			}).start();
		} catch (IOException e) {
			addToFile(logFile, "- Failure to execute the process with error: " + e.getMessage(), true);
			result.completeExceptionally(e);
		}

		return result;
	}

	private void addEnvVarsToFile() {
		String[] envVars = {"LD_PRELOAD", "LD_LIBRARY_PATH", "GDK_BACKEND", "BREAKPAD_DUMP_LOCATION"};
		addToFile(logFile, "--------- Environment Variables ---------", true);
		for (String envVar : envVars) {
			addToFile(logFile, envVar + "=" + getVarOrEmpty(envVar), true);
		}
		addToFile(logFile, "--------- Environment Variables ---------" + System.lineSeparator(), true);
	}

	private String getVarOrEmpty(String name) {
		String envVar = System.getenv(name);
		return envVar != null ? envVar : "";
	}

	private Path verifyRequiredLibraries(CompletableFuture<String> result) {
		Path libsPath = null;
		try {
			addToFile(logFile, "--------- Verify required libraries  ---------", true);
			libsPath = Engine.findLibsPath();
			addToFile(logFile, "--------- End verify required libraries ---------", true);
		} catch (UnsatisfiedLinkError e) {
			addToFile(logFile, "- Error when verify required libraries : " + e.getMessage(), true);
			result.completeExceptionally(e);
		}
		return libsPath;
	}

	private List<String> generateCommandLine(Path libsPath) {
		String classpath = getCp();
		addToFile(logFile, "- Classpath: " + classpath, true);
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

	private static void addToFile(File logFile, String message, boolean apend) {
		try (FileWriter fw = new FileWriter(logFile.getAbsolutePath(), apend)) {
			fw.write(message + System.lineSeparator());
		} catch (IOException e) {
			System.out.println("An error occurred while saving the content in the " + logFile.getAbsolutePath()
					+ " file: " + e.getMessage());
		}
	}
}
