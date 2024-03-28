package com.google.gwt.dom.client;

public class DomDispatchJso implements DomDispatchContract {
	// per-browser custom
	DOMImpl domImpl;

	@Override
	public void buttonClick(ButtonElement button) {
		domImpl.buttonClick(button.implAccess().ensureJsoRemote());
	}

	public ElementJso createElement(String tagName) {
		return domImpl.createElement(Document.get().jsoRemote(), tagName);
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
	public void cssSetOpacity(Style style, String value) {
		if (style.linkedToRemote()) {
			domImpl.cssSetOpacity(style, value);
		}
	}

	@Override
	public void eventPreventDefault(NativeEvent evt) {
		domImpl.eventPreventDefault(evt.jso);
	}

	@Override
	public void eventStopPropagation(NativeEvent evt) {
		domImpl.eventStopPropagation(evt.jso);
	}

	@Override
	public void selectAdd(SelectElement select, OptionElement option,
			OptionElement before) {
		if (select.linkedToRemote()) {
			domImpl.selectAdd(select.jsoRemote(), option.jsoRemote(),
					before == null ? null : before.jsoRemote());
		}
	}

	@Override
	public void selectClear(SelectElement select) {
		if (select.linkedToRemote()) {
			domImpl.selectClear(select.jsoRemote());
		}
	}

	@Override
	public void selectRemoveOption(SelectElement select, int index) {
		if (select.linkedToRemote()) {
			domImpl.selectRemoveOption(select.jsoRemote(), index);
		}
	}
}
