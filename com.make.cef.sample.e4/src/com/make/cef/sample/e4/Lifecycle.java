package com.make.cef.sample.e4;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.e4.ui.workbench.lifecycle.PostContextCreate;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.swt.widgets.Display;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import com.make.swtcef.Chromium;

@SuppressWarnings("restriction")
public class Lifecycle {
	@PostContextCreate
	void postContextCreate(final IEventBroker eventBroker, IApplicationContext context, Display display) {
		System.out.println("postContextCreate");
		eventBroker.subscribe(UIEvents.UILifeCycle.APP_SHUTDOWN_STARTED, new EventHandler() {
			@Override
			public void handleEvent(Event event) {
				eventBroker.unsubscribe(this);
				System.out.println("APP_SHUTDOWN_STARTED, shutting down CEF");
				Chromium.shutdown();
			}
		});
		context.applicationRunning();
	}
}
