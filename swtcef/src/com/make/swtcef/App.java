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
        Label label = new Label(shell, SWT.BORDER);
        label.setText("This is a label:");
        label.setToolTipText("This is the tooltip of this label");
        
        Button button = new Button(shell, SWT.PUSH);
        button.setText("Create Chrome");
        button.addSelectionListener(new SelectionAdapter() {
        	@Override
        	public void widgetSelected(SelectionEvent e) {
        		final Chromium c = new Chromium(shell, SWT.NONE);
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
    }
}
