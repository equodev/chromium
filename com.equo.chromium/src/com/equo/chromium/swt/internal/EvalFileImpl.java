package com.equo.chromium.swt.internal;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.cef.CefApp;
import org.cef.browser.CefBrowser;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.widgets.Display;

public class EvalFileImpl extends AbstractEval {
	private static final String EVAL_FILE_NAME = "equochromium";
	private CefBrowser cefBrowser;

	public EvalFileImpl(CefBrowser cefBrowser) {
		this.cefBrowser = cefBrowser;
	}

	@Override
	public Object eval(String script, CompletableFuture<Boolean> created) throws InterruptedException, ExecutionException {
		String id = Integer.toString(new Random().nextInt());
		Display display = Display.getCurrent();
		String eval = getEvalFunction(id, script, "return req;");
		final File file = createTmpFile();

		CompletableFuture<Object> received = new CompletableFuture<>();
		try {
			WatchService ws = FileSystems.getDefault().newWatchService();
			Paths.get(file.getParent()).register(ws, StandardWatchEventKinds.ENTRY_MODIFY);
			created.thenRun(() -> {
				CefApp.getInstance().doMessageLoopWork(-1);
				startFileWatcher(display, file, received, ws, id);
				cefBrowser.sendEvalMessage(file.getAbsolutePath(), eval);
			});

			awaitCondition(display, received, true);
		} catch (IOException e) {
			new SWTException(e.getMessage());
		}

		return received.get();
	}

	private void startFileWatcher(Display display, final File file, CompletableFuture<Object> received,
			WatchService ws, String id) {
		Thread t = new Thread(() -> {
			long timeOut = System.currentTimeMillis() + 60000;
			try {
				WatchKey key = ws.take();
				while (!received.isDone()) {
					for (WatchEvent<?> event : key.pollEvents()) {
						if (StandardWatchEventKinds.ENTRY_MODIFY.equals(event.kind())
								&& file.getName().equals(event.context().toString())) {
							String evalContext = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())));
							file.delete();
							Object[] decodeType = ((Object[]) decodeType(evalContext, SWT.ERROR_INVALID_RETURN_VALUE));
							if (!id.equals(decodeType[0]))
								return;
							finish(display, received, ws, decodeType[1]);
						}
					}
					if (System.currentTimeMillis() > timeOut) {
						finish(display, received, ws, new SWTException("Evaluate timeout exception"));
					}
				}
			} catch (Throwable throwable) {
				finish(display, received, ws, throwable);
			}
		}, "eval");
		t.setDaemon(true);
		t.start();
	}

	private void finish(Display display, CompletableFuture<Object> received, WatchService ws, Object decodeType) {
		received.complete(decodeType);
		if (display != null) 
			display.wake();
		try {
			ws.close();
		} catch (IOException e) {}
	}

	private File createTmpFile() {
		Path dir = Paths.get(System.getProperty("java.io.tmpdir"), EVAL_FILE_NAME);
		try {
			Files.createDirectory(dir);
		} catch (IOException e2) {
			//Directory already exists;
		}

		try {
			File file = Files.createTempFile(dir, "eval", null).toFile();
			return file;
		} catch (IOException e) {
			new SWTException(e.getMessage());
		}
		return null;
	}
}
