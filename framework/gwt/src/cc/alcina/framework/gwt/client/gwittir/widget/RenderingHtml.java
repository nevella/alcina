/*
 * Label.java
 *
 * Created on July 24, 2007, 5:35 PM
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package cc.alcina.framework.gwt.client.gwittir.widget;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasHorizontalAlignment.HorizontalAlignmentConstant;
import com.google.gwt.user.client.ui.MouseListener;
import com.google.gwt.user.client.ui.MouseWheelListener;
import com.totsp.gwittir.client.ui.AbstractBoundWidget;
import com.totsp.gwittir.client.ui.Renderer;
import com.totsp.gwittir.client.ui.ToStringRenderer;

/**
 * 
 * @author <a href="mailto:cooper@screaming-penguin.com">Robert "kebernet"
 *         Cooper</a>
 * 
 */
@SuppressWarnings("deprecation")
public class RenderingHtml<T> extends AbstractBoundWidget<T> {
	private com.google.gwt.user.client.ui.HTML base;

	private T value;

	private Renderer<T, String> renderer = (Renderer) ToStringRenderer.INSTANCE;

	private Renderer<T, String> titleRenderer = null;

	public RenderingHtml() {
		this.init();
	}

	public void addChangeListener(ClickListener clickListener) {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	public HandlerRegistration addClickHandler(ClickHandler handler) {
		return addDomHandler(handler, ClickEvent.getType());
	}

	public void addClickListener(ClickListener listener) {
		this.base.addClickListener(listener);
	}

	public void addMouseListener(MouseListener listener) {
		this.base.addMouseListener(listener);
	}

	public void addMouseWheelListener(MouseWheelListener listener) {
	}

	@Override
	public void addStyleName(String style) {
		this.base.addStyleName(style);
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

	public HasHorizontalAlignment.HorizontalAlignmentConstant
			getHorizontalAlignment() {
		HorizontalAlignmentConstant retValue;
		retValue = this.base.getHorizontalAlignment();
		return retValue;
	}

	public String getHTML() {
		return this.base.getHTML();
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

	public Renderer<T, String> getTitleRenderer() {
		return this.titleRenderer;
	}

	@Override
	public T getValue() {
		return value;
	}

	public boolean getWordWrap() {
		boolean retValue;
		retValue = this.base.getWordWrap();
		return retValue;
	}

	@Override
	public boolean isVisible() {
		boolean retValue;
		retValue = this.base.isVisible();
		return retValue;
	}

	public void removeClickListener(ClickListener listener) {
		this.base.removeClickListener(listener);
	}

	public void removeMouseListener(MouseListener listener) {
		this.base.removeMouseListener(listener);
	}

	public void removeMouseWheelListener(MouseWheelListener listener) {
	}

	@Override
	public void removeStyleName(String style) {
		this.base.removeStyleName(style);
	}

	@Override
	public void setHeight(String height) {
		this.base.setHeight(height);
	}

	public void setHorizontalAlignment(
			HasHorizontalAlignment.HorizontalAlignmentConstant align) {
		this.base.setHorizontalAlignment(align);
	}

	public void setHTML(String html) {
		this.base.setHTML(html);
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

	public void setText(String text) {
		this.base.setText(text);
	}

	@Override
	public void setTitle(String title) {
		this.base.setTitle(title);
	}

	public void setTitleRenderer(Renderer<T, String> titleRenderer) {
		this.titleRenderer = titleRenderer;
	}

	@Override
	public void setValue(T value) {
		// ("Setting value "+ value, null );
		Object old = this.getValue();
		this.value = value;
		this.setHTML(renderer.render(value));
		if (titleRenderer != null) {
			setTitle(titleRenderer.render(value));
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

	public void setWordWrap(boolean wrap) {
		this.base.setStyleName("nowrap", !wrap);
	}

	@Override
	public void sinkEvents(int eventBitsToAdd) {
		this.base.sinkEvents(eventBitsToAdd);
	}

	@Override
	public void unsinkEvents(int eventBitsToRemove) {
		this.base.unsinkEvents(eventBitsToRemove);
	}

	private void init() {
		base = new com.google.gwt.user.client.ui.HTML();
		super.initWidget(base);
	}
}
