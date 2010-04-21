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

import cc.alcina.framework.common.client.logic.domaintransform.CommitType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformListener;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;

/**
 *
 * @author Nick Reddel
 */

 public class ServerTransformListener implements DomainTransformListener {
	public void domainTransform(DomainTransformEvent event)
			throws DomainTransformException {
		if (event.getCommitType() == CommitType.TO_STORAGE) {
			TransformManager tm = TransformManager.get();
			try {
				tm.consume(event);
			} catch (Exception e) {
				 
				 System.out.println("Direct cause:");
				 System.out.println(event);
				 throw DomainTransformException.wrap(e, event);
			}
			tm.setTransformCommitType(event, CommitType.ALL_COMMITTED);
		}
	}
}
