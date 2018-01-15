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

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.HasHTML;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.ui.HasEnabled;

import cc.alcina.framework.gwt.client.logic.AlcinaHistoryItem;
import cc.alcina.framework.gwt.client.place.BasePlace;
import cc.alcina.framework.gwt.client.util.WidgetUtils;
import cc.alcina.framework.gwt.client.widget.SelectWithSearch.HasItem;

/**
 * 
 * @author Nick Reddel
 */
public class Link<T> extends Widget
		implements HasHTML, HasEnabled, HasClickHandlers, HasItem<T>, HasText {
	public static Link createHashHref(String text, String token) {
		return createHrefNoUnderline(text, "#" + token);
	}

	public static Link createHrefNoUnderline(String text,
			AlcinaHistoryItem epoch) {
		return createHrefNoUnderline(text, epoch.toHref());
	}

	public static Link createHrefNoUnderline(String text, String href) {
		Link link = new Link(text);
		link.setHref(href);
		link.noUnderline();
		return link;
	}

	public static Link createNoUnderline(String text, ClickHandler handler) {
		Link link = new Link(text, handler);
		link.noUnderline();
		return link;
	}

	protected Element anchorElem;

	private T userObject;

	private boolean preventDefault = true;

	private boolean enabled = true;

	/**
	 * Creates an empty hyperlink.
	 */
	public Link() {
		anchorElem = DOM.createAnchor();
		createElement();
		setStyleName("gwt-Hyperlink alcina-NoHistory");
		setHref("#");
	}

	/**
	 * Creates a hyperlink with its text and target history token specified.
	 * 
	 * @param text
	 *            the hyperlink's text
	 * @param asHTML
	 *            <code>true</code> to treat the specified text as html
	 */
	public Link(String text) {
		this(text, false);
	}

	public Link(String text, AlcinaHistoryItem historyItem) {
		this(text, false);
		setHref(historyItem.toHref());
	}

	public Link(String text, boolean asHTML) {
		this();
		if (asHTML) {
			setHTML(text);
		} else {
			setText(text);
		}
	}

	public Link(String string, boolean asHTML, ClickHandler handler) {
		this(string, asHTML);
		addDomHandler(handler, ClickEvent.getType());
	}

	public Link(String string, ClickHandler handler) {
		this(string, false, handler);
	}

	public HandlerRegistration addClickHandler(ClickHandler handler) {
		return addDomHandler(handler, ClickEvent.getType());
	}

	public String getHref() {
		return DOM.getElementProperty(anchorElem, "href");
	}

	public String getHTML() {
		return DOM.getInnerHTML(anchorElem);
	}

	@Override
	public T getItem() {
		return userObject;
	}

	public String getTarget() {
		return DOM.getElementProperty(anchorElem, "target");
	}

	public String getText() {
		return DOM.getInnerText(anchorElem);
	}

	public T getUserObject() {
		return userObject;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public boolean isPreventDefault() {
		return this.preventDefault;
	}

	public Link noUnderline() {
		setStyleName("link-no-underline");
		return this;
	}

	@Override
	public void onBrowserEvent(Event event) {
		if (DOM.eventGetType(event) == Event.ONCLICK) {
			if (!WidgetUtils.isNewTabModifier() && preventDefault) {
				DOM.eventPreventDefault(event);
			}
			if (enabled) {
				super.onBrowserEvent(event);
			}
		}
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
		if (enabled) {
			removeStyleName("disabled");
		} else {
			addStyleName("disabled");
		}
	}

	public void setEpoch(AlcinaHistoryItem epoch) {
		setHref(epoch.toHref());
	}

	public void setHref(String href) {
		DOM.setElementProperty(anchorElem, "href", href);
		if (href != null && !href.matches("#?")) {
			setPreventDefault(false);
		}
	}

	public void setHTML(String html) {
		DOM.setInnerHTML(anchorElem, html);
	}

	public void setPreventDefault(boolean preventDefault) {
		this.preventDefault = preventDefault;
	}

	public void setTarget(String target) {
		DOM.setElementProperty(anchorElem, "target", target);
	}

	public void setText(String text) {
		DOM.setInnerText(anchorElem, text);
	}

	public void setTitle(String title) {
		if (title == null || title.length() == 0) {
			DOM.removeElementAttribute(anchorElem, "title");
		} else {
			DOM.setElementAttribute(anchorElem, "title", title);
		}
	}

	public void setUserObject(T userObject) {
		this.userObject = userObject;
	}

	public void setWordWrap(boolean wrap) {
		getElement().getStyle().setProperty("whiteSpace",
				wrap ? "normal" : "nowrap");
	}

	protected void createElement() {
		setElement(anchorElem);
	}

	/**
	 * <b>Affected Elements:</b>
	 * <ul>
	 * <li>-wrapper = the div around the link.</li>
	 * </ul>
	 * 
	 * @see UIObject#onEnsureDebugId(String)
	 */
	@Override
	protected void onEnsureDebugId(String baseID) {
		ensureDebugId(anchorElem, "", baseID);
	}

	public static Link createPlace(String text, BasePlace place) {
		return createHashHref(text, place.toTokenString());
	}
}
