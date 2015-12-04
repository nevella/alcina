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
package cc.alcina.framework.gwt.client.gwittir;

import com.totsp.gwittir.client.action.BindingAction;
import com.totsp.gwittir.client.beans.Binding;
import com.totsp.gwittir.client.ui.BoundWidget;

/**
 * 
 * @author Nick Reddel
 */
public abstract class BasicBindingAction<T extends BoundWidget<?>> implements BindingAction<T>, HasBinding {
	protected Binding binding = new Binding();

	public Binding getBinding() {
		return this.binding;
	}

	public void bind(T widget) {
		binding.bind();
	}

	boolean wasSet = false;

	public void execute(T model) {
	}

	public void set(BoundWidget widget) {
		if (wasSet) {
			if (isSetLeftOnLaterSets()) {
				binding.setLeft();
			}
			return;
		}
		set0(widget);
		wasSet = true;
	}

	protected boolean isSetLeftOnLaterSets() {
		return false;
	}


	protected abstract void set0(BoundWidget widget);

	public void unbind(T widget) {
		binding.unbind();
	}
}
