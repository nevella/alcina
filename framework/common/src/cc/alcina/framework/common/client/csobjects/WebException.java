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

/**
 * 
 * @author Nick Reddel
 */
public class WebException extends Exception implements Serializable {
	public static final String THE_APPLICATION_IS_OUT_OF_DATE = "The application is out of date";

	public WebException() {
	}

	public WebException(Exception e) {
		super(e.getMessage());
	}

	public WebException(String message) {
		super(message);
	}

	public WebException(String message, Exception e) {
		super(message + " Cause: " + e.getClass() + " - " + e.getMessage());
	}

	public static class ClientMessagingWebException extends WebException {
		private transient String internalMessage;

		public ClientMessagingWebException() {
		}

		public ClientMessagingWebException(String clientMessage,
				String internalMessage) {
			super(clientMessage);
			this.internalMessage = internalMessage;
		}

		@Override
		public String getMessage() {
			return internalMessage != null ? internalMessage : super
					.getMessage();
		}
	}

	public static WebException maybeWrap(Exception e) {
		if(e instanceof WebException){
			return (WebException) e;
		}
		return new WebException(e);
	}
}
