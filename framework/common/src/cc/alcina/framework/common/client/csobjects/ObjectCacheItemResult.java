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

import cc.alcina.framework.common.client.logic.domaintransform.DataTransformEvent;
import cc.alcina.framework.common.client.logic.permissions.HasIdAndLocalId;


/**
 * TODO: change the "result" to DTEs
 * 
 * @author nreddel@barnet.com.au
 * 
 */
public class ObjectCacheItemResult implements Serializable {
	private ObjectCacheItemSpec itemSpec;

	private Set<? extends HasIdAndLocalId> result;

	private List<DataTransformEvent> transforms = new ArrayList<DataTransformEvent>();

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

	public void setTransforms(List<DataTransformEvent> transforms) {
		this.transforms = transforms;
	}

	public List<DataTransformEvent> getTransforms() {
		return transforms;
	}
}
