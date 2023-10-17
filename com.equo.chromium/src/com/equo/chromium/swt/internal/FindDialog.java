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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;

import com.equo.chromium.swt.internal.Chromium.ExtraApi;

public class FindDialog extends Dialog {

	private boolean isOpen = false;
	private ExtraApi extraApi;
	private Control browser;
	private Shell parent;
	private Shell shell;
	private Text find;
	private Button caseSensitive;
	private Point locationInBrowser;

	public FindDialog(ExtraApi extraApi, Shell shell) {
		super(shell);
		this.extraApi = extraApi;
	}

	public void open() {
		isOpen = true;
		browser = extraApi.getChromium().browser;
		parent = getParent();
		createShell();
		createFindText();
		createButtons();
		shell.pack();
		setInitialLocation(getBrowserLimits());
		locationInBrowser = Display.getCurrent().map(null, browser, shell.getLocation());
		shell.open();
		DragListener dragListener = new DragListener();
		shell.addMouseListener(dragListener);
		addFindListeners();
		BrowserDragListener browserDragListener = new BrowserDragListener();
		BrowserResizeListener browserResizeListener = new BrowserResizeListener();
		parent.addListener(SWT.Deiconify, browserDragListener);
		parent.addListener(SWT.Move, browserDragListener);
		browser.addListener(SWT.Resize, browserResizeListener);
		PaintListener paintListener = listenForReparenting();
		browser.addPaintListener(paintListener);
		Listener hideListener = createHideOrShowListener(false);
		Listener showListener = createHideOrShowListener(true);
		browser.getDisplay().addFilter(SWT.Hide, hideListener);
		browser.getDisplay().addFilter(SWT.Show, showListener);
		browser.addListener(SWT.Dispose, event -> {
			if (!shell.isDisposed()) {
				shell.close();
			}
		});
		shell.addListener(SWT.Close, event -> {
			isOpen = false;
			extraApi.getChromium().findDialog = null;
			shell.removeMouseListener(dragListener);
			parent.removeListener(SWT.Deiconify, browserDragListener);
			parent.removeListener(SWT.Move, browserDragListener);
			browser.removeListener(SWT.Resize, browserResizeListener);
			browser.getDisplay().removeFilter(SWT.Hide, hideListener);
			browser.getDisplay().removeFilter(SWT.Show, showListener);
			browser.removePaintListener(paintListener);
		});
	}

	private PaintListener listenForReparenting() {
		return new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				if (parent != browser.getShell() && parent.getBounds() != browser.getShell().getBounds() && !shell.isDisposed()) {
					shell.close();
				}
			}
		};
	}

	private void createButtons() {
		createButton(SWT.PUSH, "Ʌ").addListener(SWT.Selection, event -> findText(isNotShift(event.stateMask), caseSensitive.getSelection()));
		createButton(SWT.PUSH, "V").addListener(SWT.Selection, event -> findText(!isNotShift(event.stateMask), caseSensitive.getSelection()));
		caseSensitive = createButton(SWT.TOGGLE, "Aa");
		caseSensitive.addListener(SWT.Selection, event -> findText(caseSensitive.getSelection(), caseSensitive.getSelection()));
		createButton(SWT.PUSH, "✕").addListener(SWT.Selection, event -> shell.close());
	}

	private Button createButton(int style, String text) {
		Button button = new Button(shell, style);
		GridData gridData = new GridData(SWT.CENTER, SWT.CENTER, false, false);
		if ("cocoa".equals(SWT.getPlatform())) {
			gridData.heightHint = 35;
			gridData.widthHint = 40;
		}
		button.setLayoutData(gridData);
		button.setText(text);
		return button;
	}

	private Listener createHideOrShowListener(boolean show) {
		return event -> {
			if (isAncestor(event.widget, browser) && !shell.isDisposed()) {
				shell.setVisible(show ? browser.getVisible() : show);
			}
		};
	}

	private boolean isAncestor(Widget widget, Control browser) {
		while (browser.getParent() != null) {
			if (browser.getParent() == widget) {
				return true;
			}
			browser = browser.getParent();
		}
		return false;
	}

	private void createShell() {
		shell = new Shell(parent, SWT.NONE);
		GridLayout layout = new GridLayout(8, false);
		shell.setLayout(layout);
	}

	private void addFindListeners() {
		find.addModifyListener(e -> extraApi.find(find.getText(), true, caseSensitive.getSelection()));
		find.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR) {
					findText(isNotShift(e.stateMask), caseSensitive.getSelection());
				}
			}
		});
	}

	protected boolean isNotShift(int stateMask ) {
		return (stateMask & SWT.SHIFT) == 0;
	}

	protected void findText(boolean forward, boolean caseSensitive) {
		extraApi.find(find.getText(), forward, caseSensitive);
	}

	private void createFindText() {
		find = new Text(shell, SWT.BORDER);
		GridData gridData = new GridData(SWT.CENTER, SWT.CENTER, false, true);
		if ("cocoa".equals(SWT.getPlatform())) {
			gridData.grabExcessVerticalSpace = false;
			gridData.heightHint = 22;
		}
		gridData.widthHint = "cocoa".equals(SWT.getPlatform()) ? 170 : 150;
		find.setLayoutData(gridData);
	}

	private void setInitialLocation(Rectangle limits) {
		shell.setLocation((limits.x + browser.getBounds().width - shell.getBounds().width), limits.y);
	}

	private Rectangle getBrowserLimits() {
		return Display.getCurrent().map(browser.getParent(), null, browser.getBounds());
	}

	protected Point checkLocationWithinBrowser(Rectangle newLimits, Point shellLocation) {
		int maxX = newLimits.x + newLimits.width - shell.getBounds().width;
		int maxY = newLimits.y + newLimits.height - shell.getBounds().height;
		if (shellLocation.x < newLimits.x) {
			shellLocation.x = newLimits.x;
		} else if (shellLocation.x > maxX) {
			shellLocation.x = maxX;
		}
		if (shellLocation.y < newLimits.y) {
			shellLocation.y = newLimits.y;
		} else if (shellLocation.y > maxY) {
			shellLocation.y = maxY;
		}
		return shellLocation;
	}

	public boolean isOpen() {
		return isOpen;
	}

	class DragListener implements MouseListener, MouseMoveListener {
		private Point dragOffset;
	
		@Override
		public void mouseDown(MouseEvent e) {
			dragOffset = new Point(e.x, e.y);
			shell.addMouseMoveListener(this);
		}

		@Override
		public void mouseUp(MouseEvent e) {
			dragOffset = null;
			shell.removeMouseMoveListener(this);
		}

		@Override
		public void mouseMove(MouseEvent e) {
			if (dragOffset != null) {
				Point newLocation = new Point(e.x, e.y);
				newLocation = shell.toDisplay(newLocation);
				newLocation.x -= dragOffset.x;
				newLocation.y -= dragOffset.y;
				Rectangle limits = Display.getCurrent().map(browser.getParent(), null, browser.getBounds());
				newLocation = checkLocationWithinBrowser(limits, newLocation);
				shell.setLocation(newLocation);
				locationInBrowser = Display.getCurrent().map(null, browser, shell.getLocation());
			}
		}

		@Override
		public void mouseDoubleClick(MouseEvent e) {
		}
	}

	class BrowserDragListener implements Listener {
		@Override
		public void handleEvent(Event event) {
			Point shellLocation = Display.getCurrent().map(browser, null, locationInBrowser);
			shell.setLocation(shellLocation);
			checkShellVisibility(getBrowserLimits(), shell.getBounds());
		}
	}

	class BrowserResizeListener implements Listener {
		@Override
		public void handleEvent(Event event) {
			Point shellLocation = checkLocationWithinBrowser(getBrowserLimits(), shell.getLocation());
			shell.setLocation(shellLocation);
			locationInBrowser = Display.getCurrent().map(null, browser, shellLocation);
			checkShellVisibility(getBrowserLimits(), shell.getBounds());
		}
	}

	public void checkShellVisibility(Rectangle browserBounds, Rectangle shellBounds) {
		boolean shellWidthFitsInBrowser = browserBounds.x + browserBounds.width >= shellBounds.width;
		shellWidthFitsInBrowser = shellWidthFitsInBrowser ? Display.getCurrent().getBounds().width - browserBounds.x > shellBounds.width : false;
		boolean shellHeightFitsInBrowser = browserBounds.y + browserBounds.height >= shellBounds.height;
		shellHeightFitsInBrowser = shellHeightFitsInBrowser ? Display.getCurrent().getBounds().height - browserBounds.y > shellBounds.height : false;
		boolean shellFitsInResizedBrowser = shellBounds.height < browserBounds.height && shellBounds.width < browserBounds.width;
		shell.setVisible(shellWidthFitsInBrowser && shellHeightFitsInBrowser && shellFitsInResizedBrowser);
	}
}
