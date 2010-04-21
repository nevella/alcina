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

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.entity.WrapperPersistable;
import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.entity.SEUtilities;


/**
 *
 * @author Nick Reddel
 */

 public class UnwrapInfoItem {
	private String propertyName;

	private WrappedObject<WrapperPersistable> wrappedObject;

	public UnwrapInfoItem() {
	}
	public UnwrapInfoItem(String propertyName, WrappedObject<WrapperPersistable> wrappedObject) {
		this.propertyName = propertyName;
		this.wrappedObject = wrappedObject;
	}

	public String getPropertyName() {
		return this.propertyName;
	}

	public WrappedObject<WrapperPersistable> getWrappedObject() {
		return this.wrappedObject;
	}

	public static class UnwrapInfoContainer {
		private List<UnwrapInfoItem> items = new ArrayList<UnwrapInfoItem>();

		private HasId hasId;

		public HasId getHasId() {
			return hasId;
		}

		public List<UnwrapInfoItem> getItems() {
			return items;
		}

		public void setHasId(HasId hasId) {
			this.hasId = hasId;
		}

		public void setItems(List<UnwrapInfoItem> items) {
			this.items = items;
		}

		public HasId unwrap(ClassLoader classLoader) {
			for (UnwrapInfoItem item : getItems()) {
				try {
					PropertyDescriptor pd = SEUtilities.descriptorByName(
							hasId.getClass(), item.getPropertyName());
					pd.getWriteMethod().invoke(hasId,
							item.getWrappedObject().getObject(classLoader));
				} catch (Exception e) {
					throw new WrappedRuntimeException(e);
				}
			}
			return hasId;
		}
	}
}
