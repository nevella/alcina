package com.google.gwt.dom.client;

public class DomDispatchLocal implements DomDispatchContract {
	@Override
	public void buttonClick(ButtonElement button) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void cssClearOpacity(Style style) {
		style.local.removeProperty("opacity");
	}

	@Override
	public void cssSetOpacity(Style style, double value) {
		style.local.setProperty("opacity", String.valueOf(value));
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
		select.local().insertBefore(option, before);
	}

	@Override
	public void selectClear(SelectElement select) {
		select.local().removeAllChildren();
	}

	@Override
	public void selectRemoveOption(SelectElement select, int index) {
		NodeList<Node> list = select.getChildNodes()
				.filteredSubList(n -> n instanceof OptionElement);
		list.getItem(index).removeFromParent();
	}
}
