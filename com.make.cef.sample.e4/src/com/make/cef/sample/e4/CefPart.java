 
package com.make.cef.sample.e4;

import javax.inject.Inject;
import javax.annotation.PostConstruct;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.events.*;

import com.make.swtcef.Chromium;

import javax.annotation.PreDestroy;
import org.eclipse.e4.ui.di.Focus;

public class CefPart {
	private Chromium browser;
	private Text text;

	@Inject
	public CefPart() {
		
	}
	
	@PostConstruct
	public void postConstruct(Composite parent) {
//		parent.setLayout(new FillLayout());
		
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		
		text = new Text(composite, SWT.BORDER | SWT.SEARCH);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		final Button ok = new Button(composite, SWT.PUSH);
		ok.setText("Go");

		browser = new Chromium(composite, SWT.NONE);
//		browser = new Chromium(parent, SWT.NONE);
		browser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		setUrl("about:gpu");

		ok.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				browser.setUrl(text.getText());
			}
		});
	}
	
	@PreDestroy
	public void preDestroy() {
		System.out.println("part preDestroy");
	}
	
	
	@Focus
	public void onFocus() {
		System.out.println("part onfocus");
//		browser.setFocus();
	}

	public void setUrl(String url) {
		text.setText(url);
		browser.setUrl(url);
	}
	
}