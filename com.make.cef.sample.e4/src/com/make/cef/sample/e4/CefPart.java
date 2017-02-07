 
package com.make.cef.sample.e4;

import javax.inject.Inject;
import javax.annotation.PostConstruct;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import com.make.swtcef.Chromium;

import javax.annotation.PreDestroy;
import org.eclipse.e4.ui.di.Focus;

public class CefPart {
	private Chromium browser;

	@Inject
	public CefPart() {
		
	}
	
	@PostConstruct
	public void postConstruct(Composite parent) {
//		parent.setLayout(new FillLayout());
		
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		
		Text text = new Text(composite, SWT.BORDER | SWT.SEARCH);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		browser = new Chromium(composite, SWT.NONE);
//		browser = new Chromium(parent, SWT.NONE);
		browser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	}
	
	
	@PreDestroy
	public void preDestroy() {
		System.err.println("part preDestroy");
	}
	
	
	@Focus
	public void onFocus() {
		System.err.println("part onfocus");
//		browser.setFocus();
	}
	
}