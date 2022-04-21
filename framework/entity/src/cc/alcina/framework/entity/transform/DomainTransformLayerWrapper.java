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
package cc.alcina.framework.entity.transform;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse.DomainTransformResponseResult;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceEventType;

/**
 *
 * @author Nick Reddel
 */
public class DomainTransformLayerWrapper implements Serializable {
	static final transient long serialVersionUID = 1;

	private static transient Logger logger = LoggerFactory
			.getLogger(DomainTransformLayerWrapper.class);

	private static Set<List<Class>> nonEntityWarningsEmitted = Collections
			.synchronizedSet(new HashSet<>());

	public DomainTransformResponse response;

	public EntityLocatorMap locatorMap;

	public int ignored;

	public List<DomainTransformEventPersistent> persistentEvents = new ArrayList<DomainTransformEventPersistent>();

	public List<DomainTransformEvent> remoteEventsPersisted = new ArrayList<DomainTransformEvent>();

	private Multimap<Class, List<DomainTransformEventPersistent>> eventsByClass;

	public List<DomainTransformRequestPersistent> persistentRequests = new ArrayList<DomainTransformRequestPersistent>();

	int mergeCount = 0;

	public boolean fireAsQueueEvent;

	public DomainTransformLayerWrapper() {
		// for serialization
	}

	public DomainTransformLayerWrapper(TransformPersistenceToken token) {
		if (token != null) {
			token.setTransformResult(this);
		}
	}

	public boolean containsTransformClasses(Class... classes) {
		return containsTransformClasses(Arrays.asList(classes));
	}

	public boolean containsTransformClasses(List<Class> classes) {
		// FIXME - 2022 - convert to precondition
		if (classes.stream().anyMatch(c -> !Entity.class.isAssignableFrom(c))) {
			if (nonEntityWarningsEmitted.add(classes)) {
				logger.warn("non-transform-classes-filter - {}", classes);
			}
		}
		return !CommonUtils.intersection(getTransformedClasses(), classes)
				.isEmpty();
	}

	public Multimap<Class, List<DomainTransformEventPersistent>>
			getEventsByClass() {
		if (eventsByClass == null) {
			eventsByClass = new Multimap<Class, List<DomainTransformEventPersistent>>();
			for (DomainTransformEventPersistent dte : persistentEvents) {
				eventsByClass.add(dte.getObjectClass(), dte);
			}
		}
		return this.eventsByClass;
	}

	// For the moment, this seems as good as than using the Kafka transform
	// commit topic log offset - in
	// that if this has been seen, then so has the corresponding Kafka log
	// offset. And simpler to get
	public String getLogOffset() {
		if (persistentRequests.size() == 0) {
			return null;
		}
		return String.valueOf(CommonUtils.last(persistentRequests).getId());
	}

	public <V extends Entity> Set<V> getObjectsFor(Class<V> clazz) {
		return (Set<V>) (Set) getTransformsFor(clazz).stream()
				.map(DomainTransformEvent::toObjectLocator).map(Domain::find)
				.filter(Objects::nonNull).collect(Collectors.toSet());
	}

	public Set<Class> getTransformedClasses() {
		return getEventsByClass().keySet();
	}

	public List<DomainTransformEventPersistent> getTransformsFor(Class clazz) {
		return getEventsByClass().getAndEnsure(clazz);
	}

	public Stream<DomainTransformEventPersistent>
			getTransformsFor(Entity entity) {
		return getTransformsFor(entity.entityClass()).stream().filter(
				dte -> dte.toObjectLocator().equals(entity.toLocator()));
	}

	public EntityLocatorMap locatorMapOrEmpty() {
		return locatorMap == null ? new EntityLocatorMap() : locatorMap;
	}

	public void merge(DomainTransformLayerWrapper toMerge) {
		if (++mergeCount > 1) {
			throw new UnsupportedOperationException();
		}
		this.ignored = toMerge.ignored;
		this.locatorMap = toMerge.locatorMap;
		this.persistentEvents = toMerge.persistentEvents;
		this.persistentRequests = toMerge.persistentRequests;
		this.remoteEventsPersisted = toMerge.remoteEventsPersisted;
		this.response = toMerge.response;
		this.eventsByClass = toMerge.eventsByClass;
		this.mergeCount = toMerge.mergeCount;
	}

	public DomainTransformPersistenceEventType providePersistenceEventType() {
		return response.getResult() == DomainTransformResponseResult.OK
				? DomainTransformPersistenceEventType.COMMIT_OK
				: DomainTransformPersistenceEventType.COMMIT_ERROR;
	}

	public void snapshotEntityLocatorMap() {
		locatorMap = locatorMap.copy();
	}
}
