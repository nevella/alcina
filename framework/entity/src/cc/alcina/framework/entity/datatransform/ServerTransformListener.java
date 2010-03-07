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

import cc.alcina.framework.common.client.logic.domaintransform.CommitType;
import cc.alcina.framework.common.client.logic.domaintransform.DataTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.DataTransform.DataTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DataTransform.DataTransformListener;

/**
 *
 * @author <a href="mailto:nick@alcina.cc">Nick Reddel</a>
 */

 public class ServerTransformListener implements DataTransformListener {
	public void dataTransform(DataTransformEvent evt)
			throws DataTransformException {
		if (evt.getCommitType() == CommitType.TO_REMOTE_STORAGE) {
			TransformManager tm = TransformManager.get();
			try {
				tm.consume(evt);
			} catch (Exception e) {
				 DataTransformException dte = new DataTransformException(e);
				 dte.setEvent(evt);
				 System.out.println("Direct cause:");
				 System.out.println(evt);
				 throw dte;
			}
			tm.setTransformCommitType(evt, CommitType.ALL_COMMITTED);
		}
	}
}
