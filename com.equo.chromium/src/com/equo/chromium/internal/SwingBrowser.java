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

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
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

import org.cef.CefApp;
import org.cef.CefApp.CefAppState;
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
import org.cef.handler.CefKeyboardHandler.CefKeyEvent;
import org.cef.handler.CefKeyboardHandlerAdapter;
import org.cef.handler.CefMessageRouterHandlerAdapter;
import org.cef.misc.BoolRef;

import com.equo.chromium.swt.internal.MimeTypeLinux;

public class SwingBrowser extends IndependentBrowser {
	private boolean ignoreFirstFocus = true;
	private CefMessageRouter writeText = null;
	private CefMessageRouter readText = null;
	private CefMessageRouterHandlerAdapter readTextHandler = null;
	private CefMessageRouterHandlerAdapter writeTextHandler = null;
	private WindowAdapter windowAdapter = null;
	private Window parentWindow = null;
	private boolean isFullscreen = false;

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
					Vector<String> acceptFilters, Vector<String> acceptExtensions, Vector<String> acceptDescriptions,
					CefFileDialogCallback callback) {
				return SwingBrowser.onFileDialog(browser, mode, title, defaultFilePath, acceptFilters,
						/* selectedAcceptFilter */ 0, callback);
			}
		});
		getClientHandler().addDownloadHandler(new CefDownloadHandlerAdapter() {
			@Override
			public boolean onBeforeDownload(CefBrowser browser, CefDownloadItem downloadItem, String suggestedName,
					CefBeforeDownloadCallback callback) {
				callback.Continue("", true);
				return true;
			}
		});
		getClientHandler().addJSDialogHandler(new CefJSDialogHandlerAdapter() {
			@Override
			public boolean onJSDialog(CefBrowser browser, String origin_url, JSDialogType dialog_type,
					String message_text, String default_prompt_text, CefJSDialogCallback callback,
					BoolRef suppress_message) {
				return openJsDialog(dialog_type, origin_url, message_text, default_prompt_text, default_prompt_text,
						callback);
			}
		});
		if (OS.isMacintosh()) {
			getClientHandler().addKeyboardHandler(new CefKeyboardHandlerAdapter() {
				@Override
				public boolean onKeyEvent(CefBrowser browser, CefKeyEvent event) {
					return SwingBrowser.this.onKeyEvent_(browser, event);
				}
			});
		}
		setBrowser(getClientHandler().createBrowser(url, false, false));
		getBrowser().setReference(this);
		createClipboardRouters();
	}

	public boolean onKeyEvent_(CefBrowser browser, CefKeyEvent event) {
		if (!event.focus_on_editable_field && event.windows_key_code == 0x20) {
			// Special handling for the space character when an input element does not
			// have focus. Handling the event in OnPreKeyEvent() keeps the event from
			// being processed in the renderer. If we instead handled the event in the
			// OnKeyEvent() method the space key would cause the window to scroll in
			// addition to showing the alert box.
			return true;
		} else if (event.type == CefKeyEvent.EventType.KEYEVENT_RAWKEYDOWN && event.is_system_key) {
			// CMD+[key] is not working on a Mac.
			// This switch statement delegates the common keyboard shortcuts to the browser
			switch (event.unmodified_character) {
			case 'a':
				browser.getFocusedFrame().selectAll();
				break;
			case 'c':
				browser.getFocusedFrame().copy();
				break;
			case 'v':
				browser.getFocusedFrame().paste();
				break;
			case 'x':
				browser.getFocusedFrame().cut();
				break;
			case 'z':
				browser.getFocusedFrame().undo();
				break;
			case 'Z':
				browser.getFocusedFrame().redo();
				break;
			default:
				return false;
			}
			return true;
		}
		return false;
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

		isCreated().thenRun(() -> {
			// Close browser when window is closed
			parentWindow = SwingUtilities.windowForComponent(parentContainer);
			if (parentWindow != null) {
				windowAdapter = new WindowAdapter() {
					@Override
					public void windowClosed(WindowEvent windowEvent) {
						close();
						if (OS.isMacintosh() && CefApp.getState() != CefAppState.TERMINATED) {
							Window[] windows = Window.getWindows();
							if (windows.length == 0 || (windows.length == 1 && !windows[0].isShowing())) {
								CefApp ca = CefApp.getInstance();
								if (ca != null) {
									ca.dispose();
								}
							}
						}
						if (parentWindow != null)
							parentWindow.removeWindowListener(this);
					}
				};
				parentWindow.addWindowListener(windowAdapter);
				if (isFullscreen)
					setFullscreen(true);
			}
		});
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
		writeText = CefMessageRouter.create(new CefMessageRouterConfig("__writeText", "__writeTextCancel"));
		readText = CefMessageRouter.create(new CefMessageRouterConfig("__readText", "__readTextCancel"));
		writeTextHandler = new CefMessageRouterHandlerAdapter() {
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
						callback.success(request);
					} catch (Exception e) {
						e.printStackTrace();
						callback.failure(0, e.getMessage());
					}
				});
				return true;
			}
		};
		readTextHandler = new CefMessageRouterHandlerAdapter() {
			@Override
			public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent,
					CefQueryCallback callback) {
				SwingUtilities.invokeLater(() -> {
					Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
					try {
						Transferable transferable = cb.getContents(null);
						String text = (String) transferable.getTransferData(DataFlavor.stringFlavor);
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

	private static File[] openDialog(Component parent, FileDialogMode mode, String title, String defaultFilePath, String[] acceptFilters,
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
		if (defaultFilePath != null && !defaultFilePath.isEmpty()) {
			fc.setSelectedFile(new File(defaultFilePath));
		}
		Map<String, Integer> indexFilters = new HashMap<String, Integer>();
		FileFilter defaultFilter = null;
		int index = 1;
		if (acceptFilters != null && acceptFilters.length > 0) {
			for (String acceptFilter : acceptFilters) {
				indexFilters.put(acceptFilter, index++);
				FileFilter filter = new FileFilter() {

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
				};
				fc.addChoosableFileFilter(filter);
				if (defaultFilePath != null) {
					String extension = acceptFilter.contains("*.")
							? acceptFilter.substring(acceptFilter.lastIndexOf("*.") + 1)
							: acceptFilter.replace("*", "");

					if (defaultFilePath.toLowerCase().endsWith(extension.toLowerCase())) {
						defaultFilter = filter;
					}
				}
			}
		}

		if (defaultFilter != null) {
			fc.setFileFilter(defaultFilter);
		}

		int returnVal = fc.showDialog(parent, null);

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
		String[] acceptFiltersArray = MimeTypeLinux.getExtensions(acceptFilters);
		javax.swing.SwingUtilities.invokeLater(() -> {
			AtomicInteger filterIndex = new AtomicInteger(-1);
			Component parentComponent = null;
			if (browser != null && browser.getUIComponent() instanceof Component) {
				parentComponent = (Component) browser.getUIComponent();
			}
			File[] files = openDialog(parentComponent, mode, title, defaultFilePath, acceptFiltersArray, filterIndex);
			if (files != null) {
				Vector<String> filePaths = new Vector<String>();
				for (File file : files) {
					filePaths.add(file.getAbsolutePath().toString());
				}
				filterIndex.set(
						(filterIndex.get() < 0 || filterIndex.get() >= acceptFiltersArray.length) ? selectedAcceptFilter
								: filterIndex.get());
				callback.Continue(/* filterIndex.get(), */ filePaths);
			} else {
				callback.Cancel();
			}
		});
		return true;
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

	@Override
	protected void disposeInBrowser() {
		if (writeText != null) {
			writeText.removeHandler(writeTextHandler);
			writeTextHandler = null;
			writeText.dispose();
		}
		if (readText != null) {
			readText.removeHandler(readTextHandler);
			readTextHandler = null;
			readText.dispose();
		}
		if (parentWindow != null && windowAdapter != null) {
			parentWindow.removeWindowListener(windowAdapter);
		}
		parentWindow = null;
		windowAdapter = null;
	}

	@Override
	public void setFullscreen(boolean fullscreen) {
		isFullscreen = fullscreen;
		if (parentWindow == null)
			return;

		GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		device.setFullScreenWindow(fullscreen ? parentWindow : null);
	}

	@Override
	public boolean isFullscreen() {
		if (parentWindow == null)
			return false;

		GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		return device.getFullScreenWindow() == parentWindow;
	}

}
