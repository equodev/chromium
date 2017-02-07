package com.make.cef;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class PartRenderingEngine /*implements IPresentationEngine*/ {
	public static final String EARLY_STARTUP_HOOK = "runEarlyStartup";

	public static final String engineURI = "bundleclass://org.eclipse.e4.ui.workbench.swt/"
			+ "org.eclipse.e4.ui.internal.workbench.swt.PartRenderingEngine";

	private static final String defaultFactoryUrl = "bundleclass://org.eclipse.e4.ui.workbench.renderers.swt/"
			+ "org.eclipse.e4.ui.workbench.renderers.swt.WorkbenchRendererFactory";

	public static final String ENABLED_THEME_KEY = "themeEnabled";

	private static boolean enableThemePreference;
	private String factoryUrl;

	org.eclipse.swt.widgets.Listener keyListener;

	protected Shell testShell;

	private Shell limbo;

	public PartRenderingEngine(
			) {
		if (factoryUrl == null) {
			factoryUrl = defaultFactoryUrl;
		}
		this.factoryUrl = factoryUrl;
	}

//	protected void fixZOrder(MUIElement element) {
//		MElementContainer<MUIElement> parent = element.getParent();
//		if (parent == null) {
//			Object econtainer = ((EObject) element).eContainer();
//			if (econtainer instanceof MElementContainer<?>) {
//				@SuppressWarnings("unchecked")
//				MElementContainer<MUIElement> container = (MElementContainer<MUIElement>) econtainer;
//				parent = container;
//			}
//		}
//		if (parent == null || !(element.getWidget() instanceof Control)) {
//			return;
//		}
//
//		Control elementCtrl = (Control) element.getWidget();
//		Control prevCtrl = null;
//		for (MUIElement kid : parent.getChildren()) {
//			if (kid == element) {
//				if (prevCtrl != null) {
//					elementCtrl.moveBelow(prevCtrl);
//				} else {
//					elementCtrl.moveAbove(null);
//				}
//				break;
//			} else if (kid.getWidget() instanceof Control && kid.isVisible()) {
//				prevCtrl = (Control) kid.getWidget();
//			}
//		}
//
//		Object widget = parent.getWidget();
//		if (widget instanceof Composite) {
//			Composite composite = (Composite) widget;
//			if (composite.getShell() == elementCtrl.getShell()) {
//				Composite temp = elementCtrl.getParent();
//				while (temp != composite) {
//					if (temp == null) {
//						return;
//					}
//					temp = temp.getParent();
//				}
//				composite.layout(true, true);
//			}
//		}
//	}

	private Shell getLimboShell() {
		if (limbo == null) {
			limbo = new Shell(Display.getCurrent(), SWT.NONE);
			limbo.setText("PartRenderingEngine's limbo"); //$NON-NLS-1$ // just for debugging, not shown anywhere

			// Place the limbo shell 'off screen'
			limbo.setLocation(0, 10000);

			limbo.setBackgroundMode(SWT.INHERIT_DEFAULT);
//			limbo.setData(ShellActivationListener.DIALOG_IGNORE_KEY,
//					Boolean.TRUE);
		}
		return limbo;
	}

	public Object run(CefOsgiApp cefOsgiApp) {
		final Display display;
		display = Display.getDefault();
		initializeStyling(display);
				// set up the keybinding manager
//				KeyBindingDispatcher dispatcher = ContextInjectionFactory.make(KeyBindingDispatcher.class, runContext);
//				runContext.set(KeyBindingDispatcher.class, dispatcher);
//				keyListener = dispatcher.getKeyDownFilter();
//				display.addFilter(SWT.KeyDown, keyListener);
//				display.addFilter(SWT.Traverse, keyListener);

				// Show the initial UI

				// Create a 'limbo' shell (used to host controls that shouldn't
				// be in the current layout)
				Shell limbo = getLimboShell();
//				runContext.set("limbo", limbo);

				// HACK!! we should loop until the display gets disposed...
				// ...then we listen for the last 'main' window to get disposed
				// and dispose the Display
				testShell = null;
//				theApp = null;
				boolean spinOnce = true;
//				if (uiRoot instanceof MApplication) {
//					ShellActivationListener shellDialogListener = new ShellActivationListener((MApplication) uiRoot);
//					display.addFilter(SWT.Activate, shellDialogListener);
//					display.addFilter(SWT.Deactivate, shellDialogListener);
					spinOnce = false; // loop until the app closes
//					theApp = (MApplication) uiRoot;
					// long startTime = System.currentTimeMillis();
//					for (MWindow window : theApp.getChildren()) {
//						createGui(window);
//					}

					// long endTime = System.currentTimeMillis();
					// System.out.println("Render: " + (endTime - startTime));
					// tell the app context we are starting so the splash is
					// torn down
//					IApplicationContext ac = appContext.get(IApplicationContext.class);
//					if (ac != null) {
//						ac.applicationRunning();
//						if (eventBroker != null) {
//							eventBroker.post(
//									UIEvents.UILifeCycle.APP_STARTUP_COMPLETE,
//									theApp);
//						}
//					}
//				} else if (uiRoot instanceof MUIElement) {
//					if (uiRoot instanceof MWindow) {
//						testShell = (Shell) createGui((MUIElement) uiRoot);
//					} else {
//						// Special handling for partial models (for testing...)
//						testShell = new Shell(display, SWT.SHELL_TRIM);
//						createGui((MUIElement) uiRoot, testShell, null);
//					}
//				}

//				// allow any early startup extensions to run
//				Runnable earlyStartup = (Runnable) runContext.get(EARLY_STARTUP_HOOK);
//				if (earlyStartup != null) {
//					earlyStartup.run();
//				}

//				TestableObject testableObject = runContext.get(TestableObject.class);
//				if (testableObject instanceof E4Testable) {
//					((E4Testable) testableObject).init(display, runContext.get(IWorkbench.class));
//				}

//				if (advisor == null) {
//					advisor = new IEventLoopAdvisor() {
//						@Override
//						public void eventLoopIdle(Display display) {
//							display.sleep();
//						}
//
//						@Override
//						public void eventLoopException(Throwable exception) {
//							StatusReporter statusReporter = appContext.get(StatusReporter.class);
//							if (statusReporter != null) {
//								statusReporter.show(StatusReporter.ERROR, "Internal Error", exception);
//							} else {
//								if (logger != null) {
//									logger.error(exception);
//								}
//							}
//						}
//					};
//				}
					cefOsgiApp.main(null);
					
					
				// Spin the event loop until someone disposes the display
				while (((testShell != null && !testShell.isDisposed()) || !display.isDisposed())) {
					try {
						if (!display.readAndDispatch()) {
//							runContext.processWaiting();
//							if (spinOnce) {
//								return;
//							}
//							advisor.eventLoopIdle(display);
							display.sleep();
						}
					} catch (ThreadDeath th) {
						throw th;
					} catch (Exception ex) {
//						handle(ex, advisor);
					} catch (Error err) {
//						handle(err, advisor);
					}
				}
				if (!spinOnce) {
				}
//			}

//		return IApplication.EXIT_OK;
				return null;
	}

	public void initializeStyling(Display display) {
			Shell[] shells = display.getShells();
			for (Shell s : shells) {
				try {
					s.setRedraw(false);
					s.reskin(SWT.ALL);
//					cssEngine.applyStyles(s, true);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {
					s.setRedraw(true);
				}
			}
//		CSSRenderingUtils cssUtils = ContextInjectionFactory.make(CSSRenderingUtils.class, appContext);
//		appContext.set(CSSRenderingUtils.class, cssUtils);
	}

}
