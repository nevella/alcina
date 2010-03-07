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

package cc.alcina.framework.gwt.client.logic;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.ui.TreeItem;

/**
 *
 * @author <a href="mailto:nick@alcina.cc">Nick Reddel</a>
 */

 public class ExtraTreeEvent {
	public interface ExtraTreeEventListener {
		public void onExtraTreeEvent(ExtraTreeEventEvent evt);
	}
	public enum ETEType{
		DBL_CLICK, RIGHT_CLICK
	}
	public static class ExtraTreeEventEvent {
		private TreeItem source;
		private final ETEType type;

		public ETEType getType() {
			return this.type;
		}

		public ExtraTreeEventEvent(TreeItem source, ETEType type) {
			this.source = source;
			this.type = type;
		}

		public TreeItem getSource() {
			return this.source;
		}
	}

	public interface ExtraTreeEventSource {
		public void addExtraTreeEventListener(ExtraTreeEventListener listener);

		public void removeExtraTreeEventListener(ExtraTreeEventListener listener);
	}

	public static class ExtraTreeEventSupport implements ExtraTreeEventSource {
		private List<ExtraTreeEventListener> listenerList = new ArrayList<ExtraTreeEventListener>();;


		public void addExtraTreeEventListener(ExtraTreeEventListener listener) {
			listenerList.add(listener);
		}

		public void removeExtraTreeEventListener(ExtraTreeEventListener listener) {
			listenerList.remove(listener);
		}

		public void fireActionsAvailbleChange(ExtraTreeEventEvent event) {
			for (ExtraTreeEventListener listener : listenerList) {
				listener.onExtraTreeEvent(event);
			}
		}
	}
}
