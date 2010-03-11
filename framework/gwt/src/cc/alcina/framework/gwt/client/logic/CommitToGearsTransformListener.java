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

package cc.alcina.framework.gwt.client.logic;

import cc.alcina.framework.common.client.logic.domaintransform.CommitType;
import cc.alcina.framework.common.client.logic.domaintransform.DataTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.DataTransform.DataTransformListener;
/**
 * For a more serious gears-based system. 
 * @author nick@alcina.cc
 *
 */
public class CommitToGearsTransformListener implements DataTransformListener {
	public void dataTransform(DataTransformEvent evt) {
		if (evt.getCommitType() == CommitType.TO_LOCAL_STORAGE) {
			TransformManager tm = TransformManager.get();
			String pn = evt.getPropertyName();
			if (pn != null
					&& (pn.equals(TransformManager.ID_FIELD_NAME) || pn
							.equals(TransformManager.LOCAL_ID_FIELD_NAME))) {
			} else {
				tm.setTransformCommitType(evt, CommitType.TO_REMOTE_STORAGE);
			}
			return;
		}
	}
}