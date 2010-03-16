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

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

/**
 *
 * @author Nick Reddel
 */

 public class StyledAWidget<O> extends Link {
	private Element spanElem;

	public StyledAWidget(String text) {
		super();
		DOM.appendChild(anchorElem, spanElem = DOM.createSpan());
		setText(text);
		setHref("#");
		DOM.setElementProperty(anchorElem, "href", "#");
	}

	public StyledAWidget(String text, boolean asHTML) {
		super();
		DOM.appendChild(anchorElem, spanElem = DOM.createSpan());
		setHref("#");
		DOM.setElementProperty(anchorElem, "href", "#");
		if (asHTML) {
			setHTML(text);
		} else {
			setText(text);
		}
	}

	@Override
	public void setHTML(String html) {
		DOM.setInnerHTML(spanElem, html);
	}

	@Override
	public void setText(String text) {
		DOM.setInnerText(spanElem, text);
	}

	@Override
	protected Element getStyleElement() {
		return anchorElem;
	}

	
	public String getTarget() {
		return DOM.getElementProperty(anchorElem, "target");
	}

	public void setTarget(String target) {
		DOM.setElementProperty(anchorElem, "target", target);
	}
}