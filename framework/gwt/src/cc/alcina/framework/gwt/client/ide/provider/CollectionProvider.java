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
package cc.alcina.framework.gwt.client.ide.provider;

import java.util.Collection;

import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationListener;
import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationSource;
import cc.alcina.framework.gwt.client.ide.widget.DetachListener;

/**
 * 
 * @author Nick Reddel
 */
public interface CollectionProvider<T>
		extends CollectionModificationSource, DetachListener {
	public Collection<T> getCollection();

	public Class<? extends T> getCollectionMemberClass();

	public int getCollectionSize();

	public abstract static class SilentCollectionProvider<T>
			implements CollectionProvider<T> {
		public void addCollectionModificationListener(
				CollectionModificationListener listener) {
		}

		@Override
		public int getCollectionSize() {
			return 0;
		}

		public void onDetach() {
		}

		public void removeCollectionModificationListener(
				CollectionModificationListener listener) {
		}
	}
}
