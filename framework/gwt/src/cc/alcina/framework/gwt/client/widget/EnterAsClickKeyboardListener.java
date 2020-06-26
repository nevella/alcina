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

import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyEvent;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.TextBoxBase;

import cc.alcina.framework.common.client.util.LooseContext;

/**
 *
 * @author Nick Reddel
 */
public class EnterAsClickKeyboardListener
		implements KeyPressHandler, KeyDownHandler {
	public static final String CONTEXT_ENTER_EVENT_ACTIVE = EnterAsClickKeyboardListener.class
			.getName() + ".CONTEXT_ENTER_EVENT_ACTIVE";

	private Button button;

	private InputButton inputButton;

	private final TextBoxBase tb;

	public boolean listenToDown = false;

	public EnterAsClickKeyboardListener(TextBoxBase tb, Button button) {
		this.tb = tb;
		this.button = button;
	}

	public EnterAsClickKeyboardListener(TextBoxBase tb,
			InputButton inputButton) {
		this.tb = tb;
		this.inputButton = inputButton;
	}

	@Override
	public void onKeyDown(KeyDownEvent event) {
		if (listenToDown) {
			handleEvent(event);
		}
	}

	public void onKeyPress(KeyPressEvent event) {
		handleEvent(event);
	}

	private void handleEvent(KeyEvent event) {
		char charCode = event instanceof KeyPressEvent
				? ((KeyPressEvent) event).getCharCode()
				: '0';
		int keyCode = event.getNativeEvent().getKeyCode();
		if ((charCode == KeyCodes.KEY_ENTER || keyCode == KeyCodes.KEY_ENTER)
				&& checkCanClick()) {
			try {
				LooseContext.pushWithTrue(CONTEXT_ENTER_EVENT_ACTIVE);
				if (button != null) {
					button.click();
				}
				if (inputButton != null) {
					inputButton.click();
				}
			} finally {
				LooseContext.pop();
			}
		}
	}

	protected boolean checkCanClick() {
		return tb.getText().length() != 0;
	}
}
