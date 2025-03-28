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
 *
 * 
 *
 */
public class WrappedRuntimeException extends RuntimeException {
	public static void throwWrapped(Throwable e) {
		throw (wrap(e));
	}

	public static RuntimeException wrap(Throwable e) {
		if (e instanceof RuntimeException) {
			return (RuntimeException) e;
		} else {
			return new WrappedRuntimeException(e);
		}
	}

	public WrappedRuntimeException() {
	}

	public WrappedRuntimeException(String message, Throwable cause) {
		super(message, cause);
	}

	public WrappedRuntimeException(Throwable cause) {
		super(cause);
	}

	public static Throwable unwrap(RuntimeException e) {
		if (e instanceof WrappedRuntimeException) {
			return e.getCause();
		} else {
			return e;
		}
	}
}
