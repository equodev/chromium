
package com.make.cef.sample.e4;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MBasicFactory;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

public class ControlsPart {
	private EPartService partService;
	private MApplication application;
	private EModelService modelService;
	int parts = 1;
	private List<String> urls = new ArrayList<>();

	@Inject
	public ControlsPart(EPartService partService, MApplication application, EModelService modelService) {
		this.partService = partService;
		this.application = application;
		this.modelService = modelService;
		urls.add("https://html5test.com/");
		urls.add("http://peacekeeper.futuremark.com/");
		urls.add("https://web.basemark.com");
		urls.add("http://www.fishgl.com/");
		urls.add("https://www.wirple.com/bmark/");
		urls.add("http://www.speed-battle.com/speedtest_e.php");
		urls.add("http://browserbench.org/");
	}

	@PostConstruct
	public void postConstruct(Composite parent) {
		parent.setLayout(new GridLayout(1, false));
		
		Button add = new Button(parent, SWT.PUSH);
		add.setText("New browser");
		GridDataFactory.defaultsFor(add).applyTo(add);
		add.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				add();
			}
		});
	}

	public void add() {
		String url = urls.get(new Random().nextInt(urls.size()));
		
		MPart part = MBasicFactory.INSTANCE.createPart();
		part.setLabel("Chromium" + parts++);
		part.setCloseable(true);
		part.setContributionURI("platform:/plugin/com.make.cef.sample.e4/com.make.cef.sample.e4.CefPart");
		List<MPartStack> stacks = modelService.findElements(application, null, MPartStack.class, null);
		stacks.get(0).getChildren().add(part);
		partService.showPart(part, PartState.ACTIVATE);
		
		((CefPart) part.getObject()).setUrl(url);
	}
}