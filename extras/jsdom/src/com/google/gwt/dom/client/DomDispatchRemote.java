package com.google.gwt.dom.client;

public class DomDispatchRemote implements IDomDispatch {
	// per-browser custom
	DOMImpl domImpl;

	@Override
	public void buttonClick(ButtonElement button) {
		domImpl.buttonClick(button.typedRemote());
	}

	@Override
	public void cssClearOpacity(Style style) {
		domImpl.cssClearOpacity(style);
	}

	@Override
	public String cssFloatPropertyName() {
		return domImpl.cssFloatPropertyName();
	}

	@Override
	public void cssSetOpacity(Style style, double value) {
		domImpl.cssSetOpacity(style, value);
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
		domImpl.selectAdd(select.typedRemote(), option.typedRemote(),
				before == null ? null : before.typedRemote());
	}

	@Override
	public void selectClear(SelectElement select) {
		domImpl.selectClear(select.typedRemote());
	}

	@Override
	public void selectRemoveOption(SelectElement select, int index) {
		domImpl.selectRemoveOption(select.typedRemote(), index);
	}

	public ElementRemote createElement(String tagName) {
		return domImpl.createElement(Document.get().typedRemote(), tagName);
	}
}
