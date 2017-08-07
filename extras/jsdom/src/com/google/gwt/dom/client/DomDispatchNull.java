package com.google.gwt.dom.client;

public class DomDispatchNull implements IDomDispatch {
	@Override
	public void buttonClick(ButtonElement button) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void cssClearOpacity(Style style) {
	}

	@Override
	public void cssSetOpacity(Style style, double value) {
	}

	@Override
	public void eventPreventDefault(NativeEvent evt) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void eventStopPropagation(NativeEvent evt) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void selectAdd(SelectElement select, OptionElement option,
			OptionElement before) {
	}

	@Override
	public void selectClear(SelectElement select) {
	}

	@Override
	public void selectRemoveOption(SelectElement select, int index) {
	}
}
