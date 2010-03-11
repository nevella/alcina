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

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;

/**
 * 
 * 
 * @author nick@alcina.cc
 * 
 * @param <T>
 *            - user object
 */
public class BlockLink<T> extends Link<T> {
	public BlockLink() {
		super();
	}

	public BlockLink(String text, boolean asHTML) {
		super(text, asHTML);
	}

	public BlockLink(String string, boolean asHTML, ClickHandler handler) {
		super(string, asHTML, handler);
	}

	public BlockLink(String string, ClickHandler handler) {
		super(string, handler);
	}

	public BlockLink(String text) {
		super(text);
	}

	@Override
	protected void createElement() {
		setElement(DOM.createDiv());
		DOM.appendChild(getElement(), anchorElem = DOM.createAnchor());
	}
}
