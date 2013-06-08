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

import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager.OnlineState;
import cc.alcina.framework.common.client.provider.TextProvider;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.gwt.client.logic.CommitToStorageTransformListener;

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
	public void onWindowClosing(ClosingEvent event) {
		windowClosing = true;
		CommitToStorageTransformListener storage = ClientLayerLocator.get()
				.getCommitToStorageTransformListener();
		storage.setPaused(false);
		String msg = TextProvider.get().getUiObjectText(ClientBase.class,
				"commit-on-close-saving-final-changes-warning",
				"Please press 'cancel' to save recent changes");
		storage.flush();
		if (storage.getCurrentState() == CommitToStorageTransformListener.COMMITTING
				&& PermissionsManager.get().getOnlineState() != OnlineState.OFFLINE) {
			event.setMessage(msg);
		}
		windowClosing = false;
	}

	private boolean windowClosing;

	public void onClose(CloseEvent<Window> event) {
	}

	public boolean isWindowClosing() {
		return this.windowClosing;
	}
}
