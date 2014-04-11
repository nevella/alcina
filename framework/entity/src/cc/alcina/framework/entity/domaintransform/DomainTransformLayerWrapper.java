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
package cc.alcina.framework.entity.domaintransform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse;
import cc.alcina.framework.common.client.logic.domaintransform.HiliLocatorMap;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.Multimap;

/**
 * 
 * @author Nick Reddel
 */
public class DomainTransformLayerWrapper {
	public DomainTransformResponse response;

	public HiliLocatorMap locatorMap;

	public int ignored;

	public List<DomainTransformEventPersistent> persistentEvents = new ArrayList<DomainTransformEventPersistent>();

	public Set<Class> getTransformedClasses() {
		return getEventsByClass().keySet();
	}

	private Multimap<Class, List<DomainTransformEventPersistent>> eventsByClass;

	public List<DomainTransformRequestPersistent> persistentRequests = new ArrayList<DomainTransformRequestPersistent>();

	public boolean containsTransformClasses(Class... classes) {
		return !CommonUtils.intersection(getTransformedClasses(),
				Arrays.asList(classes)).isEmpty();
	}

	public Multimap<Class, List<DomainTransformEventPersistent>> getEventsByClass() {
		if (eventsByClass == null) {
			eventsByClass = new Multimap<Class, List<DomainTransformEventPersistent>>();
			for (DomainTransformEventPersistent dte : persistentEvents) {
				eventsByClass.add(dte.getObjectClass(), dte);
			}
		}
		return this.eventsByClass;
	}

	public List<DomainTransformEventPersistent> getTransformsFor(Class clazz) {
		return getEventsByClass().getAndEnsure(clazz);
	}

}
