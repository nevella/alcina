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

import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;

/**
 *
 * @author Nick Reddel
 */
@Reflected
public enum TransformType {
	CREATE_OBJECT, DELETE_OBJECT, ADD_REF_TO_COLLECTION,
	REMOVE_REF_FROM_COLLECTION, CHANGE_PROPERTY_REF, NULL_PROPERTY_REF,
	CHANGE_PROPERTY_SIMPLE_VALUE;

	public boolean isCollectionTransform() {
		switch (this) {
		case ADD_REF_TO_COLLECTION:
		case REMOVE_REF_FROM_COLLECTION:
			return true;
		default:
			return false;
		}
	}

	public boolean isNotCollectionTransform() {
		return !isCollectionTransform();
	}
}
