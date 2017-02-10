package com.make.cef;


import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.BundleContext;

import com.make.swtcef.Chromium;

public class CefOsgiApp implements IApplication {

    private static Composite left;
	private static Composite right;

	public static void main(String[] args) {
        final Display display = Display.getDefault();
        
        //Shell shell = display.getActiveShell();
		//if (shell == null) {
		//	shell = new Shell();
			// place it off so it's not visible
		//	shell.setLocation(0, 10000);
		//}

        Shell shell = new Shell(display);

        shell.setLayout(new FillLayout());
//        Composite composite = new Composite(shell, SWT.NONE);
        SashForm composite = new SashForm(shell, SWT.HORIZONTAL);
//        composite.setSashWidth(20);
        composite.setBackground(composite.getDisplay().getSystemColor( SWT.COLOR_GRAY));

//        GridLayout grid = new GridLayout(2, false);
//        composite.setLayout(grid);
        
        CTabFolder tabs = new CTabFolder(composite, SWT.NONE);
        CTabItem tab = new CTabItem(tabs, SWT.NONE);
        tab.setText("Chromium");
        
        left = new Composite(tabs, SWT.NONE);
        tab.setControl(left);
//        left.setLayout(new GridLayout(3, false));
        left.setLayout(new FillLayout(SWT.VERTICAL));
        
        right = new Composite(composite, SWT.NONE);
        right.setLayout(new GridLayout());
        composite.setWeights(new int[] {8, 2});
        
//        Text label = new Text(left, SWT.BORDER);
//        label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1));
//        label.setToolTipText("to test focus");
        
        Button button = new Button(right, SWT.PUSH);
        button.setText("Create Chrome");
        button.addSelectionListener(new SelectionAdapter() {
        	@Override
        	public void widgetSelected(SelectionEvent e) {
        		createBrowser(left);
        	}
		});

        shell.open();

        createBrowser(left);
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch())
                display.sleep();
        }
        display.dispose();
        System.err.println("Exit OK");
    }

	static void createBrowser(Composite composite) {
		Chromium c = new Chromium(composite, SWT.NONE);
//        		Composite c = new Composite(composite, SWT.NONE);
//        		c.setBackground(display.getSystemColor(SWT.COLOR_BLUE));
//		c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
		
//		final Button close = new Button(composite, SWT.PUSH);
//		close.setText("Close");
		
//		Text url = new Text(composite, SWT.BORDER);
//		url.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false));
//		final Button ok = new Button(composite, SWT.PUSH);
//		ok.setText("Go");
//		ok.addSelectionListener(new SelectionAdapter() {
//			@Override
//			public void widgetSelected(SelectionEvent e) {
//				c.setUrl(url.getText());
//			}
//		});
//		close.addSelectionListener(new SelectionAdapter() {
//			@Override
//			public void widgetSelected(SelectionEvent e) {
//				c.dispose();
//				close.dispose();
//				url.dispose();
//				ok.dispose();
//				composite.layout();
//			}
//		});
		
		composite.layout();
		System.out.println("LAYOUT");
	}

	@Override
	public Object start(IApplicationContext context) throws Exception {
//		main(null);
		
		PartRenderingEngine engine = new PartRenderingEngine();
		engine.run(this);
		
		return IApplication.EXIT_OK;
	}

	@Override
	public void stop() {
		
	}
}
