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
package cc.alcina.framework.gwt.client.widget;

import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.BidiUtils;
import com.google.gwt.i18n.client.HasDirection;
import com.google.gwt.user.client.ui.HasHTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.HasWordWrap;
import com.google.gwt.user.client.ui.Widget;

/**
 *
 * @author Nick Reddel
 */
public class Para extends Widget implements HasClickHandlers,
		HasHorizontalAlignment, HasText, HasWordWrap, HasDirection, HasHTML {
	private HorizontalAlignmentConstant horzAlign;

	public Para() {
	}

	public Para(String html) {
		setElement(Document.get().createPElement());
		setHTML(html);
	}

	public Para(String html, boolean wordWrap) {
		this(html);
		setWordWrap(wordWrap);
	}

	public HandlerRegistration addClickHandler(ClickHandler handler) {
		return addDomHandler(handler, ClickEvent.getType());
	}

	public Direction getDirection() {
		return BidiUtils.getDirectionOnElement(getElement());
	}

	public HorizontalAlignmentConstant getHorizontalAlignment() {
		return horzAlign;
	}

	public String getHTML() {
		return getElement().getInnerHTML();
	}

	public String getText() {
		return getElement().getInnerText();
	}

	public boolean getWordWrap() {
		return !getElement().getStyle().getProperty("whiteSpace")
				.equals("nowrap");
	}

	public void setDirection(Direction direction) {
		BidiUtils.setDirectionOnElement(getElement(), direction);
	}

	public void setHorizontalAlignment(HorizontalAlignmentConstant align) {
		horzAlign = align;
		getElement().getStyle().setProperty("textAlign",
				align.getTextAlignString());
	}

	public void setHTML(String html) {
		getElement().setInnerHTML(html);
	}

	public void setText(String text) {
		getElement().setInnerText(text);
	}

	public void setWordWrap(boolean wrap) {
		getElement().getStyle().setProperty("whiteSpace",
				wrap ? "normal" : "nowrap");
	}
}
