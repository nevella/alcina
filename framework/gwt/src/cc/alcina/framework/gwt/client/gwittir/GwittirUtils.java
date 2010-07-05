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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.gwittir.validator.CompositeValidator;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ClassLookup;
import cc.alcina.framework.common.client.logic.reflection.ClientReflector;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.gwittir.widget.PasswordTextBox;
import cc.alcina.framework.gwt.client.gwittir.widget.SetBasedListBox;
import cc.alcina.framework.gwt.client.util.WidgetUtils;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.beans.Binding;
import com.totsp.gwittir.client.beans.annotations.Introspectable;
import com.totsp.gwittir.client.ui.AbstractBoundWidget;
import com.totsp.gwittir.client.ui.HasEnabled;
import com.totsp.gwittir.client.ui.ListBox;
import com.totsp.gwittir.client.ui.Renderer;
import com.totsp.gwittir.client.ui.TextBox;
import com.totsp.gwittir.client.validator.Validator;

@SuppressWarnings("unchecked")
/**
 *
 * @author Nick Reddel
 */
public class GwittirUtils {
	public static void refreshEmptyTextBoxes(Binding binding) {
		TransformManager.get().setIgnorePropertyChanges(true);
		List<Binding> l = binding.getChildren();
		for (Binding b : l) {
			if (b.getLeft().object instanceof TextBox
					|| b.getLeft().object instanceof PasswordTextBox) {
				AbstractBoundWidget tb = (AbstractBoundWidget) b.getLeft().object;
				if (tb.getValue() == null) {
					tb.setValue(" ");
					tb.setValue("");
				}
			}
		}
		TransformManager.get().setIgnorePropertyChanges(false);
	}

	public static void refreshTextBox(Binding binding, String propertyName) {
		TransformManager.get().setIgnorePropertyChanges(true);
		List<Binding> l = binding.getChildren();
		for (Binding b : l) {
			if (b.getLeft().object instanceof TextBox
					|| b.getLeft().object instanceof PasswordTextBox) {
				if (!propertyName.equals(b.getRight().property.getName())) {
					continue;
				}
				AbstractBoundWidget tb = (AbstractBoundWidget) b.getLeft().object;
				if (tb.getValue() == null) {
					tb.setValue(" ");
					tb.setValue("");
				} else {
					String s = tb.getValue().toString();
					tb.setValue(" ");
					tb.setValue(s);
				}
			}
		}
		TransformManager.get().setIgnorePropertyChanges(false);
	}

	public static void refreshAllTextBoxes(Binding binding) {
		TransformManager.get().setIgnorePropertyChanges(true);
		List<Binding> l = binding.getChildren();
		for (Binding b : l) {
			if (b.getLeft().object instanceof TextBox
					|| b.getLeft().object instanceof PasswordTextBox) {
				AbstractBoundWidget tb = (AbstractBoundWidget) b.getLeft().object;
				if (tb.getValue() == null) {
					tb.setValue(" ");
					tb.setValue("");
				} else {
					String s = tb.getValue().toString();
					tb.setValue(" ");
					tb.setValue(s);
				}
			}
		}
		TransformManager.get().setIgnorePropertyChanges(false);
	}

	public static List<Validator> getAllValidators(Binding b,
			List<Validator> vList) {
		if (vList == null) {
			vList = new ArrayList<Validator>();
		}
		if (b.getLeft() != null) {
			getAllValidators(b.getLeft().validator, vList);
		}
		if (b.getRight() != null) {
			getAllValidators(b.getRight().validator, vList);
		}
		if (b.getChildren() != null) {
			List<Binding> children = b.getChildren();
			for (Binding child : children) {
				getAllValidators(child, vList);
			}
		}
		return vList;
	}

	private static void getAllValidators(Validator v, List<Validator> vList) {
		if (v == null) {
			return;
		}
		if (v instanceof CompositeValidator) {
			CompositeValidator cv = (CompositeValidator) v;
			for (Validator v2 : cv.getValidators()) {
				getAllValidators(v2, vList);
			}
		}
		vList.add(v);
	}

	public static SetBasedListBox getForEnumAndRenderer(
			Class<? extends Enum> clazz, Renderer renderer) {
		return getForEnumAndRenderer(clazz, renderer, new ArrayList());
	}

	public static SetBasedListBox getForEnumAndRenderer(
			Class<? extends Enum> clazz, Renderer renderer,
			List<? extends Enum> ignore) {
		SetBasedListBox listBox = new SetBasedListBox();
		Enum[] enumValues = clazz.getEnumConstants();
		List options = new ArrayList(Arrays.asList(enumValues));
		for (Enum e : ignore) {
			options.remove(e);
		}
		listBox.setRenderer(renderer);
		listBox.setOptions(options);
		return listBox;
	}

	public static void disableChildren(HasWidgets w) {
		List<Widget> allChildren = WidgetUtils.allChildren(w);
		for (Widget widget : allChildren) {
			System.out.println(CommonUtils.classSimpleName(widget.getClass()));
			if (widget instanceof HasEnabled) {
				HasEnabled he = (HasEnabled) widget;
				he.setEnabled(false);
				System.out.println("---disabled");
			}
			if (widget instanceof ListBox) {
				ListBox lb = (ListBox) widget;
				lb.setEnabled(false);
				System.out.println("---disabled");
			}
		}
	}

	/**
	 * Note: no support for (deprecated) Instantiable and Bindable interfaces if on server
	 */
	public static boolean isIntrospectable(Class clazz) {
		if (GWT.isClient()) {
			return ClientReflector.get().beanInfoForClass(clazz) != null;
		}
		ClassLookup cl = CommonLocator.get().classLookup();
		while (clazz!=null && clazz != Object.class) {
			if (cl.getAnnotationForClass(clazz, Introspectable.class) != null) {
				return true;
			}
			clazz=clazz.getSuperclass();
		}
		return false;
	}
	public static ArrayList sortByStringValue(Collection c, Renderer renderer) {
		Map<String, List> m = new HashMap<String, List>();
		for (Object o : c) {
			String key = o == null ? null : (String)renderer.render(o);
			if (!m.containsKey(key)) {
				m.put(key, new ArrayList());
			}
			m.get(key).add(o);
		}
		List nullValues = m.get(null);
		m.remove(null);
		ArrayList<String> keys = new ArrayList<String>(m.keySet());
		Collections.sort(keys);
		ArrayList result = new ArrayList();
		if (nullValues != null) {
			result.addAll(nullValues);
		}
		for (String key : keys) {
			result.addAll(m.get(key));
		}
		return result;
	}

}
