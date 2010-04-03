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

package cc.alcina.framework.common.client.actions;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Nick Reddel
 */

 public class PermissibleActionEvent {
	private Object source;
	private Object parameters;
	private final PermissibleAction action;

	public PermissibleAction getAction() {
		return this.action;
	}

	public PermissibleActionEvent(Object source, PermissibleAction action) {
		this.source = source;
		this.action = action;
	}

	public Object getSource() {
		return this.source;
	}

	public void setParameters(Object parameters) {
		this.parameters = parameters;
	}

	public Object getParameters() {
		return parameters;
	}

	public interface PermissibleActionListener {
		public void vetoableAction(PermissibleActionEvent evt);
	}

	public interface PermissibleActionSource {
		public void addVetoableActionListener(PermissibleActionEvent.PermissibleActionListener listener);
	
		public void removeVetoableActionListener(PermissibleActionEvent.PermissibleActionListener listener);
	}

	public static class PermissibleActionSupport implements PermissibleActionEvent.PermissibleActionSource {
		private List<PermissibleActionEvent.PermissibleActionListener> listenerList  =new ArrayList<PermissibleActionEvent.PermissibleActionListener>();
	
		
		public void addVetoableActionListener(PermissibleActionEvent.PermissibleActionListener listener) {
			listenerList.add(listener);
		}
	
		public void removeVetoableActionListener(PermissibleActionEvent.PermissibleActionListener listener) {
			listenerList.remove(listener);
		}
		public void removeAllListeners() {
			listenerList.clear();
		}
		public void fireVetoableActionEvent(PermissibleActionEvent event) {
			List<PermissibleActionEvent.PermissibleActionListener> copy = new ArrayList<PermissibleActionEvent.PermissibleActionListener>(listenerList);
			for (PermissibleActionEvent.PermissibleActionListener listener : copy) {
				listener.vetoableAction(event);
			}
		}
	}
}