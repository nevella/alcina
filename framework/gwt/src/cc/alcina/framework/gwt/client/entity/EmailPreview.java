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
package cc.alcina.framework.gwt.client.entity;

import java.io.Serializable;

/**
 *
 * @author Nick Reddel
 */
public class EmailPreview implements Serializable {
	private String toAddresses;

	private String subject;

	private String body;

	public String getBody() {
		return this.body;
	}

	public String getSubject() {
		return this.subject;
	}

	public String getToAddresses() {
		return this.toAddresses;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public void setToAddresses(String toAddresses) {
		this.toAddresses = toAddresses;
	}
}
