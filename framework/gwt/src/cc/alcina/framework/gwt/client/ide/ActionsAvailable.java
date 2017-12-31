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
package cc.alcina.framework.gwt.client.ide;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.actions.PermissibleAction;

/**
 *
 * @author Nick Reddel
 */
public class ActionsAvailable {
	public static class ActionsAvailableEvent {
		private final Object source;

		private final List<PermissibleAction> actions;

		public ActionsAvailableEvent(Object source,
				List<PermissibleAction> actions) {
			this.source = source;
			this.actions = actions;
		}

		public List<PermissibleAction> getActions() {
			return this.actions;
		}

		public Object getSource() {
			return this.source;
		}
	}

	public interface ActionsAvailableListener {
		public void actionsAvailable(ActionsAvailableEvent evt);
	}

	public interface ActionsAvailableSource {
		public void
				addActionsAvailableListener(ActionsAvailableListener listener);

		public void removeActionsAvailableListener(
				ActionsAvailableListener listener);
	}

	public static class ActionsAvailableSupport
			implements ActionsAvailableSource {
		private List<ActionsAvailableListener> listenerList;

		public ActionsAvailableSupport() {
			this.listenerList = new ArrayList<ActionsAvailableListener>();
		}

		public void
				addActionsAvailableListener(ActionsAvailableListener listener) {
			listenerList.add(listener);
		}

		public void fireActionsAvailbleChange(ActionsAvailableEvent event) {
			for (ActionsAvailableListener listener : listenerList) {
				listener.actionsAvailable(event);
			}
		}

		public void removeActionsAvailableListener(
				ActionsAvailableListener listener) {
			listenerList.remove(listener);
		}
	}
}
