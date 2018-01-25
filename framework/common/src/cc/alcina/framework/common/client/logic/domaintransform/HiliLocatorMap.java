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

import java.io.Serializable;
import java.util.HashMap;

import com.totsp.gwittir.client.beans.Converter;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;

/**
 * The key is the (client's) localid of the Hili Most methods are
 * "local to persistent" - except where marked
 * 
 * @author nick@alcina.cc
 * 
 */
public class HiliLocatorMap implements Cloneable, Serializable {
	static final transient long serialVersionUID = 1;

	private HashMap<Long, HiliLocator> localToPersistent = new HashMap<>();

	private UnsortedMultikeyMap<HiliLocator> persistentToLocal = new UnsortedMultikeyMap<>(
			2);

	public HiliLocatorMap() {
	}

	public void clear() {
		localToPersistent.clear();
		persistentToLocal.clear();
	}

	public HiliLocatorMap clone() {
		HiliLocatorMap clone = new HiliLocatorMap();
		clone.localToPersistent = (HashMap<Long, HiliLocator>) localToPersistent
				.clone();
		clone.persistentToLocal = persistentToLocal.clone();
		return clone;
	}

	public boolean containsKey(Long localId) {
		return localToPersistent.containsKey(localId);
	}

	public HiliLocator get(Long localId) {
		return localToPersistent.get(localId);
	}

	public HiliLocator getFor(HasIdAndLocalId hili) {
		return localToPersistent.get(hili.getLocalId());
	}

	public HiliLocator getFor(ObjectRef ref) {
		long id = ref.getId();
		if (id != 0) {
			return new HiliLocator(ref.getClassRef().getRefClass(), id,
					ref.getLocalId());
		}
		return localToPersistent.get(ref.getLocalId());
	}

	public <H extends HasIdAndLocalId> long
			getLocalIdForClientInstance(H hili) {
		if (hili.getLocalId() != 0) {
			return hili.getLocalId();
		}
		HiliLocator hiliLocator = persistentToLocal.get(hili.getClass(),
				hili.getId());
		if (hiliLocator != null) {
			return hiliLocator.localId;
		}
		return 0;
	}

	public HiliLocator getPersistentLocator(HasIdAndLocalId hili) {
		if (hili.getId() != 0) {
			return new HiliLocator(hili);
		} else {
			return localToPersistent.get(hili.getLocalId());
		}
	}

	public boolean isEmpty() {
		return localToPersistent.isEmpty();
	}

	public void putAll(HiliLocatorMap other) {
		localToPersistent.putAll(other.localToPersistent);
		persistentToLocal.putMulti(other.persistentToLocal);
	}

	public void putToLookups(HiliLocator hiliLocator) {
		localToPersistent.put(hiliLocator.localId, hiliLocator);
		persistentToLocal.put(hiliLocator.clazz, hiliLocator.id, hiliLocator);
	}

	public static class ToCreatedIdConverter<H extends HasIdAndLocalId>
			implements Converter<H, Long> {
		private HiliLocatorMap map;

		public ToCreatedIdConverter(HiliLocatorMap map) {
			this.map = map;
		}

		@Override
		public Long convert(H original) {
			return map.localToPersistent.containsKey(original.getLocalId())
					? map.localToPersistent.get(original.getLocalId()).getId()
					: null;
		}
	}

	public void merge(HiliLocatorMap locatorMap) {
		putAll(locatorMap);
	}
}