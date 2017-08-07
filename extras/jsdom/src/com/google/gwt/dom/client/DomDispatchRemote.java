package com.google.gwt.dom.client;

public class DomDispatchRemote implements IDomDispatch {
	// per-browser custom
	DOMImpl domImpl;

	@Override
	public void buttonClick(ButtonElement button) {
		domImpl.buttonClick(button.domImpl);
	}

	@Override
	public void cssClearOpacity(Style style) {
		domImpl.cssClearOpacity(style.domImpl());
	}

	@Override
	public String cssFloatPropertyName() {
		return domImpl.cssFloatPropertyName();
	}

	@Override
	public void cssSetOpacity(Style style, double value) {
		domImpl.cssSetOpacity(style.domImpl(), value);
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
		domImpl.selectAdd(select.domImpl, option.domImpl,
				before == null ? null : before.domImpl);
	}

	@Override
	public void selectClear(SelectElement select) {
		domImpl.selectClear(select.domImpl);
	}

	@Override
	public void selectRemoveOption(SelectElement select, int index) {
		domImpl.selectRemoveOption(select.domImpl, index);
	}
}
