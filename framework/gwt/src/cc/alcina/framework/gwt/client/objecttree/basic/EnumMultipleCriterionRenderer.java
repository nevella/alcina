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
package cc.alcina.framework.gwt.client.objecttree.basic;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.search.EnumMultipleCriterion;
import cc.alcina.framework.gwt.client.gwittir.provider.ListBoxEnumProvider;
import cc.alcina.framework.gwt.client.gwittir.renderer.FriendlyEnumRenderer;
import cc.alcina.framework.gwt.client.gwittir.widget.SetBasedListBox;
import cc.alcina.framework.gwt.client.objecttree.TreeRenderer;

import com.totsp.gwittir.client.beans.Binding;
import com.totsp.gwittir.client.beans.BindingBuilder;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

/**
 * 
 * @author Nick Reddel
 */
@RegistryLocation(registryPoint = TreeRenderer.class, targetClass = EnumMultipleCriterion.class)
public class EnumMultipleCriterionRenderer<T extends EnumMultipleCriterion>
		extends SearchCriterionRenderer<T> {
	private Binding parentBinding;

	@Override
	public String renderablePropertyName() {
		return "value";
	}

	@Override
	public void parentBinding(Binding parentBinding) {
		this.parentBinding = parentBinding;
	}

	@Override
	public BoundWidgetProvider renderCustomiser() {
		ListBoxEnumProvider provider = new ListBoxEnumProvider(getRenderable()
				.enumClass()) {
			@Override
			public SetBasedListBox get() {
				SetBasedListBox widget = super.get();
				Binding binding = BindingBuilder.bind(widget)
						.onLeftProperty("value").toRight(getRenderable())
						.onRightProperty(renderablePropertyName()).toBinding();
				if (parentBinding != null) {
					parentBinding.getChildren().add(binding);
				}
				return widget;
			}
		};
		provider.setMultiple(true);
		provider.setVisibleItemCount(4);
		provider.setRenderer(new FriendlyEnumRenderer());
		return provider;
	}
}
