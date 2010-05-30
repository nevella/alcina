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
package cc.alcina.framework.gwt.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.ClosingEvent;
import com.google.gwt.user.client.Window.ClosingHandler;

/**
 */
public abstract class ClientBase implements EntryPoint, ClosingHandler,
		CloseHandler<Window> {
	public boolean isTestMode() {
		return testMode;
	}

	public void setTestMode(boolean testMode) {
		this.testMode = testMode;
	}

	private boolean testMode;

	private boolean displayInitialised = false;

	public void onWindowClosing(ClosingEvent event) {
		ClientLayerLocator.get().getCommitToStorageTransformListener().flush();
	}

	public void onClose(CloseEvent<Window> event) {
	}

	public void setDisplayInitialised(boolean displayInitialised) {
		this.displayInitialised = displayInitialised;
	}

	public boolean isDisplayInitialised() {
		return displayInitialised;
	}
}
