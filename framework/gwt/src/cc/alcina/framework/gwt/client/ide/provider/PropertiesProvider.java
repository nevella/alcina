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

package cc.alcina.framework.gwt.client.ide.provider;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.WrappedRuntimeException.SuggestedAction;
import cc.alcina.framework.gwt.client.data.GeneralProperties;

/**
 *
 * @author <a href="mailto:nick@alcina.cc">Nick Reddel</a>
 */

 public class PropertiesProvider {

	private static PropertiesProviderSource provider;

	public static GeneralProperties getGeneralProperties() {
		if (provider != null) {
			return provider.getProperties();
		}
		throw new WrappedRuntimeException("No property provider registered",
				SuggestedAction.NOTIFY_ERROR);
	}
	

	public static void registerProvider(PropertiesProviderSource p) {
		provider = p;
	}

	public interface PropertiesProviderSource {
		public GeneralProperties getProperties();
	}
}
