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

package cc.alcina.framework.gwt.client.logic;


import cc.alcina.framework.common.client.WrappedRuntimeException;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 *
 * @author Nick Reddel
 */

 public class StandardAsyncCallback<T> implements AsyncCallback<T> {
	public void onFailure(Throwable caught) {
		throw new WrappedRuntimeException(caught);
	}

	public void onSuccess(T result) {
	}
	public static class CancellableAsyncCallback<T> extends StandardAsyncCallback<T> {
		public void onFailure(Throwable caught) {
			throw new WrappedRuntimeException(caught);
		}
		
		public void onSuccess(T result) {
		}
		public void setCancelled(boolean cancelled) {
			this.cancelled = cancelled;
		}

		public boolean isCancelled() {
			return cancelled;
		}
		private boolean cancelled;
	}
}
