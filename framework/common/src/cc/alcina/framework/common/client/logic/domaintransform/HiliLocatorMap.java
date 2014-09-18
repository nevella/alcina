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

import java.util.HashMap;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;

import com.totsp.gwittir.client.beans.Converter;

/**
 * The key is the (client's) localid of the Hili
 * 
 * @author nick@alcina.cc
 * 
 */
public class HiliLocatorMap extends HashMap<Long, HiliLocator> {
	public static class ToCreatedIdConverter<H extends HasIdAndLocalId>
			implements Converter<H, Long> {
		private HiliLocatorMap map;

		public ToCreatedIdConverter(HiliLocatorMap map) {
			this.map = map;
		}

		@Override
		public Long convert(H original) {
			return map.containsKey(original.getLocalId()) ? map.get(
					original.getLocalId()).getId() : null;
		}
	}

	public HiliLocator getFor(HasIdAndLocalId hili) {
		return get(hili.getLocalId());
	}

	public HiliLocator getFor(ObjectRef ref) {
		long id = ref.getId();
		if (id != 0) {
			return new HiliLocator(ref.getClassRef().getRefClass(), id);
		}
		return get(ref.getLocalId());
	}
}