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

package cc.alcina.framework.entity.datatransform;

import java.util.List;

import javax.persistence.EntityManager;

import cc.alcina.framework.common.client.entity.GwtPersistableObject;
import cc.alcina.framework.entity.entityaccess.WrappedObject;


/**
 *
 * @author Nick Reddel
 */

 public interface WrappedObjectProvider {

	public <T extends GwtPersistableObject> T getWrappedObjectForUser(Class<T> c, EntityManager em)
			throws Exception;

	public <T extends GwtPersistableObject> T getWrappedObjectForUser(Class<T> c, long id, EntityManager em)
			throws Exception;

	public <T extends GwtPersistableObject> WrappedObject<T> getObjectWrapperForUser(Class<T> c, long id, EntityManager em)
			throws Exception;
	public List<Class> getJaxbSubclasses();
}
