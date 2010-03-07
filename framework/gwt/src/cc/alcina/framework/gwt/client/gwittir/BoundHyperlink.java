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

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Hyperlink;
import com.totsp.gwittir.client.ui.AbstractBoundWidget;
@SuppressWarnings("deprecation")
/**
 *
 * @author <a href="mailto:nick@alcina.cc">Nick Reddel</a>
 */

 public class BoundHyperlink extends AbstractBoundWidget {
	protected com.google.gwt.user.client.ui.Hyperlink base;
	private Element anchorElem;
	private boolean asHtml;
	/** Creates a new instance of Label */
	public BoundHyperlink() {
		base = new Hyperlink();
		super.initWidget(base);
		anchorElem=(Element) getElement().getFirstChild();
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

	public Object getValue() {
		return this.base.getText().length() == 0 ? null : this.base.getText();
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

	public void setHeight(String height) {
		this.base.setHeight(height);
	}

	public void setPixelSize(int width, int height) {
		this.base.setPixelSize(width, height);
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
	@SuppressWarnings("unchecked")
	public void setValue(Object value) {
		// ("Setting value "+ value, null );
		Object old = this.getValue();
		String renderedString = this.getRenderer() != null ? (String) this.getRenderer()
				.render(value) : value == null ? "" : value.toString();
		if (isAsHtml()){
			this.base.setHTML(renderedString);
		}else{
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
	public void setAsHtml(boolean isHtml) {
		this.asHtml = isHtml;
	}
	public boolean isAsHtml() {
		return asHtml;
	}

}
