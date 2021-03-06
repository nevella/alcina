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
import java.util.function.Function;
import java.util.function.Predicate;

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
import com.totsp.gwittir.client.validator.AbstractValidationFeedback;
import com.totsp.gwittir.client.validator.DoubleValidator;
import com.totsp.gwittir.client.validator.IntegerValidator;
import com.totsp.gwittir.client.validator.ValidationFeedback;
import com.totsp.gwittir.client.validator.Validator;

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
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.domaintransform.spi.PropertyAccessor;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.AnnotationLocation;
import cc.alcina.framework.common.client.logic.reflection.AnnotationLocation.DefaultResolver;
import cc.alcina.framework.common.client.logic.reflection.AnnotationLocation.Resolver;
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
import cc.alcina.framework.common.client.logic.reflection.PropertyReflector;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.Validators;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry.RegistryFactory;
import cc.alcina.framework.common.client.provider.TextProvider;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.CommonUtils.DateStyle;
import cc.alcina.framework.gwt.client.dirndl.RenderContext;
import cc.alcina.framework.gwt.client.gwittir.customiser.Customiser;
import cc.alcina.framework.gwt.client.gwittir.customiser.ModelPlaceValueCustomiser;
import cc.alcina.framework.gwt.client.gwittir.provider.ExpandableDomainNodeCollectionLabelProvider;
import cc.alcina.framework.gwt.client.gwittir.provider.FriendlyEnumLabelProvider;
import cc.alcina.framework.gwt.client.gwittir.provider.ListBoxCollectionProvider;
import cc.alcina.framework.gwt.client.gwittir.provider.ListBoxEnumProvider;
import cc.alcina.framework.gwt.client.gwittir.renderer.DisplayNameRenderer;
import cc.alcina.framework.gwt.client.gwittir.widget.DateBox.DateBoxProvider;
import cc.alcina.framework.gwt.client.gwittir.widget.RenderingLabel;
import cc.alcina.framework.gwt.client.gwittir.widget.TextBox;
import cc.alcina.framework.gwt.client.service.BeanDescriptorProvider;
import cc.alcina.framework.gwt.client.widget.RelativePopupValidationFeedback;

/**
 *
 * @author Nick Reddel
 */
public class GwittirBridge implements PropertyAccessor, BeanDescriptorProvider {
	private static GwittirBridge INSTANCE = new GwittirBridge();

	public static final String HINT_DATE_WITH_TIME_TITLE = "HINT_DATE_WITH_TIME_TITLE";

	public static BoundWidgetTypeFactorySimpleGenerator SIMPLE_FACTORY = new BoundWidgetTypeFactorySimpleGenerator();

	public static BoundWidgetTypeFactorySimpleGenerator SIMPLE_FACTORY_NO_NULLS = new BoundWidgetTypeFactorySimpleGenerator(
			false);

	public static final BoundWidgetProvider NOWRAP_LABEL_PROVIDER = new BoundWidgetProvider() {
		@Override
		public BoundWidget get() {
			RenderingLabel label = new RenderingLabel();
			label.setWordWrap(false);
			return label;
		}
	};

	public static final BoundWidgetProvider WRAP_LABEL_PROVIDER = new BoundWidgetProvider() {
		@Override
		public BoundWidget get() {
			RenderingLabel label = new RenderingLabel();
			return label;
		}
	};

	public static final BoundWidgetProvider YES_NO_LABEL_PROVIDER = new BoundWidgetProvider() {
		@Override
		public BoundWidget get() {
			RenderingLabel label = new RenderingLabel();
			label.setWordWrap(false);
			label.setRenderer(YES_NO_RENDERER);
			return label;
		}
	};

	public static final BoundWidgetProvider AU_DATE_PROVIDER = new BoundWidgetProvider() {
		@Override
		public BoundWidget get() {
			RenderingLabel label = new RenderingLabel();
			label.setWordWrap(false);
			label.setRenderer(DATE_TIME_RENDERER);
			return label;
		}
	};

	public static final BoundWidgetProvider AU_DATE_TIME_TITLE_PROVIDER = new BoundWidgetProvider() {
		@Override
		public BoundWidget get() {
			RenderingLabel label = new RenderingLabel();
			label.setWordWrap(false);
			label.setRenderer(DATE_SLASH_RENDERER_WITH_NULL);
			label.setTitleRenderer(DATE_TIME_RENDERER);
			return label;
		}
	};

	public static final Renderer DATE_TIME_RENDERER = new Renderer() {
		@Override
		public Object render(Object o) {
			Date d = (Date) o;
			return d == null ? ""
					: CommonUtils.formatDate(d, DateStyle.AU_DATE_TIME);
		}
	};

	public static final Renderer DATE_SLASH_RENDERER_WITH_NULL = new Renderer() {
		@Override
		public Object render(Object o) {
			Date d = (Date) o;
			return d == null ? null
					: CommonUtils.formatDate(d, DateStyle.AU_DATE_SLASH);
		}
	};

	public static final Renderer YES_NO_RENDERER = new Renderer() {
		@Override
		public Object render(Object o) {
			Boolean b = (Boolean) o;
			return CommonUtils.bv(b) ? "Yes" : "No";
		}
	};

	public static final int MAX_EXPANDABLE_LABEL_LENGTH = 50;

	public static final BoundWidgetProvider DN_LABEL_PROVIDER = new BoundWidgetProvider() {
		@Override
		public BoundWidget get() {
			RenderingLabel label = new RenderingLabel();
			label.setRenderer(DisplayNameRenderer.INSTANCE);
			return label;
		}
	};

	public static final BoundWidgetProvider FRIENDLY_ENUM_LABEL_PROVIDER_INSTANCE = new FriendlyEnumLabelProvider();

	public static BoundWidget createWidget(Binding parent, Field field,
			SourcesPropertyChangeEvents target, Object model) {
		final BoundWidget widget;
		Binding binding;
		if (field.getCellProvider() != null) {
			widget = field.getCellProvider().get();
		} else {
			final Property p = Introspector.INSTANCE.getDescriptor(target)
					.getProperty(field.getPropertyName());
			widget = SIMPLE_FACTORY.getWidgetProvider(p.getType()).get();
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

	// will be compiled to a field ref
	public static GwittirBridge get() {
		return INSTANCE;
	};

	public static Converter getDefaultConverter(BoundWidgetProvider bwp,
			Class propertyType) {
		if (propertyType == Boolean.class) {
			return BooleanEnsureNonNullCoverter.INSTANCE;
		}
		if (propertyType == Double.class) {
			return Converter.DOUBLE_TO_STRING_CONVERTER;
		}
		if (bwp == BoundWidgetTypeFactory.TEXTBOX_PROVIDER
				|| bwp.getClass() == BoundWidgetProviderTextBox.class) {
			return Converter.TO_STRING_CONVERTER;
			/*
			 * these seem to be being introspected as value-type object, not
			 * string - at least w JVMIntrospector
			 */
		}
		return null;
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

	private Map<Class, Validator> validatorMap = new HashMap<Class, Validator>();
	{
		validatorMap.put(Integer.class, IntegerValidator.INSTANCE);
		validatorMap.put(int.class, IntegerValidator.INSTANCE);
		validatorMap.put(Long.class, LongValidator.INSTANCE);
		validatorMap.put(long.class, LongValidator.INSTANCE);
		validatorMap.put(Double.class, DoubleValidator.INSTANCE);
		validatorMap.put(double.class, DoubleValidator.INSTANCE);
		validatorMap.put(Date.class, ShortDateValidator.INSTANCE);
	}

	private Map<Class, BeanDescriptor> descriptorClassLookup = new HashMap<Class, BeanDescriptor>();

	private List<String> ignoreProperties;

	private GwittirDateRendererProvider dateRendererProvider = new GwittirDateRendererProvider();

	private GwittirBridge() {
		super();
	}

	public Field[] fieldsForReflectedObjectAndSetupWidgetFactory(Object obj,
			BoundWidgetTypeFactory factory, boolean editableWidgets,
			boolean multiple) {
		return fieldsForReflectedObjectAndSetupWidgetFactory(obj, factory,
				editableWidgets, multiple, null, null, new DefaultResolver());
	}

	public Field[] fieldsForReflectedObjectAndSetupWidgetFactory(Object obj,
			BoundWidgetTypeFactory factory, boolean editableWidgets,
			boolean multiple, String propertyName,
			Predicate<String> editableFieldNameFilter, Resolver resolver) {
		factory.add(Date.class, new DateBoxProvider());
		List<Field> fields = new ArrayList<Field>();
		ClientBeanReflector bi = ClientReflector.get()
				.beanInfoForClass(obj.getClass());
		Collection<ClientPropertyReflector> prs = bi.getPropertyReflectors()
				.values();
		Class<? extends Object> c = obj.getClass();
		Bean beanInfo = bi.getAnnotation(Bean.class);
		for (ClientPropertyReflector pr : prs) {
			String pn = pr.getPropertyName();
			if (propertyName != null && !(propertyName.equals(pn))) {
				continue;
			}
			if (ignoreProperties != null && ignoreProperties.contains(pn)) {
				continue;
			}
			boolean editableField = editableWidgets;
			if (editableFieldNameFilter != null
					&& !editableFieldNameFilter.test(pn)) {
				editableField = false;
			}
			Field f = getField(c, pn, editableField, multiple, factory, obj,
					resolver);
			if (f != null) {
				fields.add(f);
			}
		}
		Collections.sort(fields, new FieldDisplayNameComparator(bi));
		return (Field[]) fields.toArray(new Field[fields.size()]);
	}

	/*
	 * FIXME - dirndl.1 - in general, pass around clazz instead of obj
	 */
	public List<Field> fieldsForReflectedObjectAndSetupWidgetFactoryAsList(
			Object obj, BoundWidgetTypeFactory factory, boolean editableWidgets,
			boolean multiple, AnnotationLocation.Resolver resolver) {
		return new ArrayList<Field>(Arrays.asList(
				fieldsForReflectedObjectAndSetupWidgetFactory(obj, factory,
						editableWidgets, multiple, null, null, resolver)));
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

	@Override
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

	public GwittirDateRendererProvider getDateRendererProvider() {
		return this.dateRendererProvider;
	}

	@Override
	public BeanDescriptor getDescriptor(Object o) {
		Class c = o.getClass();
		BeanDescriptor bd = descriptorClassLookup.get(c);
		if (bd == null) {
			bd = Introspector.INSTANCE.getDescriptor(o);
			descriptorClassLookup.put(c, bd);
		}
		return bd;
	}

	public BeanDescriptor getDescriptorForClass(Class c) {
		return getDescriptorForClass(c, true);
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

	@Override
	public BeanDescriptor getDescriptorOrNull(Object o) {
		Class c = o.getClass();
		BeanDescriptor bd = descriptorClassLookup.get(c);
		if (bd == null) {
			bd = Introspector.INSTANCE.getDescriptorOrNull(o);
			descriptorClassLookup.put(c, bd);
		}
		return bd;
	}

	public Field getField(Class c, String propertyName, boolean editableWidgets,
			boolean multiple) {
		return getField(c, propertyName, editableWidgets, multiple,
				SIMPLE_FACTORY, null);
	}

	public Field getField(Class clazz, String propertyName,
			boolean editableWidgets, boolean multiple,
			BoundWidgetTypeFactory factory, Object obj) {
		return getField(clazz, propertyName, editableWidgets, multiple, factory,
				obj, new AnnotationLocation.DefaultResolver());
	}

	// FIXME - dirndl.1 - clean this up - probably one code path and a bunch of
	// reflection/registry
	public Field getField(Class clazz, String propertyName,
			boolean editableWidgets, boolean multiple,
			BoundWidgetTypeFactory factory, Object obj,
			AnnotationLocation.Resolver resolver) {
		List<PropertyReflector> propertyReflectors = Reflections.classLookup()
				.getPropertyReflectors(clazz);
		AnnotationLocation clazzLocation = new AnnotationLocation(clazz, null,
				resolver);
		Bean beanInfo = clazzLocation.getAnnotation(Bean.class);
		ObjectPermissions op = clazzLocation
				.getAnnotation(ObjectPermissions.class);
		obj = obj != null ? obj
				: ClientReflector.get().getTemplateInstance(clazz);
		PropertyReflector propertyReflector = Reflections.classLookup()
				.getPropertyReflector(clazz, propertyName);
		AnnotationLocation propertyLocation = new AnnotationLocation(clazz,
				propertyReflector, resolver);
		Property p = getProperty(obj, propertyName);
		BoundWidgetProvider bwp = factory.getWidgetProvider(p.getType());
		int position = multiple ? RelativePopupValidationFeedback.BOTTOM
				: RelativePopupValidationFeedback.RIGHT;
		Display display = propertyLocation.getAnnotation(Display.class);
		if (display != null) {
			PropertyPermissions pp = propertyLocation
					.getAnnotation(PropertyPermissions.class);
			Association association = propertyLocation
					.getAnnotation(Association.class);
			boolean fieldVisible = PermissionsManager.get()
					.checkEffectivePropertyPermission(op, pp, obj, true)
					&& display != null
					&& PermissionsManager.get().isPermitted(obj,
							display.visible())
					&& ((display.displayMask()
							& Display.DISPLAY_AS_PROPERTY) != 0);
			if (!fieldVisible) {
				return null;
			}
			boolean focus = display.focus();
			boolean propertyIsCollection = (p.getType() == Set.class);
			boolean fieldEditable = editableWidgets
					&& (PermissionsManager.get()
							.checkEffectivePropertyPermission(op, pp, obj,
									false)
							|| ((display.displayMask()
									& Display.DISPLAY_EDITABLE) != 0))
					&& ((display.displayMask() & Display.DISPLAY_RO) == 0);
			Class domainType = p.getType();
			domainType = (association == null || !propertyIsCollection
					|| association.implementationClass() == void.class)
							? domainType
							: association.implementationClass();
			boolean isDomainClass = GwittirBridge.get()
					.hasDescriptor(domainType);
			if (bwp == null && isDomainClass) {
				if (fieldEditable) {
					bwp = Registry.impl(DomainListProvider.class)
							.getProvider(domainType, propertyIsCollection);
				} else {
					if (propertyIsCollection) {
						bwp = new ExpandableDomainNodeCollectionLabelProvider(
								MAX_EXPANDABLE_LABEL_LENGTH, true);
					} else {
						if (multiple) {
							bwp = DN_LABEL_PROVIDER;
						} else {
							bwp = new ModelPlaceValueCustomiser();
						}
					}
				}
			}
			boolean isEnum = domainType.isEnum();
			boolean displayWrap = (display.displayMask()
					& Display.DISPLAY_WRAP) > 0;
			if (bwp == null && isEnum) {
				bwp = fieldEditable ? new ListBoxEnumProvider(domainType, true)
						: NOWRAP_LABEL_PROVIDER;
			}
			if (bwp != null && !fieldEditable && !(bwp instanceof Customiser)) {
				if (isDomainClass) {
					bwp = propertyIsCollection
							? new ExpandableDomainNodeCollectionLabelProvider(
									MAX_EXPANDABLE_LABEL_LENGTH, true)
							: DN_LABEL_PROVIDER;
				} else {
					if (domainType == Date.class) {
						bwp = dateRendererProvider.getRenderer(display);
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
			Custom customiserInfo = propertyLocation
					.getAnnotation(Custom.class);
			if (customiserInfo != null) {
				Customiser customiser = Reflections
						.newInstance(customiserInfo.customiserClass());
				bwp = customiser.getProvider(fieldEditable, domainType,
						multiple, customiserInfo, propertyLocation);
			}
			if (bwp != null) {
				ValidationFeedback validationFeedback = null;
				Validator validator = null;
				if (fieldEditable) {
					Function<String, ValidationFeedback> validationFeedbackSupplier = RenderContext
							.get().getValidationFeedbackSupplier();
					if (validationFeedbackSupplier == null) {
						validationFeedback = new RelativePopupValidationFeedback(
								position);
						((RelativePopupValidationFeedback) validationFeedback)
								.setCss(multiple ? null
										: "gwittir-ValidationPopup-right");
					} else {
						validationFeedback = validationFeedbackSupplier
								.apply(propertyReflector.getPropertyName());
					}
					validator = getValidator(domainType, obj,
							propertyReflector.getPropertyName(),
							validationFeedback);
				}
				Field field = new Field(propertyReflector.getPropertyName(),
						// FIXME - dirndl.2
						TextProvider.get().getLabelText(clazz,
								propertyLocation),
						bwp, validator, validationFeedback,
						getDefaultConverter(bwp, p.getType()));
				if (!display.styleName().isEmpty()) {
					field.setStyleName(display.styleName());
				}
				if (!display.widgetStyleName().isEmpty()) {
					field.setWidgetStyleName(display.widgetStyleName());
				}
				if (!display.helpText().isEmpty()) {
					field.setHelpText(display.helpText().replace("\\n", "\n"));
				}
				if (!display.autocompleteName().isEmpty()) {
					field.setAutocompleteName(display.autocompleteName());
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
			if (bwp != null && !editableWidgets) {
				Class domainType = p.getType();
				boolean isEnum = domainType.isEnum();
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
			} else if (bwp == null) {
				Class domainType = p.getType();
				boolean isEnum = domainType.isEnum();
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
			return new Field(propertyReflector.getPropertyName(),
					TextProvider.get().getLabelText(clazz, propertyLocation),
					bwp,
					getValidator(p.getType(), obj,
							propertyReflector.getPropertyName(), vf),
					vf, getDefaultConverter(bwp, p.getType()));
		}
		return null;
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

	public List<String> getIgnoreProperties() {
		return ignoreProperties;
	}

	public Property getProperty(Object o, String propertyName) {
		return getDescriptor(o).getProperty(propertyName);
	}

	public Property getPropertyForClass(Class c, String propertyName) {
		return getDescriptorForClass(c).getProperty(propertyName);
	}

	@Override
	public PropertyReflector getPropertyReflector(Class clazz,
			String propertyName) {
		Property property = getPropertyForClass(clazz, propertyName);
		return new PropertyReflector() {
			@Override
			public <A extends Annotation> A
					getAnnotation(Class<A> annotationClass) {
				throw new UnsupportedOperationException();
			}

			@Override
			public Class getDefiningType() {
				return clazz;
			}

			@Override
			public String getPropertyName() {
				return property.getName();
			}

			@Override
			public Class getPropertyType() {
				try {
					return property.getType();
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

			@Override
			public boolean isReadOnly() {
				return property.getMutatorMethod() == null;
			}

			@Override
			public void setPropertyValue(Object bean, Object value) {
				try {
					property.getMutatorMethod().invoke(bean,
							new Object[] { value });
				} catch (Exception e) {
					throw new WrappedRuntimeException(e);
				}
			}
		};
	}

	@Override
	public Class getPropertyType(Class objectClass, String propertyName) {
		return ClientReflector.get().getPropertyType(objectClass, propertyName);
	}

	@Override
	public Object getPropertyValue(Object o, String propertyName) {
		try {
			BeanDescriptor bd = getDescriptor(o);
			return bd.getProperty(propertyName).getAccessorMethod().invoke(o,
					null);
		} catch (Exception e) {
			throw new WrappedRuntimeException(
					Ax.format("Unable to get property %s for object %s",
							propertyName, o.getClass().getName()),
					e, SuggestedAction.NOTIFY_WARNING);
		}
	}

	public Validator getValidator(Class clazz, Object obj, String propertyName,
			ValidationFeedback validationFeedback) {
		ClientBeanReflector bi = ClientReflector.get()
				.beanInfoForClass(obj.getClass());
		ClientPropertyReflector pr = bi == null ? null
				: bi.getPropertyReflectors().get(propertyName);
		List<cc.alcina.framework.common.client.logic.reflection.Validator> validators = new ArrayList<>();
		cc.alcina.framework.common.client.logic.reflection.Validators validatorsAnn = pr == null
				? null
				: pr.getAnnotation(Validators.class);
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
						&& obj instanceof Entity) {
					RequiresSourceValidator rsv = (RequiresSourceValidator) v;
					rsv.setSourceObject((Entity) obj);
				}
				if (v instanceof RequiresContextBindable) {
					((RequiresContextBindable) v)
							.setBindable((SourcesPropertyChangeEvents) obj);
				}
				NamedParameter msg = NamedParameter.Support.getParameter(
						validatorAnnotation.parameters(),
						cc.alcina.framework.common.client.logic.reflection.Validator.FEEDBACK_MESSAGE);
				if (msg != null
						&& validationFeedback instanceof AbstractValidationFeedback) {
					((AbstractValidationFeedback) validationFeedback)
							.addMessage(validatorAnnotation.validator(),
									msg.stringValue());
				}
				if (validatorAnnotation.validateBeanOnly()) {
					v = new BeanValidationOnlyValidator(v);
				}
				cv.add(v);
			}
			return cv;
		} else {
			return validatorMap.get(clazz);
		}
	}

	public boolean hasDescriptor(Class clazz) {
		try {
			return ClientReflector.get().beanInfoForClass(clazz) != null;
		} catch (RuntimeException re) {
			return false;
		}
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
					&& PermissionsManager.get().isPermitted(obj,
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

	@Override
	public boolean isReadOnly(Class objectClass, String propertyName) {
		return getDescriptorForClass(objectClass).getProperty(propertyName)
				.getMutatorMethod() == null;
	}

	public void setDateRendererProvider(
			GwittirDateRendererProvider dateRendererProvider) {
		this.dateRendererProvider = dateRendererProvider;
	}

	public void setIgnoreProperties(List<String> ignoreProperties) {
		this.ignoreProperties = ignoreProperties;
	}

	@Override
	public void setPropertyValue(Object o, String propertyName, Object value) {
		try {
			getProperty(o, propertyName).getMutatorMethod().invoke(o,
					new Object[] { value });
		} catch (Exception e) {
			try {
				throw new WrappedRuntimeException(Ax.format(
						"Unable to set property %s for object %s to value %s",
						propertyName, o, value), e,
						SuggestedAction.NOTIFY_WARNING);
			} catch (Exception e1) {
				// tostring problem
				throw new WrappedRuntimeException(Ax.format(
						"Unable to set property %s for object %s to value %s",
						propertyName, o.getClass(),
						value == null ? null : value.getClass()), e,
						SuggestedAction.NOTIFY_WARNING);
			}
		}
	}

	public static class BoundWidgetProviderTextBox
			implements BoundWidgetProvider {
		@Override
		public TextBox get() {
			return new TextBox();
		}
	}

	public static class BoundWidgetTypeFactorySimpleGenerator
			extends BoundWidgetTypeFactory {
		private boolean withNull;

		public BoundWidgetTypeFactorySimpleGenerator() {
			super(true);
			add(Date.class, new DateBoxProvider());
			add(String.class, new BoundWidgetProviderTextBox());
			add(Integer.class, new BoundWidgetProviderTextBox());
			add(int.class, new BoundWidgetProviderTextBox());
			add(Long.class, new BoundWidgetProviderTextBox());
			add(long.class, new BoundWidgetProviderTextBox());
			add(Float.class, new BoundWidgetProviderTextBox());
			add(float.class, new BoundWidgetProviderTextBox());
			add(Double.class, new BoundWidgetProviderTextBox());
			add(double.class, new BoundWidgetProviderTextBox());
		}

		public BoundWidgetTypeFactorySimpleGenerator(boolean withNull) {
			this();
			this.withNull = withNull;
		}

		@Override
		public BoundWidgetProvider getWidgetProvider(Class type) {
			if (type.isEnum()) {
				return new ListBoxEnumProvider(type, withNull);
			}
			return super.getWidgetProvider(type);
		}
	}

	@RegistryLocation(registryPoint = DomainListProvider.class, implementationType = ImplementationType.SINGLETON)
	@ClientInstantiable
	public static class DomainListProvider {
		public BoundWidgetProvider getProvider(
				Class<? extends Entity> domainType,
				boolean propertyIsCollection) {
			return new ListBoxCollectionProvider(domainType,
					propertyIsCollection);
		}
	}

	@RegistryLocation(registryPoint = BeanDescriptorProvider.class, implementationType = ImplementationType.FACTORY)
	@ClientInstantiable
	public static class GwittirBridgeBdpFactory
			implements RegistryFactory<BeanDescriptorProvider> {
		@Override
		public BeanDescriptorProvider impl() {
			return GwittirBridge.get();
		}
	}

	public static class GwittirDateRendererProvider {
		public BoundWidgetProvider getRenderer(Display display) {
			if (display.rendererHint().equals(HINT_DATE_WITH_TIME_TITLE)) {
				return AU_DATE_TIME_TITLE_PROVIDER;
			} else {
				return AU_DATE_PROVIDER;
			}
		}
	}

	class FieldDisplayNameComparator implements Comparator<Field> {
		private final ClientBeanReflector bi;

		FieldDisplayNameComparator(ClientBeanReflector bi) {
			this.bi = bi;
		}

		@Override
		public int compare(Field o1, Field o2) {
			return bi.getPropertyReflectors().get(o1.getPropertyName())
					.compareTo(bi.getPropertyReflectors()
							.get(o2.getPropertyName()));
		}
	}
}
