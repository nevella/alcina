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
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.totsp.gwittir.client.beans.Binding;
import com.totsp.gwittir.client.beans.Converter;
import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;
import com.totsp.gwittir.client.ui.BoundWidget;
import com.totsp.gwittir.client.ui.Renderer;
import com.totsp.gwittir.client.ui.table.Field;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;
import com.totsp.gwittir.client.ui.util.BoundWidgetTypeFactory;
import com.totsp.gwittir.client.validator.AbstractValidationFeedback;
import com.totsp.gwittir.client.validator.IntegerValidator;
import com.totsp.gwittir.client.validator.ValidationFeedback;
import com.totsp.gwittir.client.validator.Validator;

import cc.alcina.framework.common.client.gwittir.validator.BooleanEnsureNonNullCoverter;
import cc.alcina.framework.common.client.gwittir.validator.CompositeValidator;
import cc.alcina.framework.common.client.gwittir.validator.DoubleValidator;
import cc.alcina.framework.common.client.gwittir.validator.LongValidator;
import cc.alcina.framework.common.client.gwittir.validator.ParameterisedValidator;
import cc.alcina.framework.common.client.gwittir.validator.RequiresSourceValidator;
import cc.alcina.framework.common.client.gwittir.validator.ServerUniquenessValidator;
import cc.alcina.framework.common.client.gwittir.validator.ShortDateValidator;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.Association;
import cc.alcina.framework.common.client.logic.reflection.Custom;
import cc.alcina.framework.common.client.logic.reflection.DefaultAnnotationResolver;
import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.PropertyOrder;
import cc.alcina.framework.common.client.logic.reflection.PropertyPermissions;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.Validators;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.common.client.provider.TextProvider;
import cc.alcina.framework.common.client.reflection.ClassReflector;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.CommonUtils.DateStyle;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.gwt.client.gwittir.customiser.Customiser;
import cc.alcina.framework.gwt.client.gwittir.customiser.ModelPlaceValueCustomiser;
import cc.alcina.framework.gwt.client.gwittir.provider.ExpandableDomainNodeCollectionLabelProvider;
import cc.alcina.framework.gwt.client.gwittir.provider.FriendlyEnumLabelProvider;
import cc.alcina.framework.gwt.client.gwittir.provider.ListBoxCollectionProvider;
import cc.alcina.framework.gwt.client.gwittir.provider.ListBoxEnumProvider;
import cc.alcina.framework.gwt.client.gwittir.renderer.DisplayNameRenderer;
import cc.alcina.framework.gwt.client.gwittir.widget.RenderingLabel;
import cc.alcina.framework.gwt.client.gwittir.widget.TextBox;
import cc.alcina.framework.gwt.client.widget.RelativePopupValidationFeedback;

/**
 * @author Nick Reddel
 */
public class BeanFields {
	public static final String HINT_DATE_WITH_TIME_TITLE = "HINT_DATE_WITH_TIME_TITLE";

	public static final String HINT_DATE_WITH_TIME_TITLE_TZ = "HINT_DATE_WITH_TIME_TITLE_TZ";

	public static final String CONTEXT_ALLOW_NULL_BOUND_WIDGET_PROVIDERS = BeanFields.class
			.getName() + ".CONTEXT_ALLOW_NULL_BOUND_WIDGET_PROVIDERS";

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

	public static final BoundWidgetProvider AU_DATE_TIME_TITLE_PROVIDER_TZ = new BoundWidgetProvider() {
		@Override
		public BoundWidget get() {
			RenderingLabel label = new RenderingLabel();
			label.setWordWrap(false);
			label.setRenderer(DATE_SLASH_RENDERER_WITH_NULL);
			label.setTitleRenderer(DATE_TIME_RENDERER_TZ);
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

	public static final Renderer DATE_TIME_RENDERER_TZ = new Renderer() {
		@Override
		public Object render(Object o) {
			Date d = (Date) o;
			return d == null ? ""
					: CommonUtils.formatDate(d, DateStyle.AU_DATE_TIME_TZ);
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

	private static boolean renderDatesWithTimezoneTitle;

	public static BoundWidget createWidget(Binding parent, Field field,
			SourcesPropertyChangeEvents target, Object model) {
		final BoundWidget widget;
		Binding binding;
		if (field.getCellProvider() != null) {
			widget = field.getCellProvider().get();
		} else {
			widget = new BoundWidgetTypeFactory().getWidgetProvider(Reflections
					.at(target).property(field.getPropertyName()).getType())
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

	public static Converter getDefaultConverter(BoundWidgetProvider bwp,
			Class propertyType) {
		if (bwp == null) {
			return null;
		}
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

	public static Field getFieldToFocus(List<Field> fields) {
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

	public static boolean isRenderDatesWithTimezoneTitle() {
		return renderDatesWithTimezoneTitle;
	}

	public static FieldQuery query() {
		return new BeanFields().new FieldQuery();
	}

	public static void setRenderDatesWithTimezoneTitle(
			boolean renderDatesWithTimezoneTitle) {
		BeanFields.renderDatesWithTimezoneTitle = renderDatesWithTimezoneTitle;
	}

	private Map<Class, Validator> validatorMap = new HashMap<Class, Validator>();
	{
		validatorMap.put(Integer.class, IntegerValidator.INSTANCE);
		validatorMap.put(int.class, new IntegerValidator.Primitive());
		validatorMap.put(Long.class, LongValidator.INSTANCE);
		validatorMap.put(long.class, new LongValidator.Primitive());
		validatorMap.put(Double.class, DoubleValidator.INSTANCE);
		validatorMap.put(double.class, new DoubleValidator.Primitive());
		validatorMap.put(Date.class, ShortDateValidator.INSTANCE);
	}

	private GwittirDateRendererProvider dateRendererProvider = new GwittirDateRendererProvider();

	BoundWidgetTypeFactory factory = new BoundWidgetTypeFactory();

	private BeanFields() {
		super();
	}

	public Object findObjectWithPropertyInCollection(Collection c,
			String propertyName, Object value) {
		for (Object o : c) {
			Object pv = Reflections.at(o).property(propertyName).get(o);
			if (pv != null && pv.equals(value)) {
				return o;
			}
		}
		return null;
	}

	public GwittirDateRendererProvider getDateRendererProvider() {
		return this.dateRendererProvider;
	}

	public List<Field> listFields(FieldQuery query) {
		query.validationFeedbackProvider = query.validationFeedbackProvider != null
				? query.validationFeedbackProvider
				: ValidationFeedback.Support.DEFAULT_PROVIDER;
		ClassReflector<? extends Object> classReflector = Reflections
				.at(query.clazz);
		return classReflector.properties().stream().map(property -> {
			String propertyName = property.getName();
			if (query.propertyName != null
					&& !(query.propertyName.equals(propertyName))) {
				return null;
			}
			boolean editableField = query.editable
					&& query.editableNamePredicate.test(propertyName);
			FieldQuery perFieldQuery = query.clone().asEditable(editableField)
					.forPropertyName(propertyName);
			return getField(perFieldQuery);
		}).filter(Objects::nonNull).sorted(new FieldOrdering(classReflector))
				.collect(Collectors.toList());
	}

	public void setDateRendererProvider(
			GwittirDateRendererProvider dateRendererProvider) {
		this.dateRendererProvider = dateRendererProvider;
	}

	Field getField(FieldQuery query) {
		Class clazz = query.clazz;
		Object object = query.bean;
		AnnotationLocation clazzLocation = new AnnotationLocation(clazz, null,
				query.resolver);
		Bean bean = clazzLocation.getAnnotation(Bean.class);
		ClassReflector<?> classReflector = Reflections.at(clazz);
		ObjectPermissions op = clazzLocation
				.getAnnotation(ObjectPermissions.class);
		object = object != null ? object : classReflector.templateInstance();
		Property property = Reflections.at(clazz).property(query.propertyName);
		AnnotationLocation propertyLocation = new AnnotationLocation(clazz,
				property, query.resolver);
		Class type = classReflector.property(query.propertyName).getType();
		BoundWidgetProvider bwp = factory.getWidgetProvider(type);
		int position = query.multiple ? RelativePopupValidationFeedback.BOTTOM
				: RelativePopupValidationFeedback.RIGHT;
		Display display = propertyLocation.getAnnotation(Display.class);
		Display.AllProperties displayAllProperties = propertyLocation
				.getAnnotation(Display.AllProperties.class);
		if (display != null) {
			PropertyPermissions pp = propertyLocation
					.getAnnotation(PropertyPermissions.class);
			Association association = propertyLocation
					.getAnnotation(Association.class);
			boolean fieldVisible = PermissionsManager.get()
					.checkEffectivePropertyPermission(op, pp, object, true)
					&& display != null
					&& PermissionsManager.get().isPermitted(object,
							display.visible())
					&& ((display.displayMask()
							& Display.DISPLAY_AS_PROPERTY) != 0);
			if (!fieldVisible) {
				return null;
			}
			boolean focus = display.focus();
			boolean propertyIsCollection = type == Set.class;
			boolean fieldEditable = query.editable
					&& (PermissionsManager.get()
							.checkEffectivePropertyPermission(op, pp, object,
									false)
							|| ((display.displayMask()
									& Display.DISPLAY_EDITABLE) != 0))
					&& ((display.displayMask() & Display.DISPLAY_RO) == 0);
			Class domainType = type;
			domainType = (association == null || !propertyIsCollection
					|| association.implementationClass() == void.class)
							? domainType
							: association.implementationClass();
			boolean isDomainClass = Reflections.isAssignableFrom(Entity.class,
					domainType);
			if (bwp == null && isDomainClass) {
				if (fieldEditable) {
					bwp = Registry.impl(DomainListProvider.class)
							.getProvider(domainType, propertyIsCollection);
				} else {
					if (propertyIsCollection) {
						bwp = new ExpandableDomainNodeCollectionLabelProvider(
								MAX_EXPANDABLE_LABEL_LENGTH, true);
					} else {
						if (query.multiple) {
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
						query.multiple, customiserInfo, propertyLocation);
			}
			if (bwp != null || LooseContext
					.is(CONTEXT_ALLOW_NULL_BOUND_WIDGET_PROVIDERS)) {
				ValidationFeedback validationFeedback = null;
				Validator validator = null;
				if (fieldEditable) {
					validationFeedback = query.validationFeedbackProvider
							.builder().forPropertyName(query.propertyName)
							.displayDirection(query.multiple
									? ValidationFeedback.Provider.Direction.BOTTOM
									: ValidationFeedback.Provider.Direction.RIGHT)
							.createFeedback();
					validator = getValidator(domainType, object,
							query.propertyName, validationFeedback);
				}
				Field field = new Field(property,
						TextProvider.get().getLabelText(propertyLocation), bwp,
						validator, validationFeedback,
						getDefaultConverter(bwp, type), clazz, query.resolver);
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
		} else if (displayAllProperties != null
				&& PermissionsManager.get().checkEffectivePropertyPermission(op,
						null, object, !query.editable)) {
			// no property info, but all writeable (if object is set)
			RelativePopupValidationFeedback vf = new RelativePopupValidationFeedback(
					position);
			vf.setCss(query.multiple ? null : "gwittir-ValidationPopup-right");
			if (bwp != null && !query.editable) {
				Class domainType = type;
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
				Class domainType = type;
				boolean isEnum = domainType.isEnum();
				if (domainType == Date.class) {
					bwp = AU_DATE_PROVIDER;
				} else if (isEnum) {
					bwp = query.editable
							? new ListBoxEnumProvider(domainType, true)
							: NOWRAP_LABEL_PROVIDER;
				} else if (domainType == boolean.class
						|| domainType == Boolean.class) {
					bwp = YES_NO_LABEL_PROVIDER;
				} else {
					bwp = NOWRAP_LABEL_PROVIDER;
				}
			}
			return new Field(property,
					TextProvider.get().getLabelText(propertyLocation), bwp,
					getValidator(type, object, query.propertyName, vf), vf,
					getDefaultConverter(bwp, type), clazz, query.resolver);
		}
		return null;
	}

	Validator getValidator(Class<?> clazz, Object obj, String propertyName,
			ValidationFeedback validationFeedback) {
		ClassReflector<? extends Object> classReflector = Reflections.at(obj);
		Property property = classReflector.property(propertyName);
		List<cc.alcina.framework.common.client.logic.reflection.Validator> validators = new ArrayList<>();
		cc.alcina.framework.common.client.logic.reflection.Validators validatorsAnn = property == null
				? null
				: property.annotation(Validators.class);
		cc.alcina.framework.common.client.logic.reflection.Validator info = property == null
				? null
				: property.annotation(
						cc.alcina.framework.common.client.logic.reflection.Validator.class);
		if (propertyName == null) {
			validatorsAnn = classReflector.annotation(Validators.class);
			info = classReflector.annotation(
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
				Validator v = Reflections
						.newInstance(validatorAnnotation.value());
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
							.addMessage(validatorAnnotation.value(),
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

	public static class BoundWidgetProviderTextBox
			implements BoundWidgetProvider {
		@Override
		public TextBox get() {
			return new TextBox();
		}
	}

	@Reflected
	@Registration.Singleton
	public static class DomainListProvider {
		public BoundWidgetProvider getProvider(
				Class<? extends Entity> domainType,
				boolean propertyIsCollection) {
			return new ListBoxCollectionProvider(domainType,
					propertyIsCollection);
		}
	}

	public static class FieldOrdering implements Comparator<Field> {
		private final ClassReflector<?> classReflector;

		private PropertyOrder propertyOrder;

		FieldOrdering(ClassReflector<?> classReflector) {
			this.classReflector = classReflector;
			this.propertyOrder = classReflector.annotation(PropertyOrder.class);
		}

		@Override
		public int compare(Field o1, Field o2) {
			if (propertyOrder != null) {
				int idx1 = Arrays.asList(propertyOrder.value())
						.indexOf(o1.getPropertyName());
				int idx2 = Arrays.asList(propertyOrder.value())
						.indexOf(o2.getPropertyName());
				if (propertyOrder.fieldOrder()) {
					idx1 = classReflector.properties().indexOf(
							classReflector.property(o1.getPropertyName()));
					idx2 = classReflector.properties().indexOf(
							classReflector.property(o2.getPropertyName()));
				}
				if (idx1 == -1) {
					if (idx2 == -1) {
						// fall through
					} else {
						return 1;
					}
				} else {
					if (idx2 == -1) {
						return -1;
					} else {
						return idx1 - idx2;
					}
				}
			}
			Property p1 = classReflector.property(o1.getPropertyName());
			Property p2 = classReflector.property(o2.getPropertyName());
			int orderingHint1 = RenderedProperty.orderingHint(p1);
			int orderingHint2 = RenderedProperty.orderingHint(p2);
			int i = CommonUtils.compareInts(orderingHint1, orderingHint2);
			if (i != 0) {
				return i;
			}
			return RenderedProperty.displayName(p1)
					.compareToIgnoreCase(RenderedProperty.displayName(p2));
		}
	}

	/**
	 * <p>
	 * Generate the edit {@link Field} objects for a given class which are used
	 * to create an edit form, detail view or table rendering of one or more
	 * instances
	 * <p>
	 * The default field configuration is non-editable, non-adjunct (i.e. when
	 * editing, changes are made directly to the object without save/cancel) and
	 * single (i.e. fields are for a form, not a table)
	 *
	 *
	 */
	public class FieldQuery {
		Class clazz;

		String propertyName;

		boolean editable;

		boolean multiple;

		AnnotationLocation.Resolver resolver = new DefaultAnnotationResolver();

		boolean adjunct;

		Predicate<String> editableNamePredicate = name -> true;

		Object bean;

		ValidationFeedback.Provider validationFeedbackProvider;

		public FieldQuery asAdjunctEditor(boolean adjunct) {
			this.adjunct = adjunct;
			return this;
		}

		public FieldQuery asEditable(boolean editable) {
			this.editable = editable;
			return this;
		}

		@Override
		public FieldQuery clone() {
			return new FieldQuery().asAdjunctEditor(adjunct)
					.asEditable(editable).forClass(clazz).forBean(bean)
					.forPropertyName(propertyName)
					.forMultipleWidgetContainer(multiple)
					.withEditableNamePredicate(editableNamePredicate)
					.withResolver(resolver)
					.withValidationFeedbackProvider(validationFeedbackProvider);
		}

		public FieldQuery forBean(Object bean) {
			this.bean = bean;
			if (bean != null) {
				forClass(bean.getClass());
			}
			return this;
		}

		public FieldQuery forClass(Class clazz) {
			this.clazz = clazz;
			return this;
		}

		public FieldQuery forMultipleWidgetContainer(boolean multiple) {
			this.multiple = multiple;
			return this;
		}

		public FieldQuery forPropertyName(String propertyName) {
			this.propertyName = propertyName;
			return this;
		}

		public Field getField() {
			return listFields().get(0);
		}

		public Validator getValidator() {
			// TODO Auto-generated method stub
			return null;
		}

		public List<Field> listFields() {
			return BeanFields.this.listFields(this);
		}

		public FieldQuery withEditableNamePredicate(
				Predicate<String> editableNamePredicate) {
			if (editableNamePredicate != null) {
				this.editableNamePredicate = editableNamePredicate;
			}
			return this;
		}

		public FieldQuery withResolver(AnnotationLocation.Resolver resolver) {
			if (resolver != null) {
				this.resolver = resolver;
			}
			return this;
		}

		public FieldQuery withValidationFeedbackProvider(
				ValidationFeedback.Provider validationFeedbackProvider) {
			this.validationFeedbackProvider = validationFeedbackProvider;
			return this;
		}
	}

	public static class GwittirDateRendererProvider {
		public BoundWidgetProvider getRenderer(Display display) {
			String rendererHint = display.rendererHint();
			switch (display.rendererHint()) {
			case HINT_DATE_WITH_TIME_TITLE:
				return AU_DATE_TIME_TITLE_PROVIDER;
			case HINT_DATE_WITH_TIME_TITLE_TZ:
				return AU_DATE_TIME_TITLE_PROVIDER_TZ;
			default:
				return renderDatesWithTimezoneTitle
						? AU_DATE_TIME_TITLE_PROVIDER_TZ
						: AU_DATE_PROVIDER;
			}
		}
	}
}
