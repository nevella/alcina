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

package cc.alcina.framework.gwt.client.widget.dialog;


import cc.alcina.framework.gwt.client.widget.GlassDisplayer;

import com.google.gwt.user.client.ui.DialogBox;

/**
 *
 * @author <a href="mailto:nick@alcina.cc">Nick Reddel</a>
 */

 public class GlassDialogBox extends DialogBox {
	@Override
	public void hide() {
		super.hide();
		glass.show(false);
	}

	private GlassDisplayer glass = new GlassDisplayer();

	public GlassDisplayer getGlass() {
		return this.glass;
	}

	@Override
	//glass won't be visible, but will be added to DOM before dialog
	public void center() {
		glass.show(true);
		super.center();
	}

	@Override
	public void show() {
		glass.show(true);
		super.show();
	}
}
