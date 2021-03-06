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

import com.totsp.gwittir.client.beans.annotations.Introspectable;

import cc.alcina.framework.common.client.actions.instances.ViewAction;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.reflection.Action;
import cc.alcina.framework.common.client.logic.reflection.Bean;
import cc.alcina.framework.common.client.logic.reflection.ObjectActions;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.Permission;

/**
 * 
 * @author Nick Reddel
 * 
 */
@Introspectable
public class Bindable extends BaseSourcesPropertyChangeEvents
		implements Serializable, IsBindable {
	@Bean(actions = @ObjectActions({ @Action(actionClass = ViewAction.class) }))
	@ObjectPermissions(create = @Permission(access = AccessLevel.ROOT), read = @Permission(access = AccessLevel.EVERYONE), write = @Permission(access = AccessLevel.ROOT), delete = @Permission(access = AccessLevel.ROOT))
	public static class BindableAdapter extends Bindable {
	}

	// FIXME - dirndl.2 - maybe remove?
	public static class BindableWithContext<T> extends Bindable
			implements HasContext<T> {
		private transient T _context;

		@Override
		public T _getContext() {
			return _context;
		}

		@Override
		public void _setContext(T _context) {
			this._context = _context;
		}
	}

	public interface HasContext<T> {
		public abstract T _getContext();

		public abstract void _setContext(T _context);
	}
}
