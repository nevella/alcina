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
package cc.alcina.framework.entity.entityaccess;

import java.io.File;
import java.util.Collection;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.entity.util.GraphProjection.GraphProjectionFilter;
import cc.alcina.framework.entity.util.GraphProjection.InstantiateImplCallback;

/**
 * 
 * @author Nick Reddel
 */
public interface JPAImplementation {
	boolean isLazyInitialisationException(Exception e);

	public <T> T getInstantiatedObject(T object);

	public GraphProjectionFilter getResolvingFilter(InstantiateImplCallback callback,
			DetachedEntityCache cache);

	/**
	 * return false if no optimisation
	 */
	public boolean bulkDelete(EntityManager em, Class clazz,
			Collection<Long> ids);

	public boolean isCacheDisabled();

	public void setCacheDisabled(boolean cacheDisabled);

	public void cache(Query query);

	public void interpretException(DomainTransformException exception);

	public File getConfigDirectory();
	
	public void afterSpecificSetId(Object fromBefore) throws Exception;

	Object beforeSpecificSetId(EntityManager entityManager, Object toPersist) throws Exception;

	void muteClassloaderLogging(boolean mute);

	public abstract InstantiateImplCallback getClassrefInstantiator();
}
