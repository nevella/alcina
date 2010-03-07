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

/**
 *
 * @author <a href="mailto:nick@alcina.cc">Nick Reddel</a>
 */

 public class VetoableActionEvent {
	private Object source;
	private Object parameters;
	private final VetoableAction action;

	public VetoableAction getAction() {
		return this.action;
	}

	public VetoableActionEvent(Object source, VetoableAction action) {
		this.source = source;
		this.action = action;
	}

	public Object getSource() {
		return this.source;
	}

	public void setParameters(Object parameters) {
		this.parameters = parameters;
	}

	public Object getParameters() {
		return parameters;
	}
}