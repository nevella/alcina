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

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.WrappedRuntimeException.SuggestedAction;
import cc.alcina.framework.common.client.gwittir.validator.BooleanEnsureNonNullCoverter;
import cc.alcina.framework.common.client.gwittir.validator.CompositeValidator;
import cc.alcina.framework.common.client.gwittir.validator.LongValidator;
import cc.alcina.framework.common.client.gwittir.validator.ParameterisedValidator;
import cc.alcina.framework.common.client.gwittir.validator.RequiresSourceValidator;
import cc.alcina.framework.common.client.gwittir.validator.ServerUniquenessValidator;
import cc.alcina.framework.common.client.gwittir.validator.ShortDateValidator;
import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.spi.PropertyAccessor;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.Association;
import cc.alcina.framework.common.client.logic.reflection.Bean;
import cc.alcina.framework.common.client.logic.reflection.ClientBeanReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.ClientPropertyReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientReflector;
import cc.alcina.framework.common.client.logic.reflection.Custom;
import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.PropertyPermissions;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.Validators;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry.RegistryFactory;
import cc.alcina.framework.common.client.provider.TextProvider;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.CommonUtils.DateStyle;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.gwt.client.gwittir.customiser.Customiser;
import cc.alcina.framework.gwt.client.gwittir.provider.ExpandableDomainNodeCollectionLabelProvider;
import cc.alcina.framework.gwt.client.gwittir.provider.FriendlyEnumLabelProvider;
import cc.alcina.framework.gwt.client.gwittir.provider.ListBoxCollectionProvider;
import cc.alcina.framework.gwt.client.gwittir.provider.ListBoxEnumProvider;
import cc.alcina.framework.gwt.client.gwittir.renderer.DisplayNameRenderer;
import cc.alcina.framework.gwt.client.gwittir.widget.DateBox;
import cc.alcina.framework.gwt.client.gwittir.widget.RenderingLabel;
import cc.alcina.framework.gwt.client.gwittir.widget.TextBox;
import cc.alcina.framework.gwt.client.service.BeanDescriptorProvider;
import cc.alcina.framework.gwt.client.widget.RelativePopupValidationFeedback;

import com.totsp.gwittir.client.beans.BeanDescriptor;
import com.totsp.gwittir.client.beans.Binding;
import com.totsp.gwittir.client.beans.Converter;
import com.totsp.gwittir.client.beans.Introspector;
import com.totsp.gwittir.client.beans.Property;
import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;
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
public class GwittirBridge implements PropertyAccessor, BeanDescriptorProvider {
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
		return getDescriptorForClass(c, true);
	}

	@RegistryLocation(registryPoint = BeanDescriptorProvider.class, implementationType = ImplementationType.FACTORY)
	@ClientInstantiable
	public static class GwittirBridgeBdpFactory implements RegistryFactory {
		@Override
		public Object create(Class registryPoint, Class targetObjectClass) {
			return GwittirBridge.get();
		}
	}

	public BeanDescriptor getDescriptorForClass(Class c,
			boolean exceptionIfNotFound) {
		try {
			BeanDescriptor bd = descriptorClassLookup.get(c);
			if (bd == null) {
				Object o = ClientReflector.get().getTemplateInstance(c);
				bd = Introspector.INSTANCE.getDescriptor(o);
				descriptorClassLookup.put(c, bd);
			}
			return bd;
		} catch (RuntimeException re) {
			if (exceptionIfNotFound) {
				throw re;
			} else {
				return null;
			}
		}
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
			throw new WrappedRuntimeException(
					CommonUtils.formatJ(
							"Unable to get property %s for object %s",
							propertyName, o.getClass().getName()),
					e, SuggestedAction.NOTIFY_WARNING);
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

	public static final BoundWidgetProvider<TextBox> TEXTBOX_PROVIDER = new BoundWidgetProvider<TextBox>() {
		public TextBox get() {
			return new TextBox();
		}
	};

	public static class BoundWidgetTypeFactorySimpleGenerator
			extends BoundWidgetTypeFactory {
		public BoundWidgetTypeFactorySimpleGenerator() {
			super(true);
			add(Date.class, DateBox.PROVIDER);
			add(String.class, GwittirBridge.TEXTBOX_PROVIDER);
			add(Integer.class, GwittirBridge.TEXTBOX_PROVIDER);
			add(int.class, GwittirBridge.TEXTBOX_PROVIDER);
			add(Long.class, GwittirBridge.TEXTBOX_PROVIDER);
			add(long.class, GwittirBridge.TEXTBOX_PROVIDER);
			add(Float.class, GwittirBridge.TEXTBOX_PROVIDER);
			add(float.class, GwittirBridge.TEXTBOX_PROVIDER);
			add(Double.class, GwittirBridge.TEXTBOX_PROVIDER);
			add(double.class, GwittirBridge.TEXTBOX_PROVIDER);
		}

		public BoundWidgetProvider getWidgetProvider(Class type) {
			if (type.isEnum()) {
				return new ListBoxEnumProvider(type, true);
			}
			return super.getWidgetProvider(type);
		}
	}

	public static BoundWidgetTypeFactorySimpleGenerator SIMPLE_FACTORY = new BoundWidgetTypeFactorySimpleGenerator();

	public List<Field> fieldsForReflectedObjectAndSetupWidgetFactoryAsList(
			Object obj, BoundWidgetTypeFactory factory, boolean editableWidgets,
			boolean multiple) {
		return new ArrayList<Field>(
				Arrays.asList(fieldsForReflectedObjectAndSetupWidgetFactory(obj,
						factory, editableWidgets, multiple)));
	}

	public Field[] fieldsForReflectedObjectAndSetupWidgetFactory(Object obj,
			BoundWidgetTypeFactory factory, boolean editableWidgets,
			boolean multiple) {
		return fieldsForReflectedObjectAndSetupWidgetFactory(obj, factory,
				editableWidgets, multiple, null);
	}

	private List<String> ignoreProperties;

	public Field getField(Class c, String propertyName, boolean editableWidgets,
			boolean multiple) {
		return getField(c, propertyName, editableWidgets, multiple,
				SIMPLE_FACTORY, null);
	}

	public boolean isFieldEditable(Class c, String propertyName) {
		Object obj = ClientReflector.get().getTemplateInstance(c);
		ClientBeanReflector bi = ClientReflector.get().beanInfoForClass(c);
		Collection<ClientPropertyReflector> prs = bi.getPropertyReflectors()
				.values();
		Bean beanInfo = bi.getAnnotation(Bean.class);
		ObjectPermissions op = bi.getAnnotation(ObjectPermissions.class);
		ClientPropertyReflector pr = bi.getPropertyReflectors()
				.get(propertyName);
		Property p = getProperty(obj, pr.getPropertyName());
		if (pr != null && pr.getDisplayInfo() != null) {
			PropertyPermissions pp = pr
					.getAnnotation(PropertyPermissions.class);
			Display displayInfo = pr.getDisplayInfo();
			boolean fieldVisible = PermissionsManager.get()
					.checkEffectivePropertyPermission(op, pp, obj, true)
					&& displayInfo != null
					&& PermissionsManager.get().isPermissible(obj,
							displayInfo.visible())
					&& ((displayInfo.displayMask()
							& Display.DISPLAY_AS_PROPERTY) != 0);
			if (!fieldVisible) {
				return false;
			}
			boolean propertyIsCollection = (p.getType() == Set.class);
			return PermissionsManager.get().checkEffectivePropertyPermission(op,
					pp, obj, false)
					&& ((displayInfo.displayMask() & Display.DISPLAY_RO) == 0);
		}
		return false;
	}

	public Field getField(Class c, String propertyName, boolean editableWidgets,
			boolean multiple, BoundWidgetTypeFactory factory, Object obj) {
		ClientBeanReflector bi = ClientReflector.get().beanInfoForClass(c);
		Collection<ClientPropertyReflector> prs = bi.getPropertyReflectors()
				.values();
		Bean beanInfo = bi.getAnnotation(Bean.class);
		ObjectPermissions op = bi.getAnnotation(ObjectPermissions.class);
		obj = obj != null ? obj : ClientReflector.get().getTemplateInstance(c);
		ClientPropertyReflector pr = bi.getPropertyReflectors()
				.get(propertyName);
		Property p = getProperty(obj, pr.getPropertyName());
		BoundWidgetProvider bwp = factory.getWidgetProvider(p.getType());
		int position = multiple ? RelativePopupValidationFeedback.BOTTOM
				: RelativePopupValidationFeedback.RIGHT;
		int contextPosition = LooseContext.getContext().getInteger(
				RelativePopupValidationFeedback.CONTEXT_FEEDBACK_POSITION, -1);
		if (contextPosition != -1) {
			position = contextPosition;
		}
		if (pr != null && pr.getDisplayInfo() != null) {
			PropertyPermissions pp = pr
					.getAnnotation(PropertyPermissions.class);
			Display displayInfo = pr.getDisplayInfo();
			Association association = pr.getAnnotation(Association.class);
			boolean fieldVisible = PermissionsManager.get()
					.checkEffectivePropertyPermission(op, pp, obj, true)
					&& displayInfo != null
					&& PermissionsManager.get().isPermissible(obj,
							displayInfo.visible())
					&& ((displayInfo.displayMask()
							& Display.DISPLAY_AS_PROPERTY) != 0);
			if (!fieldVisible) {
				return null;
			}
			boolean focus = displayInfo.focus();
			boolean propertyIsCollection = (p.getType() == Set.class);
			boolean fieldEditable = editableWidgets && PermissionsManager.get()
					.checkEffectivePropertyPermission(op, pp, obj, false)
					&& ((displayInfo.displayMask() & Display.DISPLAY_RO) == 0);
			;
			Class domainType = p.getType();
			domainType = (association == null || !propertyIsCollection
					|| association.implementationClass() == void.class)
							? domainType : association.implementationClass();
			boolean isDomainClass = GwittirBridge.get()
					.hasDescriptor(domainType);
			if (bwp == null && isDomainClass) {
				bwp = new ListBoxCollectionProvider(domainType,
						propertyIsCollection);
			}
			boolean isEnum = domainType.isEnum();
			boolean displayWrap = (displayInfo.displayMask()
					& Display.DISPLAY_WRAP) > 0;
			if (bwp == null && isEnum) {
				bwp = fieldEditable ? new ListBoxEnumProvider(domainType, true)
						: NOWRAP_LABEL_PROVIDER;
			}
			if (bwp != null && !fieldEditable) {
				if (isDomainClass) {
					bwp = propertyIsCollection
							? new ExpandableDomainNodeCollectionLabelProvider(
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
						bwp = displayWrap ? WRAP_LABEL_PROVIDER
								: NOWRAP_LABEL_PROVIDER;
					}
				}
			}
			Custom customiserInfo = pr.getAnnotation(Custom.class);
			if (customiserInfo != null) {
				Customiser customiser = (Customiser) ClientReflector.get()
						.newInstance(customiserInfo.customiserClass(), 0, 0);
				bwp = customiser.getProvider(fieldEditable, domainType,
						multiple, customiserInfo);
			}
			if (bwp != null) {
				RelativePopupValidationFeedback vf = null;
				Validator validator = null;
				if (fieldEditable) {
					vf = new RelativePopupValidationFeedback(position);
					vf.setCss(
							multiple ? null : "gwittir-ValidationPopup-right");
					validator = getValidator(domainType, obj,
							pr.getPropertyName(), vf);
				}
				Field field = new Field(pr.getPropertyName(),
						TextProvider.get().getLabelText(c, pr), bwp, validator,
						vf, getDefaultConverter(bwp, p.getType()));
				if (!displayInfo.styleName().isEmpty()) {
					field.setStyleName(displayInfo.styleName());
				}
				if (!displayInfo.helpText().isEmpty()) {
					field.setHelpText(displayInfo.helpText());
				}
				return field;
			}
		} else if (beanInfo.allPropertiesVisualisable()
				&& PermissionsManager.get().checkEffectivePropertyPermission(op,
						null, obj, !editableWidgets)) {
			// no property info, but all writeable (if object is set)
			RelativePopupValidationFeedback vf = new RelativePopupValidationFeedback(
					position);
			vf.setCss(multiple ? null : "gwittir-ValidationPopup-right");
			if (bwp == null) {
				Class domainType = p.getType();
				boolean isEnum = domainType.isEnum();
				if (editableWidgets) {
					if (domainType == Date.class) {
						bwp = AU_DATE_PROVIDER;
					} else if (isEnum) {
						bwp = editableWidgets
								? new ListBoxEnumProvider(domainType, true)
								: NOWRAP_LABEL_PROVIDER;
					} else if (domainType == boolean.class
							|| domainType == Boolean.class) {
						bwp = YES_NO_LABEL_PROVIDER;
					} else {
						bwp = NOWRAP_LABEL_PROVIDER;
					}
				}
			}
			return new Field(pr.getPropertyName(),
					TextProvider.get().getLabelText(c, pr), bwp,
					getValidator(p.getType(), obj, pr.getPropertyName(), vf),
					vf, getDefaultConverter(bwp, p.getType()));
		}
		return null;
	}

	public static Converter getDefaultConverter(BoundWidgetProvider bwp,
			Class propertyType) {
		if (propertyType == Boolean.class) {
			return BooleanEnsureNonNullCoverter.INSTANCE;
		}
		if (propertyType == Double.class) {
			return Converter.DOUBLE_TO_STRING_CONVERTER;
		}
		if (bwp == BoundWidgetTypeFactory.TEXTBOX_PROVIDER
				|| bwp == TEXTBOX_PROVIDER) {
			return Converter.TO_STRING_CONVERTER;
			/*
			 * these seem to be being introspected as value-type object, not
			 * string - at least w JVMIntrospector
			 */
		}
		return null;
	}

	public Field[] fieldsForReflectedObjectAndSetupWidgetFactory(Object obj,
			BoundWidgetTypeFactory factory, boolean editableWidgets,
			boolean multiple, String propertyName) {
		factory.add(Date.class, DateBox.PROVIDER);
		List<Field> fields = new ArrayList<Field>();
		ClientBeanReflector bi = ClientReflector.get()
				.beanInfoForClass(obj.getClass());
		Collection<ClientPropertyReflector> prs = bi.getPropertyReflectors()
				.values();
		Class<? extends Object> c = obj.getClass();
		ObjectPermissions op = bi.getAnnotation(ObjectPermissions.class);
		Bean beanInfo = bi.getAnnotation(Bean.class);
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
		ClientBeanReflector bi = ClientReflector.get()
				.beanInfoForClass(obj.getClass());
		ClientPropertyReflector pr = bi == null ? null
				: bi.getPropertyReflectors().get(propertyName);
		List<cc.alcina.framework.common.client.logic.reflection.Validator> validators = new ArrayList<>();
		cc.alcina.framework.common.client.logic.reflection.Validators validatorsAnn = pr == null
				? null : pr.getAnnotation(Validators.class);
		cc.alcina.framework.common.client.logic.reflection.Validator info = pr == null
				? null
				: pr.getAnnotation(
						cc.alcina.framework.common.client.logic.reflection.Validator.class);
		if (propertyName == null) {
			validatorsAnn = bi.getAnnotation(Validators.class);
			info = bi.getAnnotation(
					cc.alcina.framework.common.client.logic.reflection.Validator.class);
		}
		if (validatorsAnn != null) {
			validators.addAll(Arrays.asList(validatorsAnn.validators()));
		}
		if (info != null) {
			validators.add(info);
		}
		if (!validators.isEmpty()) {
			CompositeValidator cv = new CompositeValidator();
			for (cc.alcina.framework.common.client.logic.reflection.Validator validatorAnnotation : validators) {
				Validator v = Reflections.classLookup()
						.newInstance(validatorAnnotation.validator());
				if (v instanceof ParameterisedValidator) {
					ParameterisedValidator pv = (ParameterisedValidator) v;
					pv.setParameters(validatorAnnotation.parameters());
				}
				if (v instanceof ServerUniquenessValidator
						&& obj instanceof HasId) {
					ServerUniquenessValidator suv = (ServerUniquenessValidator) v;
					suv.setOkId(((HasId) obj).getId());
				}
				if (v instanceof RequiresSourceValidator
						&& obj instanceof HasIdAndLocalId) {
					RequiresSourceValidator rsv = (RequiresSourceValidator) v;
					rsv.setSourceObject((HasIdAndLocalId) obj);
				}
				NamedParameter msg = NamedParameter.Support.getParameter(
						validatorAnnotation.parameters(),
						cc.alcina.framework.common.client.logic.reflection.Validator.FEEDBACK_MESSAGE);
				if (msg != null) {
					vf.addMessage(validatorAnnotation.validator(),
							msg.stringValue());
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

	public static final BoundWidgetProvider WRAP_LABEL_PROVIDER = new BoundWidgetProvider() {
		public BoundWidget get() {
			RenderingLabel label = new RenderingLabel();
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
			return d == null ? ""
					: CommonUtils.formatDate(d, DateStyle.AU_DATE_TIME);
		}
	};

	public static final Renderer DATE_SLASH_RENDERER_WITH_NULL = new Renderer() {
		public Object render(Object o) {
			Date d = (Date) o;
			return d == null ? null
					: CommonUtils.formatDate(d, DateStyle.AU_DATE_SLASH);
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
					.compareTo(bi.getPropertyReflectors()
							.get(o2.getPropertyName()));
		}
	}

	public void setPropertyValue(Object o, String propertyName, Object value) {
		try {
			getProperty(o, propertyName).getMutatorMethod().invoke(o,
					new Object[] { value });
		} catch (Exception e) {
			try {
				throw new WrappedRuntimeException(
						CommonUtils.formatJ(
								"Unable to set property %s for object %s to value %s",
								propertyName, o, value),
						e, SuggestedAction.NOTIFY_WARNING);
			} catch (Exception e1) {
				// tostring problem
				throw new WrappedRuntimeException(
						CommonUtils.formatJ(
								"Unable to set property %s for object %s to value %s",
								propertyName, o.getClass(),
								value == null ? null : value.getClass()),
						e, SuggestedAction.NOTIFY_WARNING);
			}
		}
	}

	public <A extends Annotation> A getAnnotationForProperty(Class targetClass,
			Class<A> annotationClass, String propertyName) {
		ClientBeanReflector beanInfo = ClientReflector.get()
				.beanInfoForClass(targetClass);
		if (beanInfo == null) {
			return null;
		}
		ClientPropertyReflector propertyReflector = beanInfo
				.getPropertyReflectors().get(propertyName);
		return propertyReflector == null ? null
				: propertyReflector.getAnnotation(annotationClass);
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

	public Class getPropertyType(Class objectClass, String propertyName) {
		return ClientReflector.get().getPropertyType(objectClass, propertyName);
	}

	public Field getFieldToFocus(Object bean, Field[] fields) {
		ClientBeanReflector bi = ClientReflector.get()
				.beanInfoForClass(bean.getClass());
		Map<String, ClientPropertyReflector> prs = bi.getPropertyReflectors();
		for (Field field : fields) {
			ClientPropertyReflector pr = prs.get(field.getPropertyName());
			if (pr != null && pr.getDisplayInfo() != null
					&& pr.getDisplayInfo().focus()) {
				return field;
			}
		}
		return null;
	}

	public static BoundWidget createWidget(Binding parent, Field field,
			SourcesPropertyChangeEvents target, Object model) {
		final BoundWidget widget;
		Binding binding;
		if (field.getCellProvider() != null) {
			widget = field.getCellProvider().get();
		} else {
			final Property p = Introspector.INSTANCE.getDescriptor(target)
					.getProperty(field.getPropertyName());
			widget = SIMPLE_FACTORY
					.getWidgetProvider(field.getPropertyName(), p.getType())
					.get();
		}
		binding = new Binding(widget, "value", field.getValidator(),
				field.getFeedback(), target, field.getPropertyName(), null,
				null);
		widget.setModel(model);
		if (field.getConverter() != null) {
			binding.getRight().converter = field.getConverter();
		}
		Converter inverseConverter = getInverseConverter(field.getConverter());
		if (inverseConverter != null) {
			binding.getLeft().converter = inverseConverter;
		}
		if (field.getComparator() != null) {
			widget.setComparator(field.getComparator());
		}
		parent.getChildren().add(binding);
		return widget;
	}

	public static Converter getInverseConverter(Converter c) {
		if (c == null) {
			return null;
		}
		if (c == Converter.DOUBLE_TO_STRING_CONVERTER) {
			return Converter.STRING_TO_DOUBLE_CONVERTER;
		}
		return null;
	}

	@Override
	public IndividualPropertyAccessor cachedAccessor(Class clazz,
			String propertyName) {
		Property property = getPropertyForClass(clazz, propertyName);
		return new IndividualPropertyAccessor() {
			@Override
			public void setPropertyValue(Object bean, Object value) {
				try {
					property.getMutatorMethod().invoke(bean,
							new Object[] { value });
				} catch (Exception e) {
					throw new WrappedRuntimeException(e);
				}
			}

			@Override
			public Object getPropertyValue(Object value) {
				try {
					return property.getAccessorMethod().invoke(value,
							new Object[0]);
				} catch (Exception e) {
					throw new WrappedRuntimeException(e);
				}
			}
		};
	}
}
