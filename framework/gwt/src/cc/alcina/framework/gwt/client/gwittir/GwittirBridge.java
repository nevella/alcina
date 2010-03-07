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

import java.io.Serializable;
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

import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.WrappedRuntimeException.SuggestedAction;
import cc.alcina.framework.common.client.gwittir.validator.LongValidator;
import cc.alcina.framework.common.client.gwittir.validator.ParameterisedValidator;
import cc.alcina.framework.common.client.gwittir.validator.ServerUniquenessValidator;
import cc.alcina.framework.common.client.logic.domaintransform.spi.PropertyAccessor;
import cc.alcina.framework.common.client.logic.permissions.HasId;
import cc.alcina.framework.common.client.logic.permissions.HasIdAndLocalId;
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
import cc.alcina.framework.gwt.client.gwittir.SetBasedListBox.DomainListBox;
import cc.alcina.framework.gwt.client.gwittir.customisers.Customiser;
import cc.alcina.framework.gwt.client.ide.provider.CollectionFilter;
import cc.alcina.framework.gwt.client.widget.Link;
import cc.alcina.framework.gwt.client.widget.RelativePopupValidationFeedback;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.beans.BeanDescriptor;
import com.totsp.gwittir.client.beans.Introspector;
import com.totsp.gwittir.client.beans.Property;
import com.totsp.gwittir.client.ui.AbstractBoundWidget;
import com.totsp.gwittir.client.ui.BoundWidget;
import com.totsp.gwittir.client.ui.Label;
import com.totsp.gwittir.client.ui.Renderer;
import com.totsp.gwittir.client.ui.ToStringRenderer;
import com.totsp.gwittir.client.ui.table.Field;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;
import com.totsp.gwittir.client.ui.util.BoundWidgetTypeFactory;
import com.totsp.gwittir.client.validator.CompositeValidator;
import com.totsp.gwittir.client.validator.IntegerValidator;
import com.totsp.gwittir.client.validator.ValidationException;
import com.totsp.gwittir.client.validator.Validator;

@SuppressWarnings("unchecked")
/**
 *
 * @author <a href="mailto:nick@alcina.cc">Nick Reddel</a>
 */

 public class GwittirBridge implements PropertyAccessor {
	private Map<Class, Validator> validatorMap = new HashMap<Class, Validator>();
	{
		validatorMap.put(Integer.class, IntegerValidator.INSTANCE);
		validatorMap.put(int.class, IntegerValidator.INSTANCE);
		validatorMap.put(Long.class, LongValidator.INSTANCE);
		validatorMap.put(long.class, LongValidator.INSTANCE);
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
			add(Date.class, PopupDatePickerWithText.PROVIDER);
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

	public static final Validator DATE_TEXT_VALIDATOR = new Validator() {
		public static final transient String ERR_FMT = "Dates must be "
				+ "entered in the following format: dd/mm/yyyy";

		public static final transient String ERR_INVALID = "The date entered does not exist";

		@SuppressWarnings("deprecation")
		public Object validate(Object value) throws ValidationException {
			if (value == null) {
				throw new ValidationException(ERR_FMT);
			}
			String sValue = value.toString();
			String[] splits = sValue.split("/");
			if (splits.length != 3) {
				throw new ValidationException(ERR_FMT);
			}
			try {
				Date result = new Date(Integer.parseInt(splits[2]) - 1900,
						Integer.parseInt(splits[1]) - 1, Integer
								.parseInt(splits[0]));
				return result;
			} catch (Exception e) {
				throw new ValidationException(ERR_INVALID);
			}
		}
	};

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
				bwp = customiser.getRenderer(fieldEditable, domainType,
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
						.getLabelText(c, pr), bwp, validator, vf);
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
					pr.getPropertyName(), vf), vf);
		}
		return null;
	}

	public Field[] fieldsForReflectedObjectAndSetupWidgetFactory(Object obj,
			BoundWidgetTypeFactory factory, boolean editableWidgets,
			boolean multiple, String propertyName) {
		factory.add(Date.class, PopupDatePickerWithText.PROVIDER);
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
		Collections.sort(fields, new PrFieldComparator(bi));
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

	public interface HasMaxWidth {
		public int getMaxWidth();

		public boolean isForceColumnWidth();
	}

	public static class ExpandableDomainNodeCollectionLabelProvider implements
			BoundWidgetProvider, HasMaxWidth {
		private final int maxWidth;

		private final boolean forceColumnWidth;

		public ExpandableDomainNodeCollectionLabelProvider(int maxWidth,
				boolean forceColumnWidth) {
			this.maxWidth = maxWidth;
			this.forceColumnWidth = forceColumnWidth;
		}

		public BoundWidget get() {
			return new ExpandableLabel(maxWidth);
		}

		public int getMaxWidth() {
			return maxWidth;
		}

		public boolean isForceColumnWidth() {
			return forceColumnWidth;
		}
	};

	public static class ExpandableStringLabelProvider implements
			BoundWidgetProvider, HasMaxWidth {
		private final int maxWidth;

		private final boolean forceColumnWidth;

		private final boolean showNewlinesAsBreaks;

		public ExpandableStringLabelProvider(int maxWidth,
				boolean forceColumnWidth, boolean showNewlinesAsBreaks) {
			this.maxWidth = maxWidth;
			this.forceColumnWidth = forceColumnWidth;
			this.showNewlinesAsBreaks = showNewlinesAsBreaks;
		}

		public BoundWidget get() {
			ExpandableLabel label = new ExpandableLabel(maxWidth);
			label.setShowNewlinesAsBreaks(showNewlinesAsBreaks);
			return label;
		}

		public int getMaxWidth() {
			return maxWidth;
		}

		public boolean isForceColumnWidth() {
			return forceColumnWidth;
		}
	};

	public static class FixedWidthLabelProvider extends
			ExpandableStringLabelProvider {
		public FixedWidthLabelProvider(int maxWidth,
				boolean showNewlinesAsBreaks) {
			super(maxWidth, true, showNewlinesAsBreaks);
		}

		@Override
		public BoundWidget get() {
			return new Label();
		}
	}

	public static final BoundWidgetProvider COLL_DN_LABEL_PROVIDER = new BoundWidgetProvider() {
		public BoundWidget get() {
			Label label = new Label();
			label.setRenderer(CollectionDisplayNameRenderer.INSTANCE);
			return label;
		}
	};

	public static final BoundWidgetProvider NOWRAP_LABEL_PROVIDER = new BoundWidgetProvider() {
		public BoundWidget get() {
			Label label = new Label();
			label.setWordWrap(false);
			return label;
		}
	};

	public static final BoundWidgetProvider YES_NO_LABEL_PROVIDER = new BoundWidgetProvider() {
		public BoundWidget get() {
			Label label = new Label();
			label.setWordWrap(false);
			label.setRenderer(YES_NO_RENDERER);
			return label;
		}
	};

	public static final BoundWidgetProvider CLASS_SIMPLE_NAME_PROVIDER = new BoundWidgetProvider() {
		public BoundWidget get() {
			Label label = new Label();
			label.setWordWrap(false);
			label.setRenderer(CLASS_SIMPLE_NAME_RENDERER);
			return label;
		}
	};

	public static final Renderer CLASS_SIMPLE_NAME_RENDERER = new ClassSimpleNameRenderer();

	public static class ClassSimpleNameRenderer implements Renderer {
		public Object render(Object o) {
			if (o == null) {
				return null;
			}
			String s = o.toString();
			return s.substring(s.lastIndexOf('.') + 1);
		}
	};

	public static final BoundWidgetProvider AU_DATE_PROVIDER = new BoundWidgetProvider() {
		public BoundWidget get() {
			Label label = new Label();
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

	public static final Renderer DATE_SLASH_RENDERER = new Renderer() {
		public Object render(Object o) {
			Date d = (Date) o;
			return d == null ? "" : CommonUtils.formatDate(d,
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

	// TODO - make the expansion lazy (can be a big hit...) - and, in fact, a
	// popup style would generally be prettier/nicer/lighter-weight
	public static class ExpandableLabel extends AbstractBoundWidget {
		private FlowPanel fp;

		private boolean showNewlinesAsBreaks;

		public boolean isShowNewlinesAsBreaks() {
			return this.showNewlinesAsBreaks;
		}

		public void setShowNewlinesAsBreaks(boolean showNewlinesAsBreaks) {
			this.showNewlinesAsBreaks = showNewlinesAsBreaks;
		}

		private boolean showHideAtEnd;

		private Link hideLink;

		private boolean hiding;

		private final int maxLength;

		public ExpandableLabel(int maxLength) {
			this.maxLength = maxLength;
			this.fp = new FlowPanel();
			initWidget(fp);
			fp.setStyleName("alcina-expandableLabel");
		}

		public Object getValue() {
			return null;
		}

		List<Widget> hiddenWidgets = new ArrayList<Widget>();

		private InlineLabel dots;

		private InlineHTML space;

		private Link showLink;

		private int getMaxLength() {
			return maxLength;
		}

		public void setValue(Object o) {
			fp.clear();
			if (o == null) {
				// fp.add(new InlineLabel("[Undefined]"));
				return;
			}
			if (o instanceof Collection) {
				ArrayList l = new ArrayList((Collection) o);
				Collections.sort(l);
				int strlen = 0;
				for (Object object : l) {
					InlineLabel comma = new InlineLabel(", ");
					if (strlen > 0) {
						fp.add(comma);
						strlen += 2;
					}
					String name = ClientReflector.get().displayNameForObject(
							object);
					InlineLabel label = new InlineLabel(name);
					if (strlen > getMaxLength()) {
						comma.setVisible(false);
						label.setVisible(false);
						hiddenWidgets.add(comma);
						hiddenWidgets.add(label);
					}
					strlen += name.length();
					fp.add(label);
				}
			} else {
				String s = o.toString();
				if (isShowNewlinesAsBreaks()) {
					s = s.replace("\n", "<br>\n");
				}
				int maxC = getMaxLength();
				int y1 = s.indexOf(">", maxC);
				int y2 = s.indexOf("<", maxC);
				if (y1 < y2 && y1 != -1) {
					maxC = y1 + 1;
				}
				String vis = CommonUtils.trimToWsChars(s, maxC);
				com.google.gwt.user.client.ui.Label label;
				if (s.length() == vis.length()) {
					label = isShowNewlinesAsBreaks() ? new InlineHTML(s)
							: new InlineLabel(s);
					fp.add(label);
				} else {
					label = isShowNewlinesAsBreaks() ? new InlineHTML(vis)
							: new InlineLabel(vis);
					fp.add(label);
					String notVis = s.substring(vis.length());
					label = isShowNewlinesAsBreaks() ? new InlineHTML(notVis)
							: new InlineLabel(notVis);
					label.setVisible(false);
					fp.add(label);
					hiddenWidgets.add(label);
				}
			}
			if (hiddenWidgets.size() != 0) {
				this.dots = new InlineLabel("...");
				this.space = new InlineHTML("&nbsp;");
				fp.add(dots);
				this.hiding = true;
				this.showLink = new Link("[more]");
				this.hideLink = new Link("[less]");
				if (isShowHideAtEnd()) {
					fp.add(space);
					fp.add(hideLink);
				} else {
					fp.insert(hideLink, 0);
					fp.add(space);
				}
				hideLink.setVisible(false);
				space.setVisible(false);
				fp.add(showLink);
				hideLink.addClickHandler(showHideListener);
				showLink.addClickHandler(showHideListener);
			}
		}

		public void setShowHideAtEnd(boolean showHideAtEnd) {
			this.showHideAtEnd = showHideAtEnd;
		}

		public boolean isShowHideAtEnd() {
			return showHideAtEnd;
		}

		ClickHandler showHideListener = new ClickHandler() {
			public void onClick(ClickEvent event) {
				Widget sender = (Widget) event.getSource();
				hiding = !hiding;
				for (Widget w : hiddenWidgets) {
					w.setVisible(!hiding);
				}
				hideLink.setVisible(!hiding);
				space.setVisible(!hiding);
				showLink.setVisible(hiding);
				dots.setVisible(hiding);
			}
		};
	}

	public static class DomainObjectIdRefProvider implements
			BoundWidgetProvider {
		private final Class targetObjectClass;

		public DomainObjectIdRefProvider(Class targetObjectClass) {
			this.targetObjectClass = targetObjectClass;
		}

		public BoundWidget get() {
			Label label = new Label();
			label.setRenderer(new DisplayNameIdRefRenderer(targetObjectClass));
			return label;
		}
	}

	public static final BoundWidgetProvider DN_LABEL_PROVIDER = new BoundWidgetProvider() {
		public BoundWidget get() {
			Label label = new Label();
			label.setRenderer(DisplayNameRenderer.INSTANCE);
			return label;
		}
	};

	public static final class ListBoxCollectionProvider implements
			BoundWidgetProvider {
		private final Class clazz;

		private final boolean propertyIsCollection;

		private final boolean noNullOption;

		private CollectionFilter filter;

		public ListBoxCollectionProvider(Class clazz,
				boolean propertyIsCollection) {
			this(clazz, propertyIsCollection, false);
		}

		public ListBoxCollectionProvider(Class clazz,
				boolean propertyIsCollection, boolean noNullOption) {
			this.clazz = clazz;
			this.propertyIsCollection = propertyIsCollection;
			this.noNullOption = noNullOption;
		}

		public DomainListBox get() {
			DomainListBox listBox = new DomainListBox(clazz, filter,
					!propertyIsCollection && !noNullOption);
			listBox.setRenderer(DisplayNameRenderer.INSTANCE);
			listBox.setComparator(EqualsComparator.INSTANCE);
			listBox.setMultipleSelect(propertyIsCollection);
			return listBox;
		}

		public void setFilter(CollectionFilter filter) {
			this.filter = filter;
		}

		public CollectionFilter getFilter() {
			return filter;
		}
	}

	public static class ListBoxEnumProvider implements BoundWidgetProvider {
		private final Class<? extends Enum> clazz;

		private List<Enum> hiddenValues = new ArrayList<Enum>();

		private boolean withNull;

		private boolean multiple;

		private Renderer renderer = FriendlyEnumRenderer.INSTANCE;

		public ListBoxEnumProvider(Class<? extends Enum> clazz) {
			this(clazz, false);
		}

		public ListBoxEnumProvider(Class<? extends Enum> clazz, boolean withNull) {
			this.clazz = clazz;
			this.setWithNull(withNull);
		}

		public BoundWidget get() {
			SetBasedListBox listBox = new SetBasedListBox();
			Enum[] enumValues = clazz.getEnumConstants();
			List options = new ArrayList(Arrays.asList(enumValues));
			for (Enum e : hiddenValues) {
				options.remove(e);
			}
			if (isWithNull()) {
				options.add(0, null);
			}
			listBox.setRenderer(getRenderer());
			listBox.setComparator(EqualsComparator.INSTANCE);
			listBox.setSortOptions(false);
			listBox.setOptions(options);
			listBox.setMultipleSelect(multiple);
			return listBox;
		}

		public void setHiddenValues(List<Enum> hiddenValues) {
			this.hiddenValues = hiddenValues;
		}

		public List<Enum> getHiddenValues() {
			return hiddenValues;
		}

		public void setWithNull(boolean withNull) {
			this.withNull = withNull;
		}

		public boolean isWithNull() {
			return withNull;
		}

		public void setMultiple(boolean multiple) {
			this.multiple = multiple;
		}

		public boolean isMultiple() {
			return multiple;
		}

		public void setRenderer(Renderer renderer) {
			this.renderer = renderer;
		}

		public Renderer getRenderer() {
			return renderer;
		}
	}

	public static final ToStringRenderer TO_STRING_RENDERER_INSTANCE = new ToStringRenderer();

	public static class FriendlyEnumRenderer extends ToStringRenderer {
		public static final FriendlyEnumRenderer INSTANCE = new FriendlyEnumRenderer();

		@Override
		public Object render(Object o) {
			if (o == null) {
				return "-- any --";
			}
			return CommonUtils.friendlyConstant(o);
		}
	}

	public static class TitleCaseEnumRenderer extends ToStringRenderer {
		public static final TitleCaseEnumRenderer INSTANCE = new TitleCaseEnumRenderer();

		@Override
		public Object render(Object o) {
			if (o == null) {
				return "-- Any --";
			}
			return CommonUtils.titleCase(CommonUtils.friendlyConstant(o));
		}
	}

	public static final BoundWidgetProvider FRIENDLY_ENUM_LABEL_PROVIDER_INSTANCE = new FriendlyEnumLabelProvider();

	public static class FriendlyEnumLabelProvider implements
			BoundWidgetProvider {
		public BoundWidget get() {
			Label label = new Label();
			label.setRenderer(FriendlyEnumRenderer.INSTANCE);
			return label;
		}
	}

	public static class DisplayNameRenderer extends ToStringRenderer {
		public static final DisplayNameRenderer INSTANCE = new DisplayNameRenderer();

		public Object render(Object o) {
			if (o == null) {
				return "(Undefined)";
			}
			String dn = ClientReflector.get().displayNameForObject(o);
			return (dn == null) ? super.render(o) : dn;
		}
	}

	static class DisplayNameIdRefRenderer extends ToStringRenderer {
		private final Class targetClass;

		public DisplayNameIdRefRenderer(Class targetClass) {
			this.targetClass = targetClass;
		}

		public Object render(Object o) {
			if (o == null) {
				return "0";
			}
			Long id = (Long) o;
			HasIdAndLocalId object = CommonLocator.get().objectLookup()
					.getObject(targetClass, id, 0);
			String dn = null;
			if (object != null) {
				return ClientReflector.get().displayNameForObject(object);
			} else {
				return "(" + id + ")";
			}
		}
	}

	static class CollectionDisplayNameRenderer extends ToStringRenderer {
		static final CollectionDisplayNameRenderer INSTANCE = new CollectionDisplayNameRenderer();

		public Object render(Object o) {
			if (o == null || !(o instanceof Collection)) {
				return "(Undefined)";
			}
			String result = "";
			ArrayList l = new ArrayList((Collection) o);
			Collections.sort(l);
			for (Object object : l) {
				if (result.length() != 0) {
					result += ", ";
				}
				result += ClientReflector.get().displayNameForObject(object);
			}
			return result;
		}
	}

	/** note - not transitive...uh, it's a gwittir thing **/
	public static class EqualsComparator implements Comparator {
		public static final EqualsComparator INSTANCE = new EqualsComparator();

		public int compare(Object o1, Object o2) {
			if (o1 == null) {
				return o2 == null ? 0 : -1;
			}
			return o1.equals(o2) ? 0 : -1;
		}
	}

	static class SimpleComparatorWithNull implements Comparator, Serializable {
		static final SimpleComparatorWithNull INSTANCE = new SimpleComparatorWithNull();

		private SimpleComparatorWithNull() {
		}

		public int compare(Object o1, Object o2) {
			if (o1 == null && o2 == null) {
				return 0;
			}
			if (o1 instanceof Comparable && o2 instanceof Comparable) {
				return ((Comparable) o1).compareTo(o2);
			}
			if ((o1 == o2) || ((o1 != null) && (o2 != null) && o1.equals(o2))) {
				return 0;
			} else if ((o1 != null) && (o2 != null)) {
				return (o1 instanceof HasIdAndLocalId) ? -1 : o1.toString()
						.compareTo(o2.toString());
			} else if ((o1 != null) && (o2 == null)) {
				return +1;
			} else {
				return -1;
			}
		}
	}

	class PrFieldComparator implements Comparator<Field> {
		private final ClientBeanReflector bi;

		PrFieldComparator(ClientBeanReflector bi) {
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
