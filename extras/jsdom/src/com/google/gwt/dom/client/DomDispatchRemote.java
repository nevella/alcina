package com.google.gwt.dom.client;

public class DomDispatchRemote implements IDomDispatch {
	// per-browser custom
	DOMImpl domImpl;

	@Override
	public void buttonClick(ButtonElement button) {
		domImpl.buttonClick(button.implAccess().ensureRemote());
	}

	public ElementRemote createElement(String tagName) {
		return domImpl.createElement(Document.get().typedRemote(), tagName);
	}

	@Override
	public void cssClearOpacity(Style style) {
		if (style.linkedToRemote()) {
			domImpl.cssClearOpacity(style);
		}
	}

	@Override
	public String cssFloatPropertyName() {
		return domImpl.cssFloatPropertyName();
	}

	@Override
	public void cssSetOpacity(Style style, double value) {
		if (style.linkedToRemote()) {
			domImpl.cssSetOpacity(style, value);
		}
	}

	@Override
	public void eventPreventDefault(NativeEvent evt) {
		domImpl.eventPreventDefault(evt);
	}

	@Override
	public void eventStopPropagation(NativeEvent evt) {
		domImpl.eventStopPropagation(evt);
	}

	@Override
	public void selectAdd(SelectElement select, OptionElement option,
			OptionElement before) {
		if (select.linkedToRemote()) {
			domImpl.selectAdd(select.typedRemote(), option.typedRemote(),
					before == null ? null : before.typedRemote());
		}
	}

	@Override
	public void selectClear(SelectElement select) {
		if (select.linkedToRemote()) {
			domImpl.selectClear(select.typedRemote());
		}
	}

	@Override
	public void selectRemoveOption(SelectElement select, int index) {
		if (select.linkedToRemote()) {
			domImpl.selectRemoveOption(select.typedRemote(), index);
		}
	}
}
