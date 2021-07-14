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
package cc.alcina.framework.common.client.logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Nick Reddel
 */
public abstract class StateListenable {
	private List<StateChangeListener> listeners;

	public StateListenable() {
		super();
		listeners = new ArrayList<StateChangeListener>();
	}

	public void addStateChangeListener(StateChangeListener l) {
		synchronized (listeners) {
			listeners.add(l);
		}
	}

	public void clearListeners() {
		synchronized (listeners) {
			listeners.clear();
		}
	}

	public void removeStateChangeListener(StateChangeListener l) {
		synchronized (listeners) {
			listeners.remove(l);
		}
	}

	protected void fireStateChanged(String newState) {
		List<StateChangeListener> listenersCopy = new ArrayList<>();
		synchronized (listeners) {
			listenersCopy.addAll(listeners);
		}
		for (StateChangeListener l : listenersCopy) {
			l.stateChanged(this, newState);
		}
	}
}
