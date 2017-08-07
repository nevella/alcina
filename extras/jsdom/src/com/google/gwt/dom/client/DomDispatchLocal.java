package com.google.gwt.dom.client;

public class DomDispatchLocal implements IDomDispatch {
	@Override
	public void buttonClick(ButtonElement button) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void cssClearOpacity(Style style) {
		style.removePropertyImpl("opacity");
	}

	@Override
	public void cssSetOpacity(Style style, double value) {
		style.localImpl.setPropertyImpl("opacity", String.valueOf(value));
	}

	@Override
	public void eventPreventDefault(NativeEvent evt) {
		LocalDom.eventMod(evt, "eventPreventDefault");
	}

	@Override
	public void eventStopPropagation(NativeEvent evt) {
		LocalDom.eventMod(evt, "eventStopPropagation");
	}

	@Override
	public void selectAdd(SelectElement select, OptionElement option,
			OptionElement before) {
		select.localImpl.insertBefore(option, before);
	}

	@Override
	public void selectClear(SelectElement select) {
		select.localImpl.removeAllChildren();
	}

	@Override
	public void selectRemoveOption(SelectElement select, int index) {
		NodeList<Node> list = select.getChildNodes()
				.filteredSubList(n -> n instanceof OptionElement);
		list.getItem(index).removeFromParent();
	}
}
