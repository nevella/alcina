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

import java.lang.annotation.Annotation;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.WrappedRuntimeException.SuggestedAction;
import cc.alcina.framework.common.client.gwittir.validator.CompositeValidator;
import cc.alcina.framework.common.client.gwittir.validator.DateToLongStringConverter;
import cc.alcina.framework.common.client.gwittir.validator.LongValidator;
import cc.alcina.framework.common.client.gwittir.validator.ParameterisedValidator;
import cc.alcina.framework.common.client.gwittir.validator.ServerUniquenessValidator;
import cc.alcina.framework.common.client.gwittir.validator.ShortDateValidator;
import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.domaintransform.spi.PropertyAccessor;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.Association;
import cc.alcina.framework.common.client.logic.reflection.BeanInfo;
import cc.alcina.framework.common.client.logic.reflection.ClientBeanReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientPropertyReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientReflector;
import cc.alcina.framework.common.client.logic.reflection.CustomiserInfo;
import cc.alcina.framework.common.client.logic.reflection.DisplayInfo;
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.PropertyPermissions;
import cc.alcina.framework.common.client.logic.reflection.ValidatorInfo;
import cc.alcina.framework.common.client.logic.reflection.Validators;
import cc.alcina.framework.common.client.logic.reflection.VisualiserInfo;
import cc.alcina.framework.common.client.provider.TextProvider;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.CommonUtils.DateStyle;
import cc.alcina.framework.gwt.client.gwittir.customiser.Customiser;
import cc.alcina.framework.gwt.client.gwittir.provider.ExpandableDomainNodeCollectionLabelProvider;
import cc.alcina.framework.gwt.client.gwittir.provider.FriendlyEnumLabelProvider;
import cc.alcina.framework.gwt.client.gwittir.provider.ListBoxCollectionProvider;
import cc.alcina.framework.gwt.client.gwittir.provider.ListBoxEnumProvider;
import cc.alcina.framework.gwt.client.gwittir.renderer.DisplayNameRenderer;
import cc.alcina.framework.gwt.client.gwittir.widget.DateBox;
import cc.alcina.framework.gwt.client.ide.widget.RenderingLabel;
import cc.alcina.framework.gwt.client.widget.RelativePopupValidationFeedback;

import com.totsp.gwittir.client.beans.BeanDescriptor;
import com.totsp.gwittir.client.beans.Converter;
import com.totsp.gwittir.client.beans.Introspector;
import com.totsp.gwittir.client.beans.Property;
import com.totsp.gwittir.client.ui.BoundWidget;
import com.totsp.gwittir.client.ui.Renderer;
import com.totsp.gwittir.client.ui.table.Field;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;
import com.totsp.gwittir.client.ui.util.BoundWidgetTypeFactory;
import com.totsp.gwittir.client.validator.IntegerValidator;
import com.totsp.gwittir.client.validator.Validator;

@SuppressWarnings("unchecked")
/**
 *
 * @author Nick Reddel
 */
public class GwittirBridge implements PropertyAccessor {
	private Map<Class, Validator> validatorMap = new HashMap<Class, Validator>();
	{
		validatorMap.put(Integer.class, IntegerValidator.INSTANCE);
		validatorMap.put(int.class, IntegerValidator.INSTANCE);
		validatorMap.put(Long.class, LongValidator.INSTANCE);
		validatorMap.put(long.class, LongValidator.INSTANCE);
		validatorMap.put(Date.class, ShortDateValidator.INSTANCE);
	}

	private Map<Class, BeanDescriptor> descriptorClassLookup = new HashMap<Class, BeanDescriptor>();

	public BeanDescriptor getDescriptorForClass(Class c) {
		BeanDescriptor bd = descriptorClassLookup.get(c);
		if (bd == null) {
			Object o = ClientReflector.get().getTemplateInstance(c);
			bd = Introspector.INSTANCE.getDescriptor(o);
			descriptorClassLookup.put(c, bd);
		}
		return bd;
	}

	// private Set<String> reffedDescriptor = new LinkedHashSet<String>();
	public BeanDescriptor getDescriptor(Object o) {
		Class c = o.getClass();
		// if (reffedDescriptor!=null &&
		// !reffedDescriptor.contains(c.getName())){
		// reffedDescriptor.add(c.getName());
		// System.out.println(c.getName());
		// }
		BeanDescriptor bd = descriptorClassLookup.get(c);
		if (bd == null) {
			bd = Introspector.INSTANCE.getDescriptor(o);
			descriptorClassLookup.put(c, bd);
		}
		return bd;
	}

	public Property getProperty(Object o, String propertyName) {
		return getDescriptor(o).getProperty(propertyName);
	}

	public Property getPropertyForClass(Class c, String propertyName) {
		return getDescriptorForClass(c).getProperty(propertyName);
	}

	public Object getPropertyValue(Object o, String propertyName) {
		try {
			BeanDescriptor bd = getDescriptor(o);
			return bd.getProperty(propertyName).getAccessorMethod().invoke(o,
					null);
		} catch (Exception e) {
			throw new WrappedRuntimeException(CommonUtils.format(
					"Unable to get property %1 for object %2", propertyName, o
							.getClass().getName()), e,
					SuggestedAction.NOTIFY_WARNING);
		}
	}

	public boolean hasDescriptor(Class clazz) {
		try {
			return ClientReflector.get().beanInfoForClass(clazz) != null;
		} catch (RuntimeException re) {
			return false;
		}
	}

	private GwittirBridge() {
		super();
	}

	private static GwittirBridge INSTANCE = new GwittirBridge();

	// will be compiled to a field ref
	public static GwittirBridge get() {
		return INSTANCE;
	}

	public static class BoundWidgetTypeFactorySimpleGenerator extends
			BoundWidgetTypeFactory {
		public BoundWidgetTypeFactorySimpleGenerator() {
			super(true);
			add(Date.class, DateBox.PROVIDER);
		}

		public BoundWidgetProvider getWidgetProvider(Class type) {
			if (type.isEnum()) {
				return new ListBoxEnumProvider(type, true);
			}
			return super.getWidgetProvider(type);
		}
	}

	public static BoundWidgetTypeFactorySimpleGenerator SIMPLE_FACTORY = new BoundWidgetTypeFactorySimpleGenerator();

	public Field[] fieldsForReflectedObjectAndSetupWidgetFactory(Object obj,
			BoundWidgetTypeFactory factory, boolean editableWidgets,
			boolean multiple) {
		return fieldsForReflectedObjectAndSetupWidgetFactory(obj, factory,
				editableWidgets, multiple, null);
	}

	private List<String> ignoreProperties;

	public Field getField(Class c, String propertyName,
			boolean editableWidgets, boolean multiple) {
		return getField(c, propertyName, editableWidgets, multiple,
				SIMPLE_FACTORY, null);
	}

	public Field getField(Class c, String propertyName,
			boolean editableWidgets, boolean multiple,
			BoundWidgetTypeFactory factory, Object obj) {
		ClientBeanReflector bi = ClientReflector.get().beanInfoForClass(c);
		Collection<ClientPropertyReflector> prs = bi.getPropertyReflectors()
				.values();
		BeanInfo beanInfo = bi.getAnnotation(BeanInfo.class);
		ObjectPermissions op = bi.getAnnotation(ObjectPermissions.class);
		obj = obj != null ? obj : ClientReflector.get().getTemplateInstance(c);
		ClientPropertyReflector pr = bi.getPropertyReflectors().get(
				propertyName);
		Property p = getProperty(obj, pr.getPropertyName());
		BoundWidgetProvider bwp = factory.getWidgetProvider(p.getType());
		if (pr != null && pr.getGwPropertyInfo() != null) {
			PropertyPermissions pp = pr
					.getAnnotation(PropertyPermissions.class);
			VisualiserInfo visualiserInfo = pr.getGwPropertyInfo();
			Association association = pr.getAnnotation(Association.class);
			boolean fieldVisible = PermissionsManager.get()
					.checkEffectivePropertyPermission(op, pp, obj, true)
					&& visualiserInfo != null
					&& PermissionsManager.get().isPermissible(obj,
							visualiserInfo.visible())
					&& ((visualiserInfo.displayInfo().displayMask() & DisplayInfo.DISPLAY_AS_PROPERTY) != 0);
			if (!fieldVisible) {
				return null;
			}
			boolean propertyIsCollection = (p.getType() == Set.class);
			boolean fieldEditable = editableWidgets
					&& PermissionsManager.get()
							.checkEffectivePropertyPermission(op, pp, obj,
									false)
					&& ((visualiserInfo.displayInfo().displayMask() & DisplayInfo.DISPLAY_RO) == 0);
			;
			Class domainType = p.getType();
			domainType = (association == null || !propertyIsCollection || association
					.implementationClass() == void.class) ? domainType
					: association.implementationClass();
			boolean isDomainClass = GwittirBridge.get().hasDescriptor(
					domainType);
			if (bwp == null && isDomainClass) {
				bwp = new ListBoxCollectionProvider(domainType,
						propertyIsCollection);
			}
			boolean isEnum = domainType.isEnum();
			if (bwp == null && isEnum) {
				bwp = fieldEditable ? new ListBoxEnumProvider(domainType)
						: NOWRAP_LABEL_PROVIDER;
			}
			if (bwp != null && !fieldEditable) {
				if (isDomainClass) {
					bwp = propertyIsCollection ? new ExpandableDomainNodeCollectionLabelProvider(
							MAX_EXPANDABLE_LABEL_LENGTH, true)
							: DN_LABEL_PROVIDER;
				} else {
					if (domainType == Date.class) {
						bwp = AU_DATE_PROVIDER;
					} else if (isEnum) {
						bwp = FRIENDLY_ENUM_LABEL_PROVIDER_INSTANCE;
					} else if (domainType == boolean.class
							|| domainType == Boolean.class) {
						bwp = YES_NO_LABEL_PROVIDER;
					} else {
						bwp = NOWRAP_LABEL_PROVIDER;
					}
				}
			}
			CustomiserInfo customiserInfo = pr
					.getAnnotation(CustomiserInfo.class);
			if (customiserInfo != null || (obj instanceof HasCustomiser)) {
				Customiser customiser;
				if (customiserInfo == null) {
					customiser = ((HasCustomiser) obj).customiser();
				} else {
					customiser = (Customiser) ClientReflector.get()
							.newInstance(customiserInfo.customiserClass(), 0);
				}
				bwp = customiser.getProvider(fieldEditable, domainType,
						multiple, customiserInfo);
			}
			if (bwp != null) {
				RelativePopupValidationFeedback vf = null;
				Validator validator = null;
				if (fieldEditable) {
					vf = new RelativePopupValidationFeedback(
							multiple ? RelativePopupValidationFeedback.BOTTOM
									: RelativePopupValidationFeedback.RIGHT);
					vf
							.setCss(multiple ? null
									: "gwittir-ValidationPopup-right");
					validator = getValidator(domainType, obj, pr
							.getPropertyName(), vf);
				}
				return new Field(pr.getPropertyName(), TextProvider.get()
						.getLabelText(c, pr), bwp, validator, vf,getDefaultConverter(p.getType()));
			}
		} else if (beanInfo.allPropertiesVisualisable()
				&& PermissionsManager.get().checkEffectivePropertyPermission(
						op, null, obj, false)) {
			// no property info, but all writeable
			RelativePopupValidationFeedback vf = new RelativePopupValidationFeedback(
					multiple ? RelativePopupValidationFeedback.BOTTOM
							: RelativePopupValidationFeedback.RIGHT);
			vf.setCss(multiple ? null : "gwittir-ValidationPopup-right");
			return new Field(pr.getPropertyName(), TextProvider.get()
					.getLabelText(c, pr), bwp, getValidator(p.getType(), obj,
					pr.getPropertyName(), vf), vf,getDefaultConverter(p.getType()));
		}
		return null;
	}
	public static Converter getDefaultConverter(Class propertyType){
		if (propertyType==Date.class||propertyType==Timestamp.class){
			return DateToLongStringConverter.INSTANCE;
		}
		return null;
	}
	public Field[] fieldsForReflectedObjectAndSetupWidgetFactory(Object obj,
			BoundWidgetTypeFactory factory, boolean editableWidgets,
			boolean multiple, String propertyName) {
		factory.add(Date.class, DateBox.PROVIDER);
		List<Field> fields = new ArrayList<Field>();
		ClientBeanReflector bi = ClientReflector.get().beanInfoForClass(
				obj.getClass());
		Collection<ClientPropertyReflector> prs = bi.getPropertyReflectors()
				.values();
		Class<? extends Object> c = obj.getClass();
		ObjectPermissions op = bi.getAnnotation(ObjectPermissions.class);
		BeanInfo beanInfo = bi.getAnnotation(BeanInfo.class);
		for (ClientPropertyReflector pr : prs) {
			String pn = pr.getPropertyName();
			if (propertyName != null && !(propertyName.equals(pn))) {
				continue;
			}
			if (ignoreProperties != null && ignoreProperties.contains(pn)) {
				continue;
			}
			Field f = getField(c, pn, editableWidgets, multiple, factory, obj);
			if (f != null) {
				fields.add(f);
			}
		}
		Collections.sort(fields, new FieldDisplayNameComparator(bi));
		return (Field[]) fields.toArray(new Field[fields.size()]);
	}

	// TODO - abstract this, clean above function
	public Validator getValidator(Class clazz, Object obj, String propertyName,
			RelativePopupValidationFeedback vf) {
		ClientBeanReflector bi = ClientReflector.get().beanInfoForClass(
				obj.getClass());
		ClientPropertyReflector pr = bi == null ? null : bi
				.getPropertyReflectors().get(propertyName);
		List<ValidatorInfo> validators = new ArrayList<ValidatorInfo>();
		Validators validatorsAnn = pr == null ? null : pr
				.getAnnotation(Validators.class);
		ValidatorInfo info = pr == null ? null : pr
				.getAnnotation(ValidatorInfo.class);
		if (propertyName == null) {
			validatorsAnn = bi.getAnnotation(Validators.class);
			info = bi.getAnnotation(ValidatorInfo.class);
		}
		if (validatorsAnn != null) {
			validators.addAll(Arrays.asList(validatorsAnn.validators()));
		}
		if (info != null) {
			validators.add(info);
		}
		if (!validators.isEmpty()) {
			CompositeValidator cv = new CompositeValidator();
			for (ValidatorInfo vi : validators) {
				Validator v = CommonLocator.get().classLookup().newInstance(
						vi.validator());
				if (v instanceof ParameterisedValidator) {
					ParameterisedValidator pv = (ParameterisedValidator) v;
					pv.setParameters(vi.parameters());
				}
				if (v instanceof ServerUniquenessValidator
						&& obj instanceof HasId) {
					ServerUniquenessValidator suv = (ServerUniquenessValidator) v;
					suv.setOkId(((HasId) obj).getId());
				}
				NamedParameter msg = NamedParameter.Support.getParameter(vi
						.parameters(), ValidatorInfo.FEEDBACK_MESSAGE);
				if (msg != null) {
					vf.addMessage(vi.validator(), msg.stringValue());
				}
				cv.add(v);
			}
			return cv;
		} else {
			return validatorMap.get(clazz);
		}
	}

	

	public static final BoundWidgetProvider NOWRAP_LABEL_PROVIDER = new BoundWidgetProvider() {
		public BoundWidget get() {
			RenderingLabel label = new RenderingLabel();
			label.setWordWrap(false);
			return label;
		}
	};

	public static final BoundWidgetProvider YES_NO_LABEL_PROVIDER = new BoundWidgetProvider() {
		public BoundWidget get() {
			RenderingLabel label = new RenderingLabel();
			label.setWordWrap(false);
			label.setRenderer(YES_NO_RENDERER);
			return label;
		}
	};



	public static final BoundWidgetProvider AU_DATE_PROVIDER = new BoundWidgetProvider() {
		public BoundWidget get() {
			RenderingLabel label = new RenderingLabel();
			label.setWordWrap(false);
			label.setRenderer(DATE_TIME_RENDERER);
			return label;
		}
	};

	public static final Renderer DATE_TIME_RENDERER = new Renderer() {
		public Object render(Object o) {
			Date d = (Date) o;
			return d == null ? "" : CommonUtils.formatDate(d,
					DateStyle.AU_DATE_TIME);
		}
	};

	

	public static final Renderer DATE_SLASH_RENDERER_WITH_NULL = new Renderer() {
		public Object render(Object o) {
			Date d = (Date) o;
			return d == null ? null : CommonUtils.formatDate(d,
					DateStyle.AU_DATE_SLASH);
		}
	};

	public static final Renderer YES_NO_RENDERER = new Renderer() {
		public Object render(Object o) {
			Boolean b = (Boolean) o;
			return CommonUtils.bv(b) ? "Yes" : "No";
		}
	};

	public static final int MAX_EXPANDABLE_LABEL_LENGTH = 50;

	public static final BoundWidgetProvider DN_LABEL_PROVIDER = new BoundWidgetProvider() {
		public BoundWidget get() {
			RenderingLabel label = new RenderingLabel();
			label.setRenderer(DisplayNameRenderer.INSTANCE);
			return label;
		}
	};

	public static final BoundWidgetProvider FRIENDLY_ENUM_LABEL_PROVIDER_INSTANCE = new FriendlyEnumLabelProvider();

	

	class FieldDisplayNameComparator implements Comparator<Field> {
		private final ClientBeanReflector bi;

		FieldDisplayNameComparator(ClientBeanReflector bi) {
			this.bi = bi;
		}

		public int compare(Field o1, Field o2) {
			return bi.getPropertyReflectors().get(o1.getPropertyName())
					.compareTo(
							bi.getPropertyReflectors()
									.get(o2.getPropertyName()));
		}
	}

	public void setPropertyValue(Object o, String propertyName, Object value) {
		try {
			getProperty(o, propertyName).getMutatorMethod().invoke(o,
					new Object[] { value });
		} catch (Exception e) {
			throw new WrappedRuntimeException(CommonUtils.format(
					"Unable to set property %1 for object %2 to value %3",
					propertyName, o, value), e, SuggestedAction.NOTIFY_WARNING);
		}
	}

	public <A extends Annotation> A getAnnotationForProperty(Class targetClass,
			Class<A> annotationClass, String propertyName) {
		ClientBeanReflector beanInfo = ClientReflector.get().beanInfoForClass(
				targetClass);
		if (beanInfo == null) {
			return null;
		}
		ClientPropertyReflector propertyReflector = beanInfo
				.getPropertyReflectors().get(propertyName);
		return propertyReflector == null ? null : propertyReflector
				.getAnnotation(annotationClass);
	}

	public Object findObjectWithPropertyInCollection(Collection c,
			String propertyName, Object value) {
		for (Object o : c) {
			Object pv = getPropertyValue(o, propertyName);
			if (pv != null && pv.equals(value)) {
				return o;
			}
		}
		return null;
	}

	public void setIgnoreProperties(List<String> ignoreProperties) {
		this.ignoreProperties = ignoreProperties;
	}

	public List<String> getIgnoreProperties() {
		return ignoreProperties;
	}
}
