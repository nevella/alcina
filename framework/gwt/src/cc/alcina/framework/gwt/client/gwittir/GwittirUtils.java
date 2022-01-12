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

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.beans.Binding;
import com.totsp.gwittir.client.beans.Converter;
import com.totsp.gwittir.client.beans.annotations.Introspectable;
import com.totsp.gwittir.client.ui.AbstractBoundWidget;
import com.totsp.gwittir.client.ui.Checkbox;
import com.totsp.gwittir.client.ui.HasEnabled;
import com.totsp.gwittir.client.ui.ListBox;
import com.totsp.gwittir.client.ui.RadioButton;
import com.totsp.gwittir.client.ui.Renderer;
import com.totsp.gwittir.client.ui.table.Field;
import com.totsp.gwittir.client.validator.Validator;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.gwittir.validator.CompositeValidator;
import cc.alcina.framework.common.client.logic.ExtensibleEnum;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ClassLookup;
import cc.alcina.framework.common.client.logic.reflection.Bean;
import cc.alcina.framework.common.client.logic.reflection.ClientReflector;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.gwittir.widget.PasswordTextBox;
import cc.alcina.framework.gwt.client.gwittir.widget.RadioButtonList;
import cc.alcina.framework.gwt.client.gwittir.widget.SetBasedListBox;
import cc.alcina.framework.gwt.client.util.WidgetUtils;

/**
 *
 * @author Nick Reddel
 */
public class GwittirUtils {
	public static void commitAllTextBoxes(Binding binding) {
		refreshTextBoxes(binding, null, false, false, true);
	}

	public static Collection convertCollection(Collection source,
			Converter converter) {
		ArrayList result = new ArrayList();
		for (Object object : source) {
			result.add(converter.convert(object));
		}
		return result;
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

	public static void focusFirstInput(Binding binding) {
		List<Binding> l = binding.getChildren();
		for (Binding b : l) {
			boolean isText = CommonUtils
					.simpleClassName(b.getLeft().object.getClass())
					.equals("TextBox")
					|| b.getLeft().object instanceof PasswordTextBox;
			if (isText) {
				((Focusable) b.getLeft().object).setFocus(true);
			}
		}
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

	public static int getFieldIndex(Field[] fields, String propertyName) {
		int i = 0;
		for (Field f : fields) {
			if (f.getPropertyName().equals(propertyName)) {
				return i;
			}
			i++;
		}
		return -1;
	}

	public static SetBasedListBox getForEnumAndRenderer(
			Class<? extends Enum> clazz, Renderer renderer) {
		return getForEnumAndRenderer(clazz, renderer, new ArrayList());
	}

	public static SetBasedListBox getForEnumAndRenderer(
			Class<? extends Enum> clazz, Renderer renderer,
			Collection<? extends Enum> ignore) {
		return getForEnumAndRenderer(clazz, renderer, ignore, false);
	}

	public static SetBasedListBox getForEnumAndRenderer(
			Class<? extends Enum> clazz, Renderer renderer,
			Collection<? extends Enum> ignore, boolean withNull) {
		SetBasedListBox listBox = new SetBasedListBox();
		Enum[] enumValues = clazz.getEnumConstants();
		List options = new ArrayList(Arrays.asList(enumValues));
		if (withNull) {
			options.add(0, null);
		}
		if (ignore != null) {
			options.removeAll(ignore);
		}
		listBox.setRenderer(renderer);
		listBox.setOptions(options);
		return listBox;
	}

	public static SetBasedListBox getForExtensibleEnumAndRenderer(
			Class<? extends ExtensibleEnum> clazz, Renderer renderer,
			Collection<? extends ExtensibleEnum> ignore) {
		SetBasedListBox listBox = new SetBasedListBox();
		List options = new ArrayList(ExtensibleEnum.values(clazz));
		options.removeAll(ignore);
		listBox.setRenderer(renderer);
		listBox.setOptions(options);
		return listBox;
	}

	/**
	 * Note: no support for (deprecated) Instantiable and Bindable interfaces if
	 * on server
	 */
	public static boolean isIntrospectable(Class clazz) {
		if (GWT.isScript()) {
			return ClientReflector.get().beanInfoForClass(clazz) != null;
		}
		ClassLookup cl = Reflections;
		while (clazz != null && clazz != Object.class) {
			if (cl.getAnnotationForClass(clazz, Introspectable.class) != null
					|| cl.getAnnotationForClass(clazz, Bean.class) != null) {
				return true;
			}
			clazz = clazz.getSuperclass();
		}
		return false;
	}

	public static void refreshAllTextBoxes(Binding binding) {
		refreshTextBoxes(binding, null, true, false, false);
	}

	public static void refreshEmptyTextBoxes(Binding binding) {
		refreshTextBoxes(binding, null, true, true, false);
	}

	public static void refreshFields(Binding binding, String onlyPropertyName,
			boolean muteTransformManager, boolean onlyEmpties,
			boolean onlyCommit, FormFieldTypeForRefresh[] types) {
		List<Binding> allBindings = binding.provideAllBindings(null);
		List<FormFieldTypeForRefresh> lTypes = Arrays.asList(types);
		try {
			if (muteTransformManager) {
				TransformManager.get().setIgnorePropertyChanges(true);
			}
			for (Binding b : allBindings) {
				if (b.getLeft() == null || b.getLeft().object == null
						|| b.getRight() == null
						|| !(b.getLeft().object instanceof AbstractBoundWidget)) {
					continue;
				}
				if (onlyPropertyName != null && !onlyPropertyName
						.equals(b.getRight().property.getName())) {
					continue;
				}
				boolean satisfiesType = false;
				boolean isText = CommonUtils
						.simpleClassName(b.getLeft().object.getClass())
						.equals("TextBox")
						|| b.getLeft().object instanceof PasswordTextBox;
				satisfiesType |= lTypes.contains(FormFieldTypeForRefresh.TEXT)
						&& isText;
				boolean isTextArea = CommonUtils
						.simpleClassName(b.getLeft().object.getClass())
						.equals("TextArea");
				satisfiesType |= lTypes.contains(
						FormFieldTypeForRefresh.TEXT_AREA) && isTextArea;
				boolean isSetBasedListBox = (b
						.getLeft().object instanceof SetBasedListBox);
				satisfiesType |= lTypes.contains(FormFieldTypeForRefresh.SELECT)
						&& isSetBasedListBox;
				boolean isCheckbox = b.getLeft().object instanceof Checkbox;
				satisfiesType |= lTypes.contains(FormFieldTypeForRefresh.CHECK)
						&& isCheckbox;
				boolean isRadio = (b.getLeft().object instanceof RadioButton
						|| b.getLeft().object instanceof RadioButtonList);
				satisfiesType |= lTypes.contains(FormFieldTypeForRefresh.RADIO)
						&& isRadio;
				AbstractBoundWidget tb = (AbstractBoundWidget) b
						.getLeft().object;
				if (!tb.isVisible()) {
					continue;
				}
				if (tb instanceof HasBinding) {
					Binding subBinding = ((HasBinding) tb).getBinding();
					if (subBinding != null) {
						refreshFields(subBinding, onlyPropertyName,
								muteTransformManager, onlyEmpties, onlyCommit,
								types);
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
					if (onlyCommit) {
						b.setRight();
					} else {
						Object other = isSetBasedListBox
								? ((SetBasedListBox) tb).provideOtherValue()
								: " ".equals(tbValue) ? "" : " ";
						other = isCheckbox
								? !(CommonUtils.bv((Boolean) tb.getValue()))
								: other;
						tb.setValue(other);
						b.getRight().property.getMutatorMethod().invoke(
								b.getRight().object, new Object[] { value });
						tb.setValue(tbValue);
					}
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

	public static void refreshTextBox(Binding binding, String propertyName) {
		refreshTextBoxes(binding, propertyName, true, false, false);
	}

	public static void refreshTextBoxes(Binding binding,
			String onlyPropertyName, boolean muteTransformManager,
			boolean onlyEmpties, boolean onlyCommit) {
		refreshFields(binding, onlyPropertyName, muteTransformManager,
				onlyEmpties, onlyCommit,
				new FormFieldTypeForRefresh[] { FormFieldTypeForRefresh.TEXT,
						FormFieldTypeForRefresh.TEXT_AREA });
	}

	public static void refreshTextBoxesAndSelects(Binding binding,
			String onlyPropertyName, boolean muteTransformManager,
			boolean onlyEmpties, boolean onlyCommit) {
		refreshFields(binding, onlyPropertyName, muteTransformManager,
				onlyEmpties, onlyCommit,
				new FormFieldTypeForRefresh[] { FormFieldTypeForRefresh.TEXT,
						FormFieldTypeForRefresh.TEXT_AREA,
						FormFieldTypeForRefresh.SELECT });
	}

	public static void renameField(Field[] fields, String propertyName,
			String label) {
		for (Field f : fields) {
			if (f.getPropertyName().equals(propertyName)) {
				f.setLabel(label);
			}
		}
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

	public enum FormFieldTypeForRefresh {
		TEXT, RADIO, CHECK, SELECT, TEXT_AREA
	}
}
