package com.google.gwt.dom.client;

/*
 * So named to avoid 'IDomDispatch' which is so COM+
 */
public interface DomDispatchContract {
	void buttonClick(ButtonElement button);

	void cssClearOpacity(Style style);

	default String cssFloatPropertyName() {
		return "float";
	}

	void cssSetOpacity(Style style, String value);

	void eventPreventDefault(NativeEvent evt);

	void eventStopPropagation(NativeEvent evt);

	public void selectAdd(SelectElement select, OptionElement option,
			OptionElement before);

	public void selectClear(SelectElement select);

	void selectRemoveOption(SelectElement select, int index);
}
