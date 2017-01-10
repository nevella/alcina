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

/**
 * 
 * @author Nick Reddel
 */
public class LoginBean implements Serializable {
	private String userName;

	private String password;

	private boolean rememberMe;
	
	private Map<String,String> properties = new LinkedHashMap<>();

	public LoginBean() {
	}

	public LoginBean(String userName, String password, boolean rememberMe) {
		this.userName = userName;
		this.password = password;
		this.rememberMe = rememberMe;
	}

	public String getPassword() {
		return this.password;
	}

	public Map<String, String> getProperties() {
		return this.properties;
	}

	public String getUserName() {
		return this.userName;
	}

	public boolean isRememberMe() {
		return this.rememberMe;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}

	public void setRememberMe(boolean rememberMe) {
		this.rememberMe = rememberMe;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}
}
