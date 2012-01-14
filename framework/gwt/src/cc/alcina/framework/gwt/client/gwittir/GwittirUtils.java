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
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.gwittir.validator.CompositeValidator;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ClassLookup;
import cc.alcina.framework.common.client.logic.reflection.ClientReflector;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.gwittir.widget.PasswordTextBox;
import cc.alcina.framework.gwt.client.gwittir.widget.SetBasedListBox;
import cc.alcina.framework.gwt.client.util.WidgetUtils;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.beans.Binding;
import com.totsp.gwittir.client.beans.Converter;
import com.totsp.gwittir.client.beans.annotations.Introspectable;
import com.totsp.gwittir.client.ui.AbstractBoundWidget;
import com.totsp.gwittir.client.ui.HasEnabled;
import com.totsp.gwittir.client.ui.ListBox;
import com.totsp.gwittir.client.ui.Renderer;
import com.totsp.gwittir.client.validator.Validator;

@SuppressWarnings("unchecked")
/**
 *
 * @author Nick Reddel
 */
public class GwittirUtils {
	public static void refreshEmptyTextBoxes(Binding binding) {
		refreshTextBoxes(binding, null, true, true);
	}

	public static Collection convertCollection(Collection source,
			Converter converter) {
		ArrayList result = new ArrayList();
		for (Object object : source) {
			result.add(converter.convert(object));
		}
		return result;
	}

	public static void refreshTextBox(Binding binding, String propertyName) {
		refreshTextBoxes(binding, propertyName, true, false);
	}

	public static void refreshTextBoxes(Binding binding,
			String onlyPropertyName, boolean muteTransformManager,
			boolean onlyEmpties) {
		refreshFields(binding, onlyPropertyName, muteTransformManager,
				onlyEmpties, new FormFieldTypeForRefresh[] {
						FormFieldTypeForRefresh.TEXT,
						FormFieldTypeForRefresh.TEXT_AREA });
	}

	public static void refreshTextBoxesAndSelects(Binding binding,
			String onlyPropertyName, boolean muteTransformManager,
			boolean onlyEmpties) {
		refreshFields(binding, onlyPropertyName, muteTransformManager,
				onlyEmpties, new FormFieldTypeForRefresh[] {
						FormFieldTypeForRefresh.TEXT,
						FormFieldTypeForRefresh.TEXT_AREA,
						FormFieldTypeForRefresh.SELECT });
	}

	public enum FormFieldTypeForRefresh {
		TEXT, RADIO, CHECK, SELECT, TEXT_AREA
	}

	public static void refreshFields(Binding binding, String onlyPropertyName,
			boolean muteTransformManager, boolean onlyEmpties,
			FormFieldTypeForRefresh[] types) {
		List<Binding> allBindings = binding.provideAllBindings(null);
		List<FormFieldTypeForRefresh> lTypes = Arrays.asList(types);
		try {
			if (muteTransformManager) {
				TransformManager.get().setIgnorePropertyChanges(true);
			}
			for (Binding b : allBindings) {
				if (onlyPropertyName != null
						&& !onlyPropertyName.equals(b.getRight().property
								.getName())) {
					continue;
				}
				if (b.getLeft() == null || b.getLeft().object == null) {
					continue;
				}
				boolean satisfiesType = false;
				boolean isText = CommonUtils.simpleClassName(
						b.getLeft().object.getClass()).equals("TextBox")
						|| b.getLeft().object instanceof PasswordTextBox;
				satisfiesType |= lTypes.contains(FormFieldTypeForRefresh.TEXT)
						&& isText;
				boolean isTextArea = CommonUtils.simpleClassName(
						b.getLeft().object.getClass()).equals("TextArea");
				satisfiesType |= lTypes
						.contains(FormFieldTypeForRefresh.TEXT_AREA)
						&& isTextArea;
				boolean isSetBasedListBox = (b.getLeft().object instanceof SetBasedListBox);
				satisfiesType |= lTypes
						.contains(FormFieldTypeForRefresh.SELECT)
						&& isSetBasedListBox;
				AbstractBoundWidget tb = (AbstractBoundWidget) b.getLeft().object;
				if (tb instanceof HasBinding) {
					Binding subBinding = ((HasBinding) tb).getBinding();
					if (subBinding != null) {
						refreshFields(subBinding, onlyPropertyName,
								muteTransformManager, onlyEmpties, types);
					}
				}
				if (satisfiesType) {
					Object value = b.getRight().property.getAccessorMethod()
							.invoke(b.getRight().object,
									CommonUtils.EMPTY_OBJECT_ARRAY);
					Object tbValue = tb.getValue();
					if (onlyEmpties && tbValue != null) {
						continue;
					}
					Object other = isSetBasedListBox ? ((SetBasedListBox) tb)
							.provideOtherValue() : " ".equals(tbValue) ? ""
							: " ";
					tb.setValue(other);
					b.getRight().property.getMutatorMethod().invoke(
							b.getRight().object, new Object[] { value });
					tb.setValue(tbValue);
				}
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		} finally {
			if (muteTransformManager) {
				TransformManager.get().setIgnorePropertyChanges(false);
			}
		}
	}

	public static void refreshAllTextBoxes(Binding binding) {
		refreshTextBoxes(binding, null, true, false);
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
	 * Note: no support for (deprecated) Instantiable and Bindable interfaces if
	 * on server
	 */
	public static boolean isIntrospectable(Class clazz) {
		if (GWT.isClient()) {
			return ClientReflector.get().beanInfoForClass(clazz) != null;
		}
		ClassLookup cl = CommonLocator.get().classLookup();
		while (clazz != null && clazz != Object.class) {
			if (cl.getAnnotationForClass(clazz, Introspectable.class) != null) {
				return true;
			}
			clazz = clazz.getSuperclass();
		}
		return false;
	}

	public static ArrayList sortByStringValue(Collection c, Renderer renderer) {
		Map<String, List> m = new HashMap<String, List>();
		for (Object o : c) {
			String key = o == null ? null : (String) renderer.render(o);
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

	public static void focusFirstInput(Binding binding) {
		List<Binding> l = binding.getChildren();
		for (Binding b : l) {
			boolean isText = CommonUtils.simpleClassName(
					b.getLeft().object.getClass()).equals("TextBox")
					|| b.getLeft().object instanceof PasswordTextBox;
			if (isText) {
				((Focusable) b.getLeft().object).setFocus(true);
			}
		}
	}
}
