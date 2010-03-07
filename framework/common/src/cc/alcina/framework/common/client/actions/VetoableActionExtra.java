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
 * @author <a href="mailto:nick@alcina.cc">Nick Reddel</a>
 */

 public class VetoableActionExtra {
	public interface VetoableActionListener {
		public void vetoableAction(VetoableActionEvent evt);
	}

	public interface VetoableActionSource {
		public void addVetoableActionListener(VetoableActionListener listener);

		public void removeVetoableActionListener(VetoableActionListener listener);
	}

	public static class VetoableActionSupport implements VetoableActionSource {
		private List<VetoableActionListener> listenerList  =new ArrayList<VetoableActionListener>();

		
		public void addVetoableActionListener(VetoableActionListener listener) {
			listenerList.add(listener);
		}

		public void removeVetoableActionListener(VetoableActionListener listener) {
			listenerList.remove(listener);
		}
		public void removeAllListeners() {
			listenerList.clear();
		}
		public void fireVetoableActionEvent(VetoableActionEvent event) {
			List<VetoableActionListener> copy = new ArrayList<VetoableActionListener>(listenerList);
			for (VetoableActionListener listener : copy) {
				listener.vetoableAction(event);
			}
		}
	}
}
