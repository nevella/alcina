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
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.TextBoxBase;

/**
 *
 * @author Nick Reddel
 */

 public class EnterAsClickKeyboardListener implements KeyPressHandler {
	private Button button;

	private InputButton inputButton;

	private final TextBoxBase tb;

	public EnterAsClickKeyboardListener(TextBoxBase tb, InputButton inputButton) {
		this.tb = tb;
		this.inputButton = inputButton;
	}

	public EnterAsClickKeyboardListener(TextBoxBase tb, Button button) {
		this.tb = tb;
		this.button = button;
	}

	public void onKeyPress(KeyPressEvent event) {
		char charCode = event.getCharCode();
		if (charCode == KeyCodes.KEY_ENTER && tb.getText().length() != 0) {
			if (button != null) {
				button.click();
			}
			if (inputButton != null) {
				inputButton.click();
			}
		}
	}
}
