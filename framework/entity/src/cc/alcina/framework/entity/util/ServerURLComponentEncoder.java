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
package cc.alcina.framework.entity.util;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.util.UrlComponentEncoder;
import cc.alcina.framework.entity.SEUtilities;

/**
 * @author Nick Reddel
 */
// only used in hosted mode
@Reflected
@Registration.Singleton(
	value = UrlComponentEncoder.class,
	priority = Registration.Priority.PREFERRED_LIBRARY)
public class ServerURLComponentEncoder implements UrlComponentEncoder {
	public String decode(String componentText) {
		return SEUtilities.decUtf8(componentText);
	}

	public String encode(String text) {
		return SEUtilities.encUtf8(text);
	}
}
