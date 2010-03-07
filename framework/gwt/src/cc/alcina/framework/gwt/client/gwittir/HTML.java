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

package cc.alcina.framework.gwt.client.gwittir;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.MouseListener;
import com.google.gwt.user.client.ui.MouseWheelListener;
import com.google.gwt.user.client.ui.HasHorizontalAlignment.HorizontalAlignmentConstant;
import com.totsp.gwittir.client.ui.AbstractBoundWidget;

@SuppressWarnings("deprecation")
/**
 *
 * @author <a href="mailto:nick@alcina.cc">Nick Reddel</a>
 */

 public class HTML extends AbstractBoundWidget {
	private com.google.gwt.user.client.ui.HTML base;

	/** Creates a new instance of Label */
	public HTML() {
		this.init(null);
	}

	public HTML(String text) {
		this.init(text);
	}

	private void init(String text) {
		base = text == null ? new com.google.gwt.user.client.ui.HTML()
				: new com.google.gwt.user.client.ui.HTML(text);
		super.initWidget(base);
	}

	public void addMouseWheelListener(MouseWheelListener listener) {
	}

	public void removeMouseWheelListener(MouseWheelListener listener) {
	}

	public void setWordWrap(boolean wrap) {
		this.base.setWordWrap(wrap);
	}

	public void setVisible(boolean visible) {
		this.base.setVisible(visible);
	}

	public void addMouseListener(MouseListener listener) {
		this.base.addMouseListener(listener);
	}

	public void removeMouseListener(MouseListener listener) {
		this.base.removeMouseListener(listener);
	}

	public void setWidth(String width) {
		this.base.setWidth(width);
	}

	public void addStyleName(String style) {
		this.base.addStyleName(style);
	}

	public void removeStyleName(String style) {
		this.base.removeStyleName(style);
	}

	public void setHeight(String height) {
		this.base.setHeight(height);
	}

	public void setStyleName(String style) {
		this.base.setStyleName(style);
	}

	public void setText(String text) {
		this.base.setText(text);
	}

	public void setTitle(String title) {
		this.base.setTitle(title);
	}

	public void setHorizontalAlignment(
			HasHorizontalAlignment.HorizontalAlignmentConstant align) {
		this.base.setHorizontalAlignment(align);
	}

	public void addClickListener(ClickListener listener) {
		this.base.addClickListener(listener);
	}

	public void removeClickListener(ClickListener listener) {
		this.base.removeClickListener(listener);
	}

	public void unsinkEvents(int eventBitsToRemove) {
		this.base.unsinkEvents(eventBitsToRemove);
	}

	public void sinkEvents(int eventBitsToAdd) {
		this.base.sinkEvents(eventBitsToAdd);
	}

	public boolean isVisible() {
		boolean retValue;
		retValue = this.base.isVisible();
		return retValue;
	}

	public boolean getWordWrap() {
		boolean retValue;
		retValue = this.base.getWordWrap();
		return retValue;
	}

	public String getTitle() {
		String retValue;
		retValue = this.base.getTitle();
		return retValue;
	}

	public String getText() {
		String retValue;
		retValue = this.base.getText();
		return retValue;
	}

	public int getOffsetWidth() {
		int retValue;
		retValue = this.base.getOffsetWidth();
		return retValue;
	}

	public int getOffsetHeight() {
		int retValue;
		retValue = this.base.getOffsetHeight();
		return retValue;
	}

	public HasHorizontalAlignment.HorizontalAlignmentConstant getHorizontalAlignment() {
		HorizontalAlignmentConstant retValue;
		retValue = this.base.getHorizontalAlignment();
		return retValue;
	}

	public int getAbsoluteTop() {
		int retValue;
		retValue = this.base.getAbsoluteTop();
		return retValue;
	}

	public int getAbsoluteLeft() {
		int retValue;
		retValue = this.base.getAbsoluteLeft();
		return retValue;
	}

	public String getStyleName() {
		String retValue;
		retValue = this.base.getStyleName();
		return retValue;
	}

	public void setPixelSize(int width, int height) {
		this.base.setPixelSize(width, height);
	}

	public void setSize(String width, String height) {
		this.base.setSize(width, height);
	}
	@SuppressWarnings("unchecked")
	public void setValue(Object value) {
		// ("Setting value "+ value, null );
		Object old = this.getValue();
		this.setHTML(this.getRenderer() != null ? (String) this.getRenderer()
				.render(value) : value == null ? "" : value.toString());
		if (this.getValue() != old && this.getValue() != null
				&& this.getValue().equals(old)) {
			this.changes.firePropertyChange("value", old, this.getValue());
		}
	}

	public String getHTML() {
		return this.base.getHTML();
	}

	public void setHTML(String html) {
		this.base.setHTML(html);
	}

	public Object getValue() {
		return this.base.getHTML().length() == 0 ? null : this.base.getHTML();
	}
}
