package com.make.swtcef;

import org.eclipse.swt.SWT;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class App {

    public static void main(String[] args) {
        final Display display = new Display();

        Shell shell = new Shell(display);

        // the layout manager handle the layout
        // of the widgets in the container
        GridLayout grid = new GridLayout(2, false);
        shell.setLayout(grid);
        
     // Shell can be used as container
        Text label = new Text(shell, SWT.BORDER);
//        label.setText("This is a text:");
        label.setToolTipText("to test focus");
        
        Button button = new Button(shell, SWT.PUSH);
        button.setText("Create Chrome");
        button.addSelectionListener(new SelectionAdapter() {
        	@Override
        	public void widgetSelected(SelectionEvent e) {
        		Chromium c = new Chromium(shell, SWT.NONE);
//        		Composite c = new Composite(shell, SWT.NONE);
//        		c.setBackground(display.getSystemColor(SWT.COLOR_BLUE));
        		GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
        		c.setLayoutData(layoutData);
        		
        		final Button close = new Button(shell, SWT.PUSH);
        		close.setText("Close");
        		close.addSelectionListener(new SelectionAdapter() {
        			@Override
        			public void widgetSelected(SelectionEvent e) {
        				c.dispose();
        				close.dispose();
        				shell.layout();
        			}
        		});
        		
        		Text url = new Text(shell, SWT.BORDER);
        		url.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false));
				final Button ok = new Button(shell, SWT.PUSH);
				ok.setText("Go");
				ok.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						c.setUrl(url.getText());
					}
				});
				
        		shell.layout();
        		System.out.println("LAYOUT");
        	}
		});

        // set widgets size to their preferred size
//        label.pack();
        
        

        shell.open();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch())
                display.sleep();
        }
        display.dispose();
        System.err.println("Exit OK");
    }
}
