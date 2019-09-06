package cc.alcina.framework.gwt.client.widget.complex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import com.totsp.gwittir.client.ui.AbstractBoundWidget;

import cc.alcina.framework.gwt.client.widget.FlowPanelClickable;

public class LinkSelector<T> extends AbstractBoundWidget<T> {
	private FlowPanel fp;

	private T value;

	private Collection<T> options = new ArrayList<T>();

	private Function<T, String> optionRenderer;

	public LinkSelector() {
		this.fp = new FlowPanel();
		initWidget(fp);
		setStyleName("link-selector");
	}

	public Function<T, String> getOptionRenderer() {
		return this.optionRenderer;
	}

	public Collection<T> getOptions() {
		return this.options;
	}

	@Override
	public T getValue() {
		return this.value;
	}

	public void setOptionRenderer(Function<T, String> optionRenderer) {
		this.optionRenderer = optionRenderer;
	}

	public void setOptions(Collection<T> options) {
		this.options = options;
		render();
	}

	@Override
	public void setValue(T value) {
		T old = this.getValue();
		this.value = value;
		updateStyles(value);
		if (!Objects.equals(old, value)) {
			this.changes.firePropertyChange("value", old, this.getValue());
		}
	}

	private void render() {
		fp.clear();
		for (T option : options) {
			FlowPanelClickable optionPanel = new FlowPanelClickable();
			InlineLabel label = new InlineLabel(optionRenderer.apply(option));
			if (option == value) {
				optionPanel.addStyleName("selected");
			}
			optionPanel.add(label);
			optionPanel.addClickHandler(e -> setValue(option));
			fp.add(optionPanel);
		}
	}

	private void updateStyles(T selected) {
		render();
	}
}
