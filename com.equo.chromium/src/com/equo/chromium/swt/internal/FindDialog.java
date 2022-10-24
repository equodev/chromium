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
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.equo.chromium.swt.internal.Chromium.ExtraApi;

public class FindDialog extends Dialog {

	private static boolean isOpen = false;
	private static Point location = null;
	private ExtraApi extraApi;

	public FindDialog(ExtraApi extraApi, Shell shell) {
		super(shell);
		this.extraApi = extraApi;
	}

	public void open() {
		Shell parent = getParent();
		Shell shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.MIN | SWT.MAX);
		shell.setText("Text search");
		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = 10;
		layout.marginWidth = 10;
		shell.setLayout(layout);

		Text find = new Text(shell, SWT.SINGLE | SWT.BORDER);

		GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gridData.horizontalSpan = 2;
		find.setLayoutData(gridData);

		Group direction = new Group(shell, SWT.NULL);
		direction.setText("Direction");
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		shell.setLayout(gridLayout);

		gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		direction.setLayout(gridLayout);
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gridData.horizontalSpan = 2;
		direction.setLayoutData(gridData);

		Button forward = new Button(direction, SWT.CHECK);
		Button backward = new Button(direction, SWT.CHECK);
		forward.setText("Forward");
		forward.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent event) {
				Button btn = (Button) event.getSource();
				if (backward.getSelection()) {
					backward.setSelection(false);
				}
				btn.setSelection(true);
			}
		});

		forward.setSelection(true);

		backward.setText("Backward");
		backward.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent event) {
				Button btn = (Button) event.getSource();
				if (forward.getSelection()) {
					forward.setSelection(false);
				}
				btn.setSelection(true);
			}
		});

		Button caseSensitive = new Button(shell, SWT.CHECK);
		caseSensitive.setText("Case sensitive");
		caseSensitive.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent event) {
				Button btn = (Button) event.getSource();
				extraApi.find(null, forward.getSelection(),
						btn.getSelection());
				extraApi.find(find.getText(), forward.getSelection(),
						btn.getSelection());
			}
		});

		Composite bar = new Composite(shell, SWT.NONE);
		bar.setLayoutData(new GridData(SWT.END, SWT.END, false, true, 2, 1));
		bar.setLayout(new GridLayout(2, true));

		Button closeButton = new Button(bar, SWT.PUSH);
		closeButton.setText("Close");
		closeButton.addListener(SWT.Selection, event -> {
			shell.close();
		});
		GridData closeData = new GridData(SWT.CENTER, SWT.END, false, false);
		closeData.widthHint = 80;
		closeButton.setLayoutData(closeData);

		Button findButton = new Button(bar, SWT.PUSH);
		findButton.setText("Find");
		findButton.addListener(SWT.Selection, event -> {
			extraApi.find(find.getText(), forward.getSelection(),
					caseSensitive.getSelection());
			find.forceFocus();
		});
		GridData findData = new GridData(SWT.CENTER, SWT.END, false, false);
		findData.minimumWidth = SWT.DEFAULT;
		findData.widthHint = 80;
		findButton.setLayoutData(findData);

		shell.addListener(SWT.Close, event -> {
			isOpen = false;
			extraApi.find(null, forward.getSelection(),
					caseSensitive.getSelection());
		});

		ModifyListener inputListener = e -> {
			String search = find.getText();
			extraApi.find(search, forward.getSelection(), caseSensitive.getSelection());
		};
		find.addModifyListener(inputListener);

		find.addKeyListener(new KeyAdapter() {

			@Override
			public void keyPressed(KeyEvent e) {
				if ( e.keyCode == SWT.CR) {
					String search = find.getText();
					extraApi.find(search, forward.getSelection(),
							caseSensitive.getSelection());
				}
			}
		});

		shell.addShellListener(new ShellAdapter() {
			@Override
			public void shellClosed(ShellEvent e) {
				location = shell.getLocation();
			}
		});

		if (location != null) {
			shell.setLocation(location);
		}

		find.forceFocus();
		shell.pack();
		isOpen = true;
		shell.open();

		// Sometimes you need to resize the dialog box after it becomes visible
		shell.pack();
	}

	public static boolean isOpen() {
		return isOpen;
	}
}
