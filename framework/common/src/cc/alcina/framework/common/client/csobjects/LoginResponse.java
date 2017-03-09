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
package cc.alcina.framework.common.client.csobjects;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;

/**
 * 
 * @author Nick Reddel
 */
public class LoginResponse implements Serializable {
	private boolean ok;

	private String errorMsg;

	private ClientInstance clientInstance;

	private Map<String, String> properties = new LinkedHashMap<>();

	public Map<String, String> getProperties() {
		return this.properties;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}

	public LoginResponse() {
	}

	public ClientInstance getClientInstance() {
		return clientInstance;
	}

	public String getErrorMsg() {
		return this.errorMsg;
	}

	public boolean isOk() {
		return this.ok;
	}

	public void setClientInstance(ClientInstance clientInstance) {
		this.clientInstance = clientInstance;
	}

	public void setErrorMsg(String errorMsg) {
		this.errorMsg = errorMsg;
	}

	public void setOk(boolean ok) {
		this.ok = ok;
	}
}
