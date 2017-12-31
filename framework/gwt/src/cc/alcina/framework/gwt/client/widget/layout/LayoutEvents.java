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
package cc.alcina.framework.gwt.client.widget.layout;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

/**
 * 
 * @author Nick Reddel
 */
public class LayoutEvents {
	public static LayoutEvents get() {
		LayoutEvents singleton = Registry.checkSingleton(LayoutEvents.class);
		if (singleton == null) {
			singleton = new LayoutEvents();
			Registry.registerSingleton(LayoutEvents.class, singleton);
		}
		return singleton;
	}

	ArrayList<LayoutEventListener> listeners;

	private List<LayoutEventType> firingEvents = new ArrayList<LayoutEventType>();

	private int globalRelayoutQueuedCount;

	private LayoutEvents() {
		super();
		listeners = new ArrayList<LayoutEventListener>();
	}

	public void addLayoutEventListener(LayoutEventListener l) {
		listeners.add(l);
	}

	public void fireDeferredGlobalRelayout() {
		globalRelayoutQueuedCount++;
		Scheduler.get().scheduleDeferred(new ScheduledCommand() {
			public void execute() {
				globalRelayoutQueuedCount--;
				fireRequiresGlobalRelayout();
			}
		});
	}

	public void fireDeferredGlobalRelayoutIfNoneQueued() {
		if (globalRelayoutQueuedCount == 0) {
			fireDeferredGlobalRelayout();
		}
	}

	@SuppressWarnings("unchecked")
	public void fireLayoutEvent(LayoutEvent event) {
		if (firingEvents.contains(event.getEventType())) {
			return;
		}
		try {
			firingEvents.add(event.getEventType());
			ArrayList<LayoutEventListener> listenersCopy = (ArrayList<LayoutEventListener>) listeners
					.clone();
			for (LayoutEventListener l : listenersCopy) {
				l.onLayoutEvent(event);
			}
		} finally {
			firingEvents.remove(event.getEventType());
		}
	}

	public void fireRequiresGlobalRelayout() {
		fireLayoutEvent(
				new LayoutEvent(LayoutEventType.REQUIRES_GLOBAL_RELAYOUT));
	}

	public void removeLayoutEventListener(LayoutEventListener l) {
		listeners.remove(l);
	}

	public static class LayoutEvent {
		private LayoutEventType eventType;

		public LayoutEvent(LayoutEventType eventType) {
			this.eventType = eventType;
		}

		public LayoutEventType getEventType() {
			return this.eventType;
		}
	}

	public static interface LayoutEventListener {
		public void onLayoutEvent(LayoutEvent event);
	}

	public static enum LayoutEventType {
		REQUIRES_GLOBAL_RELAYOUT
	}
}
