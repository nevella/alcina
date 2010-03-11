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

package cc.alcina.framework.common.client;

/**
 * Design pattern: wrap non-runtime exceptions in this class, with a suggested
 * upper-level handling action
 * 
 * @author nick@alcina.cc
 * 
 */
public class WrappedRuntimeException extends RuntimeException {
	public enum SuggestedAction {
		CANCEL_STARTUP, NOTIFY_WARNING, NOTIFY_ERROR, EXPECTED_EXCEPTION,
		NOTIFY_AND_SHUTDOWN, HANDLE_INTERRUPTED_ACTION_SILENT,
		HTTP_RETRY_WAIT_LONG, HTTP_RETRY_WAIT_SHORT,
		NO_NOTIFICATION_AND_CONTINUE
	}

	private static final long serialVersionUID = 89976002L;

	public WrappedRuntimeException(Throwable cause) {
		this(cause, SuggestedAction.NOTIFY_WARNING);
	}

	public WrappedRuntimeException(Throwable cause, SuggestedAction sa) {
		super(cause);
		setSuggestedAction(sa);
	}

	public WrappedRuntimeException(String cause, SuggestedAction sa) {
		super(cause);
		setSuggestedAction(sa);
	}

	public WrappedRuntimeException(String message, Throwable cause) {
		this(message, cause, SuggestedAction.NOTIFY_WARNING);
	}

	public WrappedRuntimeException(String message, Throwable cause,
			SuggestedAction sa) {
		super(message, cause);
		setSuggestedAction(sa);
	}

	/**
	 */
	private SuggestedAction suggestedAction;

	/**
	 * Getter of the property <tt>suggestedAction</tt>
	 * 
	 * @return Returns the suggestedAction.
	 */
	public SuggestedAction getSuggestedAction() {
		return suggestedAction;
	}

	/**
	 * Setter of the property <tt>suggestedAction</tt>
	 * 
	 * @param suggestedAction
	 *            The suggestedAction to set.
	 */
	public void setSuggestedAction(SuggestedAction suggestedAction) {
		this.suggestedAction = suggestedAction;
	}
}
