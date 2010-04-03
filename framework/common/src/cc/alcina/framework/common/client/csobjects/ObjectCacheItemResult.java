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

package cc.alcina.framework.common.client.csobjects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;


/**
 * TODO: change the "result" to DTEs
 * 
 * @author nick@alcina.cc
 * 
 */
public class ObjectCacheItemResult implements Serializable {
	private ObjectCacheItemSpec itemSpec;

	private Set<? extends HasIdAndLocalId> result;

	private List<DomainTransformEvent> transforms = new ArrayList<DomainTransformEvent>();

	public ObjectCacheItemResult() {
	}

	public ObjectCacheItemResult(ObjectCacheItemSpec itemSpec,
			Set<? extends HasIdAndLocalId> result) {
		this.itemSpec = itemSpec;
		this.result = result;
	}

	public ObjectCacheItemSpec getItemSpec() {
		return this.itemSpec;
	}

	public void setItemSpec(ObjectCacheItemSpec itemSpec) {
		this.itemSpec = itemSpec;
	}

	public Set<? extends HasIdAndLocalId> getResult() {
		return this.result;
	}

	public void setResult(Set<? extends HasIdAndLocalId> result) {
		this.result = result;
	}

	public void setTransforms(List<DomainTransformEvent> transforms) {
		this.transforms = transforms;
	}

	public List<DomainTransformEvent> getTransforms() {
		return transforms;
	}
}
