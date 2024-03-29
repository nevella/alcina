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

import java.io.Serializable;

public class DomainTransformRuntimeException extends RuntimeException
		implements Serializable {
	private DomainTransformEvent event;

	public DomainTransformRuntimeException(String message) {
		super(message);
	}

	public DomainTransformEvent getEvent() {
		return event;
	}

	public void setEvent(DomainTransformEvent event) {
		this.event = event;
	}
}