package cc.alcina.framework.gwt.client.lux;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;

import cc.alcina.framework.gwt.client.dirndl.StyleType;
import cc.alcina.framework.gwt.client.widget.HeadingPanel;

public class LuxContainer extends Composite {
	FlowPanel container;

	public LuxContainer() {
		this.container = new FlowPanel();
		initWidget(container);
	}

	public LuxContainer(StyleType style) {
		this();
		style.set(this);
	}

	public LuxContainer add(IsWidget child) {
		this.container.add(child);
		return this;
	}

	public void addHeader(int level, String text) {
		HeadingPanel panel = new HeadingPanel(level);
		panel.getElement().setInnerText(text);
		container.add(panel);
	}

	public LuxContainer addLink(String text, ClickHandler clickHandler) {
		LuxLink link = new LuxLink().text(text);
		link.addClickHandler(clickHandler);
		add(link);
		return this;
	}

	public LuxContainer addLink(String text, String href) {
		LuxLink link = new LuxLink().text(text).href(href);
		add(link);
		return this;
	}

	public void addLinkInPanel(String text, String href) {
		LuxContainer container = new LuxContainer();
		container.add(new LuxLink().text(text).href(href));
		add(container);
	}

	public void addStyledHtmlBlock(StyleType style, String htmlString) {
		HTML html = new HTML(htmlString);
		style.set(html);
		add(html);
	}

	public LuxContainer addStyledPanel(StyleType style) {
		LuxContainer container = new LuxContainer();
		style.set(container);
		add(container);
		return container;
	}

	public void addStyledTextBlock(StyleType style, String text) {
		Label label = new Label(text);
		style.set(label);
		add(label);
	}

	public void addTextBlock(String text) {
		Label label = new Label(text);
		label.setStyleName("");
		add(label);
	}

	public void clear() {
		container.clear();
	}

	public void addTo(ComplexPanel complexPanel) {
		complexPanel.add(this);
	}
}
