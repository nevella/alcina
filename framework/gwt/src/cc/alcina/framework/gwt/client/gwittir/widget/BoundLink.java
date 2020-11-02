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

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.totsp.gwittir.client.ui.AbstractBoundWidget;
import com.totsp.gwittir.client.ui.Renderer;
import com.totsp.gwittir.client.ui.ToStringRenderer;

import cc.alcina.framework.gwt.client.widget.Link;

/**
 * 
 * @author Nick Reddel
 */
public class BoundLink<T> extends AbstractBoundWidget<T> {
	protected Link base;

	private boolean asHtml;

	private T value;

	private Renderer<T, String> renderer = (Renderer) ToStringRenderer.INSTANCE;

	/** Creates a new instance of Label */
	public BoundLink() {
		base = new Link();
		super.initWidget(base);
	}

	public HandlerRegistration addClickHandler(ClickHandler handler) {
		return this.base.addClickHandler(handler);
	}

	@Override
	public void addStyleName(String style) {
		this.base.addStyleName(style);
	}

	public void enableDefault() {
		base.setPreventDefault(false);
	}

	@Override
	public int getAbsoluteLeft() {
		int retValue;
		retValue = this.base.getAbsoluteLeft();
		return retValue;
	}

	@Override
	public int getAbsoluteTop() {
		int retValue;
		retValue = this.base.getAbsoluteTop();
		return retValue;
	}

	public String getHref() {
		return base.getElement().getPropertyString("href");
	}

	@Override
	public int getOffsetHeight() {
		int retValue;
		retValue = this.base.getOffsetHeight();
		return retValue;
	}

	@Override
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

	@Override
	public String getStyleName() {
		String retValue;
		retValue = this.base.getStyleName();
		return retValue;
	}

	public String getTarget() {
		return base.getElement().getPropertyString("target");
	}

	public String getText() {
		String retValue;
		retValue = this.base.getText();
		return retValue;
	}

	@Override
	public String getTitle() {
		String retValue;
		retValue = this.base.getTitle();
		return retValue;
	}

	@Override
	public T getValue() {
		return value;
	}

	public boolean isAsHtml() {
		return asHtml;
	}

	public boolean isEnabled() {
		return this.base.isEnabled();
	}

	@Override
	public boolean isVisible() {
		boolean retValue;
		retValue = this.base.isVisible();
		return retValue;
	}

	@Override
	public void removeStyleName(String style) {
		this.base.removeStyleName(style);
	}

	public void setAsHtml(boolean isHtml) {
		this.asHtml = isHtml;
	}

	public void setEnabled(boolean enabled) {
		this.base.setEnabled(enabled);
	}

	@Override
	public void setHeight(String height) {
		this.base.setHeight(height);
	}

	public void setHref(String href) {
		base.setHref(href);
	}

	@Override
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

	@Override
	public void setSize(String width, String height) {
		this.base.setSize(width, height);
	}

	@Override
	public void setStyleName(String style) {
		this.base.setStyleName(style);
	}

	public void setTarget(String target) {
		base.getElement().setPropertyString("target", target);
	}

	public void setText(String text) {
		this.base.setText(text);
	}

	@Override
	public void setTitle(String title) {
		this.base.setTitle(title);
	}

	@Override
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

	@Override
	public void setVisible(boolean visible) {
		this.base.setVisible(visible);
	}

	@Override
	public void setWidth(String width) {
		this.base.setWidth(width);
	}

	@Override
	public void sinkEvents(int eventBitsToAdd) {
		this.base.sinkEvents(eventBitsToAdd);
	}

	@Override
	public void unsinkEvents(int eventBitsToRemove) {
		this.base.unsinkEvents(eventBitsToRemove);
	}
}
