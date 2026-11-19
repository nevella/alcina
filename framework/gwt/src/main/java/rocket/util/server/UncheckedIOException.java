/*
 * Copyright Miroslav Pokorny
 *
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
package rocket.util.server;

/**
 * This IoException may be used to represent an unchecked io exception.
 * 
 * @author Miroslav Pokorny
 */
public class UncheckedIOException extends RuntimeException {
	public UncheckedIOException(final String message) {
		super(message);
	}

	public UncheckedIOException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public UncheckedIOException(final Throwable cause) {
		super(cause);
	}
}