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

import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;

@MappedSuperclass
/**
 *
 * @author Nick Reddel
 */
public abstract class DomainTransformRequestPersistent
		extends DomainTransformRequest implements HasId {
	private long id;

	public void clearForSimplePersistence() {
		setClientInstance(null);
		setEvents(null);
	}

	@Transient
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public abstract void wrap(DomainTransformRequest dtr);
}
