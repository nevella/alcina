package cc.alcina.framework.gwt.client.widget;

import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.totsp.gwittir.client.beans.Converter;
import com.totsp.gwittir.client.ui.AbstractBoundWidget;

public class AbstractBoundWidgetWrapper<T, C> extends AbstractBoundWidget<T>
		implements HasComplexPanel {
	private T value;

	private final AbstractBoundWidget<C> wrappee;

	private final Converter<T, C> converter;

	private FlowPanel fp;

	public AbstractBoundWidgetWrapper(AbstractBoundWidget<C> wrappee,
			Converter<T, C> converter) {
		this.wrappee = wrappee;
		this.converter = converter;
		fp = new FlowPanel();
		initWidget(fp);
		fp.add(wrappee);
	}

	@Override
	public Object getModel() {
		return wrappee.getModel();
	}

	@Override
	public void setModel(Object model) {
		super.setModel(model);
		wrappee.setModel(model);
	}

	public T getValue() {
		return this.value;
	}

	public void setValue(T value) {
		this.value = value;
		wrappee.setValue(converter.convert(value));
	}

	@Override
	public ComplexPanel getComplexPanel() {
		return fp;
	}
}