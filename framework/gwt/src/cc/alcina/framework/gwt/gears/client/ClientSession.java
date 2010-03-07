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

package cc.alcina.framework.gwt.gears.client;


import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.WrappedRuntimeException.SuggestedAction;

import com.google.gwt.user.client.Cookies;

/**
 * 
 * @author nreddel@barnet.com.au
 * <p>Basic session support for the (possibly offline) client</p>
 * 
 * Logic:
 * <ul>
 * <li>Each client (browser "process", pace chrome) has a unique sessionId</li>
 * <li>Persistent dtrs are tagged with the session id</li>
 * <li>For simplicity, only one offline instance (tab) can be run per sessionId 
 * (one per sessionId/clientInstanceId combo would be possible, but probably confusin
 * to users)</li>
 * <li>Sessions are marked in the persistence db on object save, and released (set to null) on window close.
 * If the window closes unexpectedly, the session marker will not 
 * </ul>
 * 
 */
public class ClientSession {
	private static final String CLIENT_SESSION_ID = "persistence_client_session_id";

	private ClientSession() {
		super();
	}
	static final double TWO_PWR_28_DBL = 0x10000000;
	private static ClientSession theInstance;

	public static ClientSession get() {
		if (theInstance == null) {
			theInstance = new ClientSession();
		}
		return theInstance;
	}

	public void appShutdown() {
		theInstance = null;
	}
	public void initSession(){
		String cookie = Cookies.getCookie(CLIENT_SESSION_ID);
		if (cookie==null){
			Cookies.setCookie(CLIENT_SESSION_ID, String.valueOf((int)(Math.random()*TWO_PWR_28_DBL)));
		}
	}
	public String getSessionId(){
		 String cookie = Cookies.getCookie(CLIENT_SESSION_ID);
		 if (cookie==null){
			 throw new WrappedRuntimeException("Session not initialized", SuggestedAction.NOTIFY_WARNING);
		 }
		 return cookie;
	}
	
}
