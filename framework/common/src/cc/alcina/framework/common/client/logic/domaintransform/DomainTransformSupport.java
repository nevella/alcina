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
package cc.alcina.framework.common.client.logic.domaintransform;

import java.util.ArrayList;
import java.util.List;

public class DomainTransformSupport {
	private List<DomainTransformListener> listenerList = new ArrayList<DomainTransformListener>();

	public void addDomainTransformListener(DomainTransformListener listener) {
		listenerList.add(listener);
	}

	public void clear() {
		listenerList.clear();
	}

	public void fireDomainTransform(DomainTransformEvent event)
			throws DomainTransformException {
		for (DomainTransformListener listener : listenerList) {
			listener.domainTransform(event);
		}
	}

	public void
			removeDomainTransformListener(DomainTransformListener listener) {
		listenerList.remove(listener);
	}
}