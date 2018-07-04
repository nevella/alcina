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
package cc.alcina.framework.common.client.publication;

/**
 * Marker interface for publication content
 * 
 * @author nick@alcina.cc
 *
 */
public interface ContentDefinition {
	public String getPublicationType();

	@Override
	public String toString();

	default void initialiseContext() {
		// for implementations, if complex context-related logic. within a
		// try/finally loosecontext call
	}

	default void validate() throws Exception {
		// for implementations
	}
}
