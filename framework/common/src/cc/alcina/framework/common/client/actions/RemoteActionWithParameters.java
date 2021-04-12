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
package cc.alcina.framework.common.client.actions;

import cc.alcina.framework.common.client.logic.HasParameters;
import cc.alcina.framework.common.client.serializer.flat.PropertySerialization;

/**
 * Marker subclass, to be run on the server
 * 
 * @author nick@alcina.cc
 *
 */
public abstract class RemoteActionWithParameters<T extends RemoteParameters>
		extends RemoteAction implements HasParameters<T> {
	private T parameters;

	public RemoteActionWithParameters() {
	}

	@Override
	@PropertySerialization(defaultProperty = true)
	public T getParameters() {
		return parameters;
	}

	@Override
	public void setParameters(T parameters) {
		this.parameters = parameters;
	}
}
