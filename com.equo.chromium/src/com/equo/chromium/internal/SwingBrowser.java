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

import java.awt.Container;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

import org.cef.OS;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.browser.CefMessageRouter.CefMessageRouterConfig;
import org.cef.callback.CefBeforeDownloadCallback;
import org.cef.callback.CefDownloadItem;
import org.cef.callback.CefFileDialogCallback;
import org.cef.callback.CefJSDialogCallback;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefDialogHandler;
import org.cef.handler.CefDialogHandler.FileDialogMode;
import org.cef.handler.CefDownloadHandlerAdapter;
import org.cef.handler.CefFocusHandlerAdapter;
import org.cef.handler.CefJSDialogHandler.JSDialogType;
import org.cef.handler.CefJSDialogHandlerAdapter;
import org.cef.handler.CefMessageRouterHandlerAdapter;
import org.cef.misc.BoolRef;

import com.equo.chromium.swt.internal.MimeTypeLinux;
import com.equo.chromium.utils.EventType;

public class SwingBrowser extends IndependentBrowser {
	private boolean ignoreFirstFocus = true;

	private void init(String url) {
		Engine.initCEF(Engine.BrowserType.SWING);
		createClient();
		getClientHandler().addFocusHandler(new CefFocusHandlerAdapter() {
			@Override
			public boolean onSetFocus(CefBrowser browser, FocusSource source) {
				if (ignoreFirstFocus && source == FocusSource.FOCUS_SOURCE_NAVIGATION) {
					ignoreFirstFocus = false;
					return true;
				}
				return false;
			}

			@Override
			public void onGotFocus(CefBrowser browser) {
				if (OS.isWindows()) {
					KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
					browser.setFocus(true);
				}
			}
		});
		getClientHandler().addDialogHandler(new CefDialogHandler() {
			@Override
			public boolean onFileDialog(CefBrowser browser, FileDialogMode mode, String title, String defaultFilePath,
					Vector<String> acceptFilters /*, int selectedAcceptFilter*/, CefFileDialogCallback callback) {
				return SwingBrowser.onFileDialog(browser, mode, title, defaultFilePath, acceptFilters, /*selectedAcceptFilter*/ 0,
						callback);
			}
		});
		getClientHandler().addDownloadHandler(new CefDownloadHandlerAdapter() {
			@Override
			public void onBeforeDownload(CefBrowser browser, CefDownloadItem downloadItem, String suggestedName,
					CefBeforeDownloadCallback callback) {
				((SwingBrowser) getBrowser().getReference()).onBeforeDownload(browser, downloadItem, suggestedName,
						callback);
			}
		});
		getClientHandler().addJSDialogHandler(new CefJSDialogHandlerAdapter() {
			@Override
			public boolean onJSDialog(CefBrowser browser, String origin_url, JSDialogType dialog_type,
					String message_text, String default_prompt_text, CefJSDialogCallback callback,
					BoolRef suppress_message) {
				return openJsDialog(dialog_type, origin_url, message_text, default_prompt_text, default_prompt_text, callback);
			}
		});
		setBrowser(getClientHandler().createBrowser(url, false, false));
		getBrowser().setReference(this);
		createClipboardRouters();
	}

	@Override
	public boolean setUrl(String url) {
		ignoreFirstFocus = true;
		return super.setUrl(url);
	}

	public SwingBrowser(Object parent, String layout, String url) {
		init(url);
		Container parentContainer = (Container) parent;
		parentContainer.add((JPanel) getBrowser().getUIComponent(), layout);

		// Close browser when window is closed
		Window parentWindow = SwingUtilities.windowForComponent(parentContainer);
		if (parentWindow != null) {
			parentWindow.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosed(WindowEvent windowEvent) {
					close();
					parentWindow.removeWindowListener(this);
				}
			});
		}
	}

	public SwingBrowser(String url) {
		init(url);
		getBrowser().createImmediately();
	}

	@Override
	public Object getUIComponent() {
		return getBrowser().getUIComponent();
	}

	private void createClipboardRouters() {
		CefMessageRouter writeText = CefMessageRouter
				.create(new CefMessageRouterConfig("__writeText", "__writeTextCancel"));
		CefMessageRouter readText = CefMessageRouter
				.create(new CefMessageRouterConfig("__readText", "__readTextCancel"));
		CefMessageRouterHandlerAdapter writeTextHandler = new CefMessageRouterHandlerAdapter() {
			@Override
			public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent,
					CefQueryCallback callback) {
				SwingUtilities.invokeLater(() -> {
					Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
					try {
						if (request == null || request.isEmpty()) {
							cb.setContents(new StringSelection(""), null);
						} else {
							StringSelection transfer = new StringSelection(request);
							cb.setContents(transfer, null);
						}
						Map<String, Object> mapData = new HashMap<>();
						mapData.put("text", request);
						notifySubscribers(EventType.onClipboardWriteText, mapData);
						callback.success(request);
					} catch (Exception e) {
						e.printStackTrace();
						callback.failure(0, e.getMessage());
					}
				});
				return true;
			}
		};
		CefMessageRouterHandlerAdapter readTextHandler = new CefMessageRouterHandlerAdapter() {
			@Override
			public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent,
					CefQueryCallback callback) {
				SwingUtilities.invokeLater(() -> {
					Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
					try {
						Transferable transferable = cb.getContents(null);
						String text = (String) transferable.getTransferData(DataFlavor.stringFlavor);
						Map<String, Object> mapData = new HashMap<>();
						mapData.put("text", text);
						notifySubscribers(EventType.onClipboardReadText, mapData);
						callback.success(text);
					} catch (Exception e) {
						e.printStackTrace();
						callback.failure(0, e.getMessage());
					}
				});
				return true;
			}
		};
		writeText.addHandler(writeTextHandler, true);
		readText.addHandler(readTextHandler, true);
		getBrowser().getClient().addMessageRouter(writeText);
		getBrowser().getClient().addMessageRouter(readText);
	};

	private static File[] openDialog(FileDialogMode mode, String title, String defaultFilePath, String[] acceptFilters,
			AtomicInteger selectedAcceptFilter) {

		int swingMode;
		String defaultDlgTitle;
		switch (mode) {
		case FILE_DIALOG_OPEN_MULTIPLE:
			swingMode = JFileChooser.CUSTOM_DIALOG;
			defaultDlgTitle = "Open Files";
			break;
		case FILE_DIALOG_SAVE:
			swingMode = JFileChooser.SAVE_DIALOG;
			defaultDlgTitle = "Save File";
			break;
		default:
			swingMode = JFileChooser.OPEN_DIALOG;
			defaultDlgTitle = "Open File";
			break;
		}

		String dlgTitle = title == null || title.isEmpty() ? defaultDlgTitle : title;

		final JFileChooser fc = new JFileChooser();
		fc.setDialogTitle(dlgTitle);
		fc.setDialogType(swingMode);

		Map<String, Integer> indexFilters = new HashMap<String, Integer>();
		int index = 1;
		if (acceptFilters != null && acceptFilters.length > 0) {
			for (String acceptFilter : acceptFilters) {
				indexFilters.put(acceptFilter, index++);
				fc.addChoosableFileFilter(new FileFilter() {

					@Override
					public String getDescription() {
						return acceptFilter;
					}

					@Override
					public boolean accept(File f) {
						if (f.isDirectory()) {
							return true;
						} else {
							return f.getName().toLowerCase().endsWith(acceptFilter.replace("*", ""));
						}
					}
				});
			}
		}

		int returnVal = fc.showDialog(null, null);

		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File[] files = fc.getSelectedFiles();
			File[] allFiles = new File[files.length + 1];
			for (int i = 0; i < files.length; i++) {
				allFiles[i] = files[i];
			}
			allFiles[files.length] = fc.getSelectedFile();
			Object selectedFilter = indexFilters.get(fc.getFileFilter().getDescription());
			if (selectedFilter != null) {
				selectedAcceptFilter.set((int) selectedFilter);
			}
			return allFiles;
		} else {
			return null;
		}
	}

	private static boolean onFileDialog(CefBrowser browser, FileDialogMode mode, String title, String defaultFilePath,
			Vector<String> acceptFilters, int selectedAcceptFilter, CefFileDialogCallback callback) {
		if (OS.isLinux()) {
			String[] acceptFiltersArray = MimeTypeLinux.getExtensions(acceptFilters);
			AtomicInteger filterIndex = new AtomicInteger(-1);
			File[] files = openDialog(mode, title, defaultFilePath, acceptFiltersArray, filterIndex);
			if (files != null) {
				Vector<String> filePaths = new Vector<String>();
				for (File file : files) {
					filePaths.add(file.getAbsolutePath().toString());
				}
				filterIndex.set(
						(filterIndex.get() < 0 || filterIndex.get() >= acceptFiltersArray.length) ? selectedAcceptFilter
								: filterIndex.get());
				((IndependentBrowser)browser.getReference()).notifySubscribers(EventType.onOpenFile);
				callback.Continue(/*filterIndex.get(),*/ filePaths);
			} else {
				((IndependentBrowser)browser.getReference()).notifySubscribers(EventType.onCancelOpenFile);
				callback.Cancel();
			}
			return true;
		}
		return false;
	}

	private static boolean openJsDialog(JSDialogType dialog_type, String title, String msg, String prompt,
			String default_prompt_text, CefJSDialogCallback callback) {
		if (OS.isLinux()) {
			int selected = 0;
			String userInput = default_prompt_text;
			switch (dialog_type) {
			case JSDIALOGTYPE_ALERT:
				selected = JOptionPane.showConfirmDialog(null, prompt != null ? msg + "\n\n" + prompt : msg, title,
						JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE);
				break;
			case JSDIALOGTYPE_CONFIRM:
				selected = JOptionPane.showConfirmDialog(null, prompt != null ? msg + "\n\n" + prompt : msg, title,
						JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
				break;
			case JSDIALOGTYPE_PROMPT:
				userInput = JOptionPane.showInputDialog(prompt != null ? msg + "\n\n" + prompt : msg);
				break;
			}
			callback.Continue(selected == JOptionPane.OK_OPTION && userInput != null, userInput);
			return true;
		}
		return false;
	}

	private void onBeforeDownload(CefBrowser browser, CefDownloadItem download_item, String suggestedName,
			CefBeforeDownloadCallback callback) {
		if (OS.isLinux()) {
			File[] files = openDialog(FileDialogMode.FILE_DIALOG_SAVE, null, null, null, null);
			if (files != null) {
				callback.Continue(files[0].getAbsolutePath(), false);
			}
		} else {
			callback.Continue("", true);
		}
	}
}
