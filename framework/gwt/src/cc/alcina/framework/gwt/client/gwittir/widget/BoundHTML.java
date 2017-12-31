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

import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasHorizontalAlignment.HorizontalAlignmentConstant;
import com.totsp.gwittir.client.ui.AbstractBoundWidget;

import cc.alcina.framework.gwt.client.gwittir.customiser.MultilineWidget;

/**
 *
 */
public class BoundHTML extends AbstractBoundWidget<String>
		implements MultilineWidget {
	private com.google.gwt.user.client.ui.HTML base;

	/** Creates a new instance of Label */
	public BoundHTML() {
		this.init(null);
	}

	public BoundHTML(String text) {
		this.init(text);
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

	public String getValue() {
		return this.base.getHTML().length() == 0 ? null : this.base.getHTML();
	}

	public boolean getWordWrap() {
		boolean retValue;
		retValue = this.base.getWordWrap();
		return retValue;
	}

	@Override
	public boolean isMultiline() {
		return true;
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

	public void setValue(String value) {
		// ("Setting value "+ value, null );
		Object old = this.getValue();
		this.setHTML(value);
		if (this.getValue() != old && this.getValue() != null
				&& !this.getValue().equals(old)) {
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
		this.base.setWordWrap(wrap);
	}

	@Override
	public void sinkEvents(int eventBitsToAdd) {
		this.base.sinkEvents(eventBitsToAdd);
	}

	@Override
	public void unsinkEvents(int eventBitsToRemove) {
		this.base.unsinkEvents(eventBitsToRemove);
	}

	private void init(String text) {
		base = text == null ? new com.google.gwt.user.client.ui.HTML()
				: new com.google.gwt.user.client.ui.HTML(text);
		super.initWidget(base);
	}
}
