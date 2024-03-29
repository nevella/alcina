/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package cc.alcina.framework.gwt.client.gwittir.widget;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Hyperlink;
import com.totsp.gwittir.client.ui.AbstractBoundWidget;
import com.totsp.gwittir.client.ui.Renderer;
import com.totsp.gwittir.client.ui.ToStringRenderer;

/**
 *
 * @author Nick Reddel
 */
@SuppressWarnings("deprecation")
public class BoundHyperlink<T> extends AbstractBoundWidget<T> {
	protected com.google.gwt.user.client.ui.Hyperlink base;

	private Element anchorElem;

	private boolean asHtml;

	private T value;

	private Renderer<T, String> renderer = (Renderer) ToStringRenderer.INSTANCE;

	/** Creates a new instance of Label */
	public BoundHyperlink() {
		base = new Hyperlink() {
			@Override
			public void onBrowserEvent(Event event) {
				// DON'T use hyperlink history (so events can be cancelled by
				// containers eg)
				superOnBrowserEvent(event);
			}
		};
		super.initWidget(base);
		anchorElem = (Element) getElement().getFirstChild();
	}

	public void addClickListener(ClickListener listener) {
		this.base.addClickListener(listener);
	}

	public void addStyleName(String style) {
		this.base.addStyleName(style);
	}

	public int getAbsoluteLeft() {
		int retValue;
		retValue = this.base.getAbsoluteLeft();
		return retValue;
	}

	public int getAbsoluteTop() {
		int retValue;
		retValue = this.base.getAbsoluteTop();
		return retValue;
	}

	public int getOffsetHeight() {
		int retValue;
		retValue = this.base.getOffsetHeight();
		return retValue;
	}

	public int getOffsetWidth() {
		int retValue;
		retValue = this.base.getOffsetWidth();
		return retValue;
	}

	/**
	 * Get the value of renderer
	 * 
	 * @return the value of renderer
	 */
	public Renderer<T, String> getRenderer() {
		return this.renderer;
	}

	public String getStyleName() {
		String retValue;
		retValue = this.base.getStyleName();
		return retValue;
	}

	public String getTarget() {
		return DOM.getElementProperty(anchorElem, "target");
	}

	public String getTargetHistoryToken() {
		return this.base.getTargetHistoryToken();
	}

	public String getText() {
		String retValue;
		retValue = this.base.getText();
		return retValue;
	}

	public String getTitle() {
		String retValue;
		retValue = this.base.getTitle();
		return retValue;
	}

	public T getValue() {
		return value;
	}

	public boolean isAsHtml() {
		return asHtml;
	}

	public boolean isVisible() {
		boolean retValue;
		retValue = this.base.isVisible();
		return retValue;
	}

	public void removeClickListener(ClickListener listener) {
		this.base.removeClickListener(listener);
	}

	public void removeStyleName(String style) {
		this.base.removeStyleName(style);
	}

	public void setAsHtml(boolean isHtml) {
		this.asHtml = isHtml;
	}

	public void setHeight(String height) {
		this.base.setHeight(height);
	}

	public void setHref(String href) {
		this.base.setHref(href);
	}

	public void setPixelSize(int width, int height) {
		this.base.setPixelSize(width, height);
	}

	/**
	 * Set the value of renderer
	 * 
	 * @param newrenderer
	 *            new value of renderer
	 */
	public void setRenderer(Renderer<T, String> newrenderer) {
		this.renderer = newrenderer;
	}

	public void setSize(String width, String height) {
		this.base.setSize(width, height);
	}

	public void setStyleName(String style) {
		this.base.setStyleName(style);
	}

	public void setTarget(String target) {
		DOM.setElementProperty(anchorElem, "target", target);
	}

	public void setTargetHistoryToken(String targetHistoryToken) {
		this.base.setTargetHistoryToken(targetHistoryToken);
	}

	public void setText(String text) {
		this.base.setText(text);
	}

	public void setTitle(String title) {
		this.base.setTitle(title);
	}

	public void setValue(T value) {
		// ("Setting value "+ value, null );
		Object old = this.getValue();
		this.value = value;
		String renderedString = this.getRenderer() != null
				? (String) this.getRenderer().render(value)
				: value == null ? "" : value.toString();
		if (isAsHtml()) {
			this.base.setHTML(renderedString);
		} else {
			this.setText(renderedString);
		}
		if (this.getValue() != old && this.getValue() != null
				&& this.getValue().equals(old)) {
			this.changes.firePropertyChange("value", old, this.getValue());
		}
	}

	public void setVisible(boolean visible) {
		this.base.setVisible(visible);
	}

	public void setWidth(String width) {
		this.base.setWidth(width);
	}

	public void sinkEvents(int eventBitsToAdd) {
		this.base.sinkEvents(eventBitsToAdd);
	}

	public void unsinkEvents(int eventBitsToRemove) {
		this.base.unsinkEvents(eventBitsToRemove);
	}
}
