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

import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import cc.alcina.framework.common.client.logic.domain.DomainTransformPersistable;
import cc.alcina.framework.common.client.logic.domain.DomainTransformPropagation;
import cc.alcina.framework.common.client.logic.domain.DomainTransformPropagation.PropagationType;
import cc.alcina.framework.common.client.logic.domain.VersionableEntity;
import cc.alcina.framework.common.client.logic.permissions.HasIUser;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;

@MappedSuperclass
/**
 *
 * 
 * 
 * @author nick@alcina.cc
 * 
 */
@DomainTransformPersistable
@DomainTransformPropagation(PropagationType.NONE)
@RegistryLocation(registryPoint = AlcinaPersistentEntityImpl.class, targetClass = PublicationCounter.class)
public abstract class PublicationCounter
		extends VersionableEntity<PublicationCounter> implements HasIUser {
	private long counter;

	public long getCounter() {
		return this.counter;
	}

	@Override
	@Transient
	public long getId() {
		return id;
	}

	public void setCounter(long counter) {
		long old_counter = this.counter;
		this.counter = counter;
		propertyChangeSupport().firePropertyChange("counter", old_counter,
				counter);
	}

	@Override
	public void setId(long id) {
		this.id = id;
	}
}
