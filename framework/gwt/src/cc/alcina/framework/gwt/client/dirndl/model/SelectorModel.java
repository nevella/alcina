package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.List;

public class SelectorModel<T> extends Model {
	private List<T> selected;

	private String input;

	public List<T> getSelected() {
		return this.selected;
	}

	public void setSelected(List<T> selected) {
		this.selected = selected;
	}

	public String getInput() {
		return this.input;
	}

	public void setInput(String input) {
		this.input = input;
	}

	public List<T> getSuggested() {
		return this.suggested;
	}

	public void setSuggested(List<T> suggested) {
		this.suggested = suggested;
	}

	private List<T> suggested;
}
