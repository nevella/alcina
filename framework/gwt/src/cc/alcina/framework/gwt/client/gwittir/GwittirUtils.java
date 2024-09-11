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

import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.RadioButton;
import com.totsp.gwittir.client.beans.Binding;
import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;
import com.totsp.gwittir.client.ui.AbstractBoundWidget;
import com.totsp.gwittir.client.ui.Checkbox;
import com.totsp.gwittir.client.ui.Renderer;
import com.totsp.gwittir.client.ui.table.Field;
import com.totsp.gwittir.client.validator.Validator;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.gwittir.validator.CompositeValidator;
import cc.alcina.framework.common.client.logic.ExtensibleEnum;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.edit.StringInput;
import cc.alcina.framework.gwt.client.gwittir.widget.PasswordTextBox;
import cc.alcina.framework.gwt.client.gwittir.widget.RadioButtonList;
import cc.alcina.framework.gwt.client.gwittir.widget.SetBasedListBox;

/**
 *
 * @author Nick Reddel
 *
 *         FIXME - reflection - prune
 */
public class GwittirUtils {
	public static void commitAllTextBoxes(Binding binding) {
		refreshTextBoxes(binding, null, false, false, true);
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

	public static boolean isIntrospectable(Class clazz) {
		return Reflections.at(clazz).has(Bean.class);
	}

	public static void refreshAllTextBoxes(Binding binding) {
		refreshTextBoxes(binding, null, true, false, false);
	}

	public static void refreshEmptyTextBoxes(Binding binding) {
		refreshTextBoxes(binding, null, true, true, false);
	}

	/*
	 * FIXME - dirndl - the 'should be refreshed' should be an interface on the
	 * editors, not this type checking
	 */
	public static void refreshFields(Binding binding, String onlyPropertyName,
			boolean muteTransformManager, boolean onlyEmpties,
			boolean onlyCommit, FormFieldTypeForRefresh[] types) {
		List<Binding> allBindings = binding.provideAllBindings(null);
		List<FormFieldTypeForRefresh> lTypes = Arrays.asList(types);
		try {
			if (muteTransformManager && TransformManager.hasInstance()) {
				TransformManager.get().setIgnorePropertyChanges(true);
			}
			for (Binding b : allBindings) {
				if (b.getLeft() == null) {
					continue;
				}
				SourcesPropertyChangeEvents leftObject = b.getLeft().object;
				if (leftObject == null || b.getRight() == null
						|| !(leftObject instanceof AbstractBoundWidget
								|| leftObject instanceof Model.Value)) {
					continue;
				}
				if (onlyPropertyName != null && !onlyPropertyName
						.equals(b.getRight().property.getName())) {
					continue;
				}
				boolean satisfiesType = false;
				boolean isText = CommonUtils
						.simpleClassName(leftObject.getClass())
						.equals("TextBox")
						|| leftObject instanceof PasswordTextBox
						|| leftObject instanceof StringInput;
				satisfiesType |= lTypes.contains(FormFieldTypeForRefresh.TEXT)
						&& isText;
				boolean isTextArea = CommonUtils
						.simpleClassName(leftObject.getClass())
						.equals("TextArea");
				satisfiesType |= lTypes.contains(
						FormFieldTypeForRefresh.TEXT_AREA) && isTextArea;
				boolean isSetBasedListBox = (leftObject instanceof SetBasedListBox);
				satisfiesType |= lTypes.contains(FormFieldTypeForRefresh.SELECT)
						&& isSetBasedListBox;
				boolean isCheckbox = leftObject instanceof Checkbox;
				satisfiesType |= lTypes.contains(FormFieldTypeForRefresh.CHECK)
						&& isCheckbox;
				boolean isRadio = (leftObject instanceof RadioButton
						|| leftObject instanceof RadioButtonList);
				satisfiesType |= lTypes.contains(FormFieldTypeForRefresh.RADIO)
						&& isRadio;
				if (leftObject instanceof AbstractBoundWidget) {
					AbstractBoundWidget tb = (AbstractBoundWidget) leftObject;
					if (!tb.isVisible()) {
						continue;
					}
				}
				if (leftObject instanceof HasBinding) {
					Binding subBinding = ((HasBinding) leftObject).getBinding();
					if (subBinding != null) {
						refreshFields(subBinding, onlyPropertyName,
								muteTransformManager, onlyEmpties, onlyCommit,
								types);
					}
				}
				if (satisfiesType) {
					Object value = b.getRight().property
							.get(b.getRight().object);
					Object inputValue = null;
					if (leftObject instanceof AbstractBoundWidget) {
						inputValue = ((AbstractBoundWidget) leftObject)
								.getValue();
					} else if (leftObject instanceof Model.Value) {
						inputValue = ((Model.Value) leftObject).getValue();
					} else {
						throw new UnsupportedOperationException();
					}
					if (onlyEmpties && inputValue != null) {
						continue;
					}
					if (onlyCommit) {
						if (leftObject instanceof StringInput) {
							/*
							 * StringInput.value doesn't exactly track
							 * <element>.value
							 */
							((StringInput) leftObject).commitCurrentValue();
						}
						b.setRight();
					} else {
						Object other = isSetBasedListBox
								? ((SetBasedListBox) leftObject)
										.provideOtherValue()
								: " ".equals(inputValue) ? "" : " ";
						other = isCheckbox
								? !(CommonUtils.bv((Boolean) inputValue))
								: other;
						if (leftObject instanceof AbstractBoundWidget) {
							AbstractBoundWidget abw = (AbstractBoundWidget) leftObject;
							abw.setValue(other);
							b.getRight().property.set(b.getRight().object,
									value);
							abw.setValue(inputValue);
						} else if (leftObject instanceof Model.Value) {
							b.getRight().property.set(b.getRight().object,
									value);
						} else {
							throw new UnsupportedOperationException();
						}
					}
				}
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		} finally {
			if (muteTransformManager && TransformManager.hasInstance()) {
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

	public enum FormFieldTypeForRefresh {
		TEXT, RADIO, CHECK, SELECT, TEXT_AREA
	}
}
