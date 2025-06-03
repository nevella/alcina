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

import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.PropertyOrder;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;

/**
 * <p>
 * Generally use this as the base class for java beans that are used by
 * ReflectedSerializer (it's not strictly necessary to extend
 * BaseSourcesPropertyChangeEvents, but the @Bean is required and Serializable
 * is advisable)
 *
 * @author Nick Reddel
 *
 */
@Bean
public class Bindable extends BaseSourcesPropertyChangeEvents
		implements Serializable, IsBindable {
	public static class BindableAdapter extends Bindable {
	}

	@Bean(PropertySource.FIELDS)
	public abstract static class Fields extends Bindable {
		@Display.AllProperties
		@PropertyOrder(fieldOrder = true)
		@ObjectPermissions(
			read = @Permission(access = AccessLevel.EVERYONE),
			write = @Permission(access = AccessLevel.EVERYONE))
		public abstract static class All extends Bindable.Fields {
		}
	}

	public interface HasBindableContext<T> {
		public abstract T _getContext();

		public abstract void _setContext(T _context);
	}
}
