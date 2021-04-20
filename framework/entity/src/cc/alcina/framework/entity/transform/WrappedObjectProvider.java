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

import java.util.List;

import javax.persistence.EntityManager;

import cc.alcina.framework.common.client.entity.WrapperPersistable;
import cc.alcina.framework.entity.persistence.WrappedObject;

/**
 *
 * @author Nick Reddel
 */
public interface WrappedObjectProvider {
	public static final String CONTEXT_DO_NOT_CREATE = WrappedObjectProvider.class
			.getName() + ".CONTEXT_DO_NOT_CREATE";

	public List<Class> getJaxbSubclasses();

	public <T extends WrapperPersistable> WrappedObject<T>
			getObjectWrapperForUser(Class<T> c, long id, EntityManager em)
					throws Exception;

	public <T extends WrapperPersistable> T getWrappedObjectForUser(Class<T> c,
			EntityManager em) throws Exception;

	public <T extends WrapperPersistable> T getWrappedObjectForUser(Class<T> c,
			long id, EntityManager em) throws Exception;
}
