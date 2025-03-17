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
import java.util.List;

import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse.DomainTransformResponseResult;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceEventType;

/**
 *
 * @author Nick Reddel
 */
public class DomainTransformLayerWrapper implements Serializable {
	public DomainTransformResponse response;

	public EntityLocatorMap locatorMap;

	public int ignored;

	public List<DomainTransformEventPersistent> persistentEvents = new ArrayList<DomainTransformEventPersistent>();

	public List<DomainTransformEvent> remoteEventsPersisted = new ArrayList<DomainTransformEvent>();

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

	public void clearPersistentTransformData() {
		persistentEvents.clear();
		persistentRequests.clear();
		remoteEventsPersisted.clear();
	}
}
