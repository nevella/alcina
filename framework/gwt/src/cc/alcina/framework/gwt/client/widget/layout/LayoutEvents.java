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

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;

/**
 * 
 * @author Nick Reddel
 */
public class LayoutEvents {
	private LayoutEvents() {
		super();
		listeners = new ArrayList<LayoutEventListener>();
	}

	private static LayoutEvents theInstance;

	public static LayoutEvents get() {
		if (theInstance == null) {
			theInstance = new LayoutEvents();
		}
		return theInstance;
	}

	public void appShutdown() {
		theInstance = null;
	}

	public void fireRequiresGlobalRelayout() {
		fireLayoutEvent(new LayoutEvent(
				LayoutEventType.REQUIRES_GLOBAL_RELAYOUT));
	}

	public static class LayoutEvent {
		private LayoutEventType eventType;

		public LayoutEventType getEventType() {
			return this.eventType;
		}

		public LayoutEvent(LayoutEventType eventType) {
			this.eventType = eventType;
		}
	}

	public static enum LayoutEventType {
		REQUIRES_GLOBAL_RELAYOUT
	}

	public static interface LayoutEventListener {
		public void onLayoutEvent(LayoutEvent event);
	}

	ArrayList<LayoutEventListener> listeners;

	public void addLayoutEventListener(LayoutEventListener l) {
		listeners.add(l);
	}

	public void removeLayoutEventListener(LayoutEventListener l) {
		listeners.remove(l);
	}

	private List<LayoutEventType> firingEvents = new ArrayList<LayoutEventType>();

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

	public void deferRequiresGlobalRelayout() {
		DeferredCommand.addCommand(new Command() {
			public void execute() {
				fireRequiresGlobalRelayout();
			}
		});
	}
}
