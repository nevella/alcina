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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.gwt.user.client.ui.FlowPanel;
import com.totsp.gwittir.client.beans.Binding;
import com.totsp.gwittir.client.ui.AbstractBoundWidget;
import com.totsp.gwittir.client.ui.BoundWidget;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.ClassRef;
import cc.alcina.framework.common.client.logic.domaintransform.ClassRef.ClassRefSimpleNameRenderer;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.search.PersistentObjectCriteriaGroup;
import cc.alcina.framework.gwt.client.gwittir.BasicBindingAction;
import cc.alcina.framework.gwt.client.gwittir.GwittirUtils;
import cc.alcina.framework.gwt.client.gwittir.widget.SetBasedListBox;
import cc.alcina.framework.gwt.client.objecttree.TreeRenderer;

/**
 * @author Nick Reddel
 */
@Registration({ TreeRenderer.class, PersistentObjectCriteriaGroup.class })
public class PersistentObjectCriteriaGroupRenderer
		extends CriteriaGroupRenderer<PersistentObjectCriteriaGroup> {
	@Override
	public boolean isSingleLineCustomiser() {
		return true;
	}

	@Override
	public BoundWidgetProvider renderCustomiser() {
		return new BoundWidgetProvider() {
			@Override
			public BoundWidget get() {
				return new EntitySelectBox();
			}
		};
	}

	public static class EntitySelectBox
			extends AbstractBoundWidget<PersistentObjectCriteriaGroup> {
		private PersistentObjectCriteriaGroup value;

		private FlowPanel fp;

		SetBasedListBox box;

		public EntitySelectBox() {
			this.fp = new FlowPanel();
			box = new SetBasedListBox();
			Set<ClassRef> all = ClassRef.all();
			List<ClassRef> list = all.stream()
					.filter(new Predicate<ClassRef>() {
						@Override
						public boolean test(ClassRef o) {
							try {
								Object templateInstance = Reflections
										.at(o.getRefClass()).templateInstance();
								return templateInstance instanceof Entity;
							} catch (Exception e) {
								return false;
							}
						}
					}).collect(Collectors.toList());
			list.add(0, null);
			ArrayList sorted = GwittirUtils.sortByStringValue(list,
					ClassRefSimpleNameRenderer.INSTANCE);
			box.setRenderer(ClassRefSimpleNameRenderer.INSTANCE);
			box.setSortOptionsByToString(false);
			box.setOptions(sorted);
			fp.add(box);
			initWidget(fp);
			setAction(new EntitySelectBoxBindingAction());
		}

		@Override
		public PersistentObjectCriteriaGroup getValue() {
			return this.value;
		}

		@Override
		public void setValue(PersistentObjectCriteriaGroup value) {
			this.value = value;
		}
	}

	private static class EntitySelectBoxBindingAction extends
			BasicBindingAction<BoundWidget<PersistentObjectCriteriaGroup>> {
		@Override
		protected void set0(BoundWidget widget) {
			EntitySelectBox hsb = (EntitySelectBox) widget;
			binding.getChildren()
					.add(new Binding(hsb.box, "value",
							((PersistentObjectCriteriaGroup) hsb.getModel())
									.soleCriterion(),
							"classRef"));
			binding.setLeft();
		}
	}
}
