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

import static com.equo.chromium.internal.Engine.debug;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.util.Stack;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.cef.CefApp;
import org.cef.browser.CefBrowser;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.widgets.Display;

import com.equo.chromium.swt.BrowserFunction;

public class EvalFileImpl extends AbstractEval {
	private static enum MsgType {
		BfCall(5), BfRet(1), BfRetEx(3), Eval(2);

		private String type;

		MsgType(int type) {
			this.type = String.valueOf(type);
		}

		String str() {
			return type;
		}
	}
	private static final String EVAL_FILE_NAME = "equochromium";
	private CefBrowser cefBrowser;
	private Chromium chromium;
	private RandomAccessFile raf;
	private Stack<CompletableFuture<Object>> received = new Stack<CompletableFuture<Object>>();
	private String id;

	public EvalFileImpl(Chromium chromium, CefBrowser cefBrowser) {
		this.chromium = chromium;
		this.cefBrowser = cefBrowser;
		id = Integer.toString(new Random().nextInt());
	}

	@Override
	public Object eval(String script, CompletableFuture<Boolean> created) throws InterruptedException, ExecutionException {
		Display display = Display.getCurrent();
		String eval = getEvalFunction(id, script, "return req;");

		CompletableFuture<Object> received = this.received.push(new CompletableFuture<>());
		if (this.received.size() == 1) {
			created.thenRun(() -> {
				try {
					CefApp.getInstance().doMessageLoopWork(-1);
					final File file = createTmpFile();
					startFileWatcher(display, file, id);
					cefBrowser.sendEvalMessage(file.getAbsolutePath(), eval);
				} catch (IOException e) {
					finish(display, null, new SWTException(e.getMessage()));
				}
			});
		} else { // Eval
			writeEvalMessage(eval);
		}

		awaitCondition(display, received, true);

		return received.get();
	}

	protected void writeEvalMessage(String eval) {
		String encodeEval = Chromium.encodeType(eval);
		try {
			raf.writeBytes(MsgType.Eval.str()); // Eval
			raf.writeBytes("\n"+encodeEval+"\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void startFileWatcher(Display display, final File file, String id) {
		Thread t = new Thread(() -> {
			long timeOut = System.currentTimeMillis() + 60000;
			try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
				this.raf = raf;
				while (!received.isEmpty()) {
					long offset = raf.getFilePointer();
					String type = raf.readLine();
					if (type != null) {
						String line = raf.readLine();
						if (line == null) {
							raf.seek(offset);
							continue;
						}
						try {
							Object[] payload = ((Object[]) decodeType(line, type.equals(MsgType.BfCall.str()) ? SWT.ERROR_INVALID_ARGUMENT : SWT.ERROR_INVALID_RETURN_VALUE));
							if (payload.length == 3) { // BF call
								Integer index = Integer.valueOf((String) payload[1]);
								String token = (String) payload[0];
								Object[] args = (Object[]) payload[2];
								BrowserFunction browserFunction = chromium.functions.get(index);
								if (browserFunction != null && browserFunction.token.equals(token)) {
									debug("eval calling function: "+browserFunction.getName());
									Chromium.asyncExec(() -> {
										Object ret = null;
										try {
											ret = browserFunction.function(args);
											String encodeType = Chromium.encodeType(ret);
											raf.writeBytes(MsgType.BfRet.str()); // BF ret
											raf.writeBytes("\n"+encodeType+"\n");
										} catch(Throwable e) {
											try {
												raf.writeBytes(MsgType.BfRetEx.str()); // BF ret
												raf.writeBytes("\n"+e.toString()+"\n");
											} catch (IOException e1) {
												e1.printStackTrace();
											}
										}
									});
								}
							}
							else if (payload.length == 2) { // Eval return
								if (id.equals(payload[0])) {
									finish(display, file, payload[1]);
								}
							}
						} catch (SWTException e1) {
							if (type.equals(MsgType.BfCall.str()) && e1.code == SWT.ERROR_INVALID_ARGUMENT) {
								raf.writeBytes(MsgType.BfRetEx.str()); // BF ret
								raf.writeBytes("\n"+e1.toString()+"\n");
							} else {
								finish(display, file, e1);
							}
						}
					} else {
						Thread.sleep(30);
					}
					if (System.currentTimeMillis() > timeOut) {
						finish(display, file, new SWTException("Evaluate timeout exception"));
					}
				}
			} catch (Throwable throwable) {
				finish(display, file, throwable);
			} finally {
				this.raf = null;
				if (file.exists())
					file.delete();
			}
		}, "eval");
		t.setDaemon(true);
		t.start();
	}

	private void finish(Display display, File file, Object decodeType) {
		if (file != null)
			file.delete();
		if (decodeType instanceof SWTException)
			received.pop().completeExceptionally((SWTException)decodeType);
		else
			received.pop().complete(decodeType);
		if (display != null) 
			display.wake();
	}

	private File createTmpFile() throws IOException {
		Path dir = Paths.get(System.getProperty("java.io.tmpdir"), EVAL_FILE_NAME);
		try {
			Files.createDirectory(dir);
		} catch (IOException e2) {
			//Directory already exists;
		}

		File file = Files.createTempFile(dir, "eval", null).toFile();
		return file;
	}
}
