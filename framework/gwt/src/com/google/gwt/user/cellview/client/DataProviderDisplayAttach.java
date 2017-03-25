package com.google.gwt.user.cellview.client;

import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.logical.shared.AttachEvent.Handler;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.view.client.AbstractDataProvider;

public class DataProviderDisplayAttach implements Handler {
	private boolean attachedToProvider;

	private HasDataWidget hasData;

	private AbstractDataProvider dataProvider;

	public DataProviderDisplayAttach(HasDataWidget hasData,
			AbstractDataProvider dataProvider) {
		this.hasData = hasData;
		this.dataProvider = dataProvider;
		((IsWidget) hasData).asWidget().addAttachHandler(this);
	}

	public void attach() {
		if (!attachedToProvider) {
			dataProvider.addDataDisplay(hasData);
			attachedToProvider = true;
		}
	}

	@Override
	public void onAttachOrDetach(AttachEvent event) {
		if (event.isAttached()) {
			attach();
		} else {
			detach();
		}
	}

	private void detach() {
		if (attachedToProvider) {
			dataProvider.removeDataDisplay(hasData);
			attachedToProvider = false;
		}
	}
}