package cc.alcina.framework.gwt.client.lux;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;

import cc.alcina.framework.gwt.client.widget.HeadingPanel;

public class LuxContainer extends Composite {
	FlowPanel container;

	public LuxContainer() {
		this.container = new FlowPanel();
		initWidget(container);
	}

	public LuxContainer(LuxStylesType style) {
		this();
		style.set(this);
	}

	public void add(IsWidget child) {
		this.container.add(child);
	}

	public void addHeader(int level, String text) {
		HeadingPanel panel = new HeadingPanel(level);
		panel.getElement().setInnerText(text);
		container.add(panel);
	}

	public void addStyledHtmlBlock(LuxStylesType style, String htmlString) {
		HTML html = new HTML(htmlString);
		style.set(html);
		add(html);
	}

	public LuxContainer addStyledPanel(LuxStylesType style) {
		LuxContainer container = new LuxContainer();
		style.set(container);
		add(container);
		return container;
	}

	public void addStyledTextBlock(LuxStylesType style, String text) {
		Label label = new Label(text);
		style.set(label);
		add(label);
	}
}
