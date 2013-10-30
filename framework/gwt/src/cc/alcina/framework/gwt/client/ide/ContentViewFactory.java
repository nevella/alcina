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
package cc.alcina.framework.gwt.client.ide;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.WrappedRuntimeException.SuggestedAction;
import cc.alcina.framework.common.client.actions.PermissibleAction;
import cc.alcina.framework.common.client.actions.PermissibleActionEvent;
import cc.alcina.framework.common.client.actions.PermissibleActionHandler.DefaultPermissibleActionHandler;
import cc.alcina.framework.common.client.actions.PermissibleActionListener;
import cc.alcina.framework.common.client.actions.instances.CancelAction;
import cc.alcina.framework.common.client.actions.instances.NonstandardObjectAction;
import cc.alcina.framework.common.client.actions.instances.ViewAction;
import cc.alcina.framework.common.client.gwittir.validator.ServerValidator;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.ClientTransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.spi.PropertyAccessor;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.ClientBeanReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientReflector;
import cc.alcina.framework.common.client.logic.reflection.PropertyPermissions;
import cc.alcina.framework.common.client.logic.reflection.VisualiserInfo;
import cc.alcina.framework.common.client.provider.TextProvider;
import cc.alcina.framework.common.client.util.CloneHelper;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.client.gwittir.GwittirBridge;
import cc.alcina.framework.gwt.client.gwittir.GwittirUtils;
import cc.alcina.framework.gwt.client.gwittir.HasBinding;
import cc.alcina.framework.gwt.client.gwittir.HasMaxWidth;
import cc.alcina.framework.gwt.client.gwittir.provider.CollectionDataProvider;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundTableExt;
import cc.alcina.framework.gwt.client.gwittir.widget.GridForm;
import cc.alcina.framework.gwt.client.ide.widget.Toolbar;
import cc.alcina.framework.gwt.client.logic.AlcinaHistory.SimpleHistoryEventInfo;
import cc.alcina.framework.gwt.client.logic.OkCallback;
import cc.alcina.framework.gwt.client.util.WidgetUtils;
import cc.alcina.framework.gwt.client.widget.BreadcrumbBar;
import cc.alcina.framework.gwt.client.widget.Link;
import cc.alcina.framework.gwt.client.widget.UsefulWidgetFactory;
import cc.alcina.framework.gwt.client.widget.complex.FastROBoundTable;
import cc.alcina.framework.gwt.client.widget.dialog.CancellableRemoteDialog;
import cc.alcina.framework.gwt.client.widget.dialog.GlassDialogBox;
import cc.alcina.framework.gwt.client.widget.dialog.NonCancellableRemoteDialog;
import cc.alcina.framework.gwt.client.widget.layout.ExpandableListPanel;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.logical.shared.AttachEvent.Handler;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.beans.Binding;
import com.totsp.gwittir.client.beans.Binding.BindingInstance;
import com.totsp.gwittir.client.beans.Converter;
import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;
import com.totsp.gwittir.client.ui.AbstractBoundWidget;
import com.totsp.gwittir.client.ui.HasEnabled;
import com.totsp.gwittir.client.ui.table.DataProvider;
import com.totsp.gwittir.client.ui.table.Field;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;
import com.totsp.gwittir.client.ui.util.BoundWidgetTypeFactory;
import com.totsp.gwittir.client.validator.ValidationException;
import com.totsp.gwittir.client.validator.Validator;

@SuppressWarnings("unchecked")
/**
 *
 * @author Nick Reddel
 */
public class ContentViewFactory {
	public static final String CONTEXT_OVERRIDE_AUTOSAVE=ContentViewFactory.class.getName()+".CONTEXT_OVERRIDE_AUTOSAVE";
	public static class RecheckVisibilityHandler implements Handler {
		private final GridForm grid;

		public RecheckVisibilityHandler(GridForm grid) {
			this.grid = grid;
		}

		@Override
		public void onAttachOrDetach(AttachEvent event) {
			if (event.isAttached()) {
				try {
					PropertyAccessor pa = CommonLocator.get()
							.propertyAccessor();
					int r = 0;
					for (Binding b : grid.getBinding().getChildren()) {
						BindingInstance right = b.getRight();
						VisualiserInfo visualiserInfo = pa
								.getAnnotationForProperty(
										right.object.getClass(),
										VisualiserInfo.class,
										right.property.getName());
						if (visualiserInfo != null) {
							if (!PermissionsManager.get().isPermissible(
									right.object, visualiserInfo.visible())) {
								grid.setRowVisibility(r, false);
							}
						}
						PropertyPermissions pp = pa.getAnnotationForProperty(
								right.object.getClass(),
								PropertyPermissions.class,
								right.property.getName());
						if (pp != null) {
							if (!PermissionsManager.get().isPermissible(
									right.object, pp.write())) {
								SourcesPropertyChangeEvents left = b.getLeft().object;
								if (left instanceof HasEnabled
										&& !(left instanceof Link)) {
									((HasEnabled) left).setEnabled(false);
								}
							}
						}
						r++;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static final String CONTEXT_ADDITIONAL_PROVISIONAL_OBJECTS = ContentViewFactory.class
			+ ".CONTEXT_ADDITIONAL_PROVISIONAL_OBJECTS";

	private boolean noButtons;

	private boolean cancelButton;

	private boolean noCaption;

	public static void registerAdditionalProvisionalObjects(Object o) {
		if (o == null) {
			return;
		}
		LooseContext.getContext()
				.set(CONTEXT_ADDITIONAL_PROVISIONAL_OBJECTS, o);
	}

	public PaneWrapperWithObjects createActionTableWithCaption(
			Collection beans, Class beanClass, Converter converter,
			Collection<PermissibleAction> actions,
			PermissibleActionListener listener, boolean withObjectActions,
			boolean multiple) {
		PaneWrapperWithObjects cp = createPaneWrapper(listener);
		cp.add(createMultiCaption(beanClass, cp));
		cp.addActionTable(createActionTable(beans, beanClass, converter,
				actions, listener, withObjectActions, multiple));
		return cp;
	}

	public static class WidgetList<T> extends Composite {
		private FlowPanel fp;

		public WidgetList(Collection<T> beans, Converter<T, Widget> converter,
				String emptyMessage) {
			this.fp = new FlowPanel();
			initWidget(fp);
			fp.setStyleName("alcina-widgetList");
			if (beans.isEmpty()) {
				Label l = new Label(emptyMessage);
				l.setStyleName("no-content");
				fp.add(l);
			} else {
				for (T t : beans) {
					fp.add(converter.convert(t));
				}
			}
		}
	}

	public ActionTableHolder createActionTable(Collection beans,
			Class beanClass, Converter converter,
			Collection<PermissibleAction> actions,
			PermissibleActionListener listener, boolean withObjectActions,
			boolean multiple) {
		ActionTableHolder holder = new ActionTableHolder();
		FlowPanel fp = holder.fp;
		if (converter != null) {
			beans = GwittirUtils.convertCollection(beans, converter);
		}
		Object bean = ClientReflector.get().getTemplateInstance(beanClass);
		BoundWidgetTypeFactory factory = new BoundWidgetTypeFactory(true);
		Field[] fields = GwittirBridge.get()
				.fieldsForReflectedObjectAndSetupWidgetFactory(bean, factory,
						false, true);
		int mask = BoundTableExt.HEADER_MASK | BoundTableExt.NO_NAV_ROW_MASK
				| BoundTableExt.SORT_MASK;
		if (withObjectActions) {
			mask |= BoundTableExt.ROW_HANDLE_MASK
					| BoundTableExt.HANDLES_AS_CHECKBOXES;
		}
		if (multiple) {
			mask |= BoundTableExt.MULTIROWSELECT_MASK;
		}
		CollectionDataProvider cp = new CollectionDataProvider(beans);
		cp.setPageSize(99999);
		NiceWidthBoundTable table = new NiceWidthBoundTable(mask, factory,
				fields, cp);
		table.addStyleName("results-table");
		if (actions != null && !actions.isEmpty()) {
			Toolbar tb = createToolbar(new ArrayList<PermissibleAction>(actions));
			tb.addVetoableActionListener(listener);
			fp.add(tb);
		}
		fp.add(table);
		holder.table = table;
		return holder;
	}

	public PaneWrapperWithObjects createBeanView(Object bean, boolean editable,
			PermissibleActionListener actionListener, boolean autoSave,
			boolean doNotClone) {
		return createBeanView(bean, editable, actionListener, autoSave,
				doNotClone, null, false);
	}

	public PaneWrapperWithObjects createBeanView(Object bean, boolean editable,
			PermissibleActionListener actionListener, boolean autoSave,
			boolean doNotClone, Object additionalProvisional) {
		return createBeanView(bean, editable, actionListener, autoSave,
				doNotClone, additionalProvisional, false);
	}

	public PaneWrapperWithObjects createBeanView(Object bean, boolean editable,
			PermissibleActionListener actionListener, boolean autoSave,
			boolean doNotClone, Object additionalProvisional,
			boolean doNotPrepare) {
		ClientBeanReflector bi = ClientReflector.get().beanInfoForClass(
				bean.getClass());
		Boolean overrideAutoSave= LooseContext.get(CONTEXT_OVERRIDE_AUTOSAVE);
		if(overrideAutoSave!=null){
			autoSave=overrideAutoSave.booleanValue();
		}
		boolean cloned = false;
		Collection supportingObjects = new ArrayList();
		if (!doNotClone
				&& !autoSave
				&& (!(bean instanceof HasIdAndLocalId) || CommonLocator.get()
						.objectLookup().getObject((HasIdAndLocalId) bean) != null)) {
			bean = new CloneHelper().shallowishBeanClone(bean);
			cloned = true;
		}
		if (bi == null) {
			throw new WrappedRuntimeException("Unviewable bean type: "
					+ bean.getClass(), SuggestedAction.NOTIFY_WARNING);
		}
		PaneWrapperWithObjects cp = createPaneWrapper(actionListener);
		cp.editable = editable;
		if (!noCaption) {
			cp.add(createCaption(bean, cp));
		}
		BoundWidgetTypeFactory factory = new BoundWidgetTypeFactory(true);
		Field[] fields = GwittirBridge.get()
				.fieldsForReflectedObjectAndSetupWidgetFactory(bean, factory,
						editable, false);
		GridForm f = new GridForm(fields, 1, factory);
		f.addAttachHandler(new RecheckVisibilityHandler(f));
		f.setAutofocusField(GwittirBridge.get().getFieldToFocus(bean, fields));
		f.setValue(bean);
		cp.add(f);
		cp.setBoundWidget(f);
		if (editable) {
			Validator beanValidator = GwittirBridge.get().getValidator(
					bean.getClass(), bean, null, null);
			if (autoSave || noButtons) {
				if (autoSave) {
					cp.propertyChangeBeanValidator = beanValidator;
				}
			} else {
				cp.setBeanValidator(beanValidator);
				ArrayList list = new ArrayList();
				list.add(bean);
				OkCancelPanel sp = new OkCancelPanel("Save", cp,
						isCancelButton());
				cp.add(sp);
				cp.setOkButton(sp.okButton);
				f.setFocusOnDetachIfEditorFocussed(sp.okButton);
				cp.setBean(bean);
				boolean provisional = cloned;
				if (bean instanceof HasIdAndLocalId) {
					HasIdAndLocalId hili = (HasIdAndLocalId) bean;
					provisional = provisional || (hili.getId() == 0);
				}
				Collection additional = CommonUtils
						.wrapInCollection(additionalProvisional != null ? additionalProvisional
								: LooseContext.getContext().get(
										CONTEXT_ADDITIONAL_PROVISIONAL_OBJECTS));
				cp.setProvisionalObjects(provisional || additional != null);
				cp.setInitialObjects(list);
				// this could be more elegant - need to register this before
				// "prepareforedit.."
				if (provisional) {
					TransformManager.get().registerProvisionalObject(list);
				}
				if (bean instanceof HasIdAndLocalId && !doNotPrepare) {
					supportingObjects = ClientTransformManager.cast()
							.prepareObject((HasIdAndLocalId) bean, autoSave,
									false, true);
				}
				if (additional != null) {
					supportingObjects.addAll(additional);
				}
				supportingObjects.addAll(list);
				cp.setObjects(supportingObjects);
			}
		}
		return cp;
	}

	public Widget createExtraActionsWidget(final Object bean) {
		ClientBeanReflector bi = ClientReflector.get().beanInfoForClass(
				bean.getClass());
		if (bi == null) {
			return null;
		}
		FlowPanel fp = null;
		ExpandableListPanel elp = null;
		for (Class<? extends PermissibleAction> c : bi.getActions(bean)) {
			final PermissibleAction v = CommonLocator.get().classLookup()
					.getTemplateInstance(c);
			if (v instanceof NonstandardObjectAction) {
				if (fp == null) {
					fp = new FlowPanel();
					fp.setStyleName("alcina-ObjectAction");
					fp.addStyleName("margin-top-15");
					fp.add(UsefulWidgetFactory.mediumTitleWidget(TextProvider
							.get().getUiObjectText(getClass(), "Extra actions",
									"Extra actions")));
					elp = new ExpandableListPanel("actions", 99);
					elp.setSeparator("\u00A0\u00A0\u00A0\u2022\u00A0\u00A0\u00A0");
					fp.add(elp);
				}
				final Link<PermissibleAction> link = new Link<PermissibleAction>();
				link.setUserObject(v);
				link.addClickHandler(new ClickHandler() {
					public void onClick(ClickEvent event) {
						DefaultPermissibleActionHandler.handleAction(
								(Widget) link, v, bean);
					}
				});
				link.setText(v.getDisplayName());
				elp.add(link);
			}
		}
		return fp;
	}

	public PaneWrapperWithObjects createMultipleBeanView(Collection beans,
			Class beanClass, boolean editable,
			PermissibleActionListener actionListener, boolean autoSave,
			boolean doNotClone) {
		return createMultipleBeanView(beans, beanClass, editable,
				actionListener, autoSave, doNotClone, 0);
	}

	public PaneWrapperWithObjects createMultipleBeanView(Collection beans,
			Class beanClass, boolean editable,
			PermissibleActionListener actionListener, boolean autoSave,
			boolean doNotClone, int tableMask) {
		ClientBeanReflector bi = ClientReflector.get().beanInfoForClass(
				beanClass);
		boolean cloned = false;
		if (!doNotClone && !autoSave && editable) {
			ArrayList beansCopy = new ArrayList();
			for (Object bean : beans) {
				beansCopy.add(new CloneHelper().shallowishBeanClone(bean));
			}
			beans = beansCopy;
			cloned = true;
		}
		if (bi == null) {
			throw new WrappedRuntimeException("Unviewable bean type: "
					+ beanClass, SuggestedAction.NOTIFY_WARNING);
		}
		Object bean = beans.iterator().hasNext() ? beans.iterator().next()
				: CommonLocator.get().classLookup()
						.getTemplateInstance(beanClass);
		PaneWrapperWithObjects cp = createPaneWrapper(actionListener);
		if (!noCaption) {
			cp.add(createMultiCaption(beanClass, cp));
		}
		BoundTableExt table = createTable(beans, editable, tableMask, bean);
		cp.add(table);
		cp.setBoundWidget(table);
		if (editable && !autoSave) {
			OkCancelPanel sp = new OkCancelPanel("Save", cp, isCancelButton());
			cp.add(sp);
			cp.setOkButton(sp.okButton);
			cp.setProvisionalObjects(cloned);
			cp.setObjects(beans);
		}
		return cp;
	}

	public BoundTableExt createTable(Collection beans, boolean editable,
			int tableMask, Object templateBean) {
		BoundWidgetTypeFactory factory = new BoundWidgetTypeFactory(true);
		Field[] fields = GwittirBridge.get()
				.fieldsForReflectedObjectAndSetupWidgetFactory(templateBean,
						factory, editable, true);
		int mask = tableMask | BoundTableExt.HEADER_MASK
				| BoundTableExt.SORT_MASK;
		CollectionDataProvider cdp = new CollectionDataProvider(beans);
		if ((mask & BoundTableExt.NO_NAV_ROW_MASK) != 0) {
			cdp.showAllObjectsInCollection();
		}
		BoundTableExt table = editable ? new BoundTableExt(mask, factory,
				fields, cdp) : new NiceWidthBoundTable(mask, factory, fields,
				cdp);
		return table;
	}

	public Toolbar createToolbar(List<PermissibleAction> actions) {
		Toolbar tb = new Toolbar();
		tb.setAsButton(true);
		tb.setActions(actions);
		tb.setStyleName("table-toolbar alcina-ToolbarSmall clearfix");
		return tb;
	}

	public boolean isCancelButton() {
		return cancelButton;
	}

	public boolean isNoButtons() {
		return this.noButtons;
	}

	public boolean isNoCaption() {
		return noCaption;
	}

	public void popupEdit(Object bean, String title,
			final PermissibleActionListener okListener) {
		FlowPanel fp = new FlowPanel();
		final DialogBox dialog = new GlassDialogBox();
		dialog.setText(title);
		dialog.add(fp);
		setNoCaption(true);
		setCancelButton(true);
		PaneWrapperWithObjects view = createBeanView(bean, true,
				new PermissibleActionListener() {
					public void vetoableAction(PermissibleActionEvent evt) {
						dialog.hide();
						if (evt.getAction().getClass() == ViewAction.class) {
							okListener.vetoableAction(evt);
						}
					}
				}, false, true);
		fp.add(view);
		dialog.center();
		dialog.show();
	}

	public void setCancelButton(boolean cancelButton) {
		this.cancelButton = cancelButton;
	}

	public void setNoButtons(boolean noButtons) {
		this.noButtons = noButtons;
	}

	public void setNoCaption(boolean noCaption) {
		this.noCaption = noCaption;
	}

	public void setNoCaptionsOrButtons(boolean noCaptionsOrButtons) {
		this.noCaption = noCaptionsOrButtons;
		this.noButtons = noCaptionsOrButtons;
	}

	private Widget createCaption(Object bean, PaneWrapperWithObjects cp) {
		ClientBeanReflector bi = ClientReflector.get().beanInfoForClass(
				bean.getClass());
		TextProvider.get().setTrimmed(true);
		List<SimpleHistoryEventInfo> history = Arrays
				.asList(new SimpleHistoryEventInfo[] {
						new SimpleHistoryEventInfo(objName()),
						new SimpleHistoryEventInfo(bi.getTypeDisplayName()),
						new SimpleHistoryEventInfo(bi.getObjectName(bean)) });
		TextProvider.get().setTrimmed(false);
		return new BreadcrumbBar(null, history, BreadcrumbBar.maxButton(cp));
	}

	private Widget createMultiCaption(Class beanClass, PaneWrapperWithObjects cp) {
		ClientBeanReflector bi = ClientReflector.get().beanInfoForClass(
				beanClass);
		List<SimpleHistoryEventInfo> history = Arrays
				.asList(new SimpleHistoryEventInfo[] {
						new SimpleHistoryEventInfo(objName()),
						new SimpleHistoryEventInfo(bi.getTypeDisplayName()) });
		return new BreadcrumbBar(null, history, BreadcrumbBar.maxButton(cp));
	}

	private PaneWrapperWithObjects createPaneWrapper(
			PermissibleActionListener actionListener) {
		PaneWrapperWithObjects vp = new PaneWrapperWithObjects();
		vp.setStyleName("alcina-BeanPanel");
		if (actionListener != null) {
			vp.addVetoableActionListener(actionListener);
		}
		return vp;
	}

	private String objName() {
		return TextProvider.get().getUiObjectText(getClass(),
				"caption-objects", "Objects");
	}

	public static class ActionTableHolder extends Composite {
		private FlowPanel fp;

		private NiceWidthBoundTable table;

		public ActionTableHolder() {
			this.fp = new FlowPanel();
			initWidget(fp);
		}

		public NiceWidthBoundTable getTable() {
			return this.table;
		}
	}

	public static class NiceWidthBoundTable extends FastROBoundTable {
		public static final double EM_WIDTH = 0.55;

		public NiceWidthBoundTable(int mask, BoundWidgetTypeFactory factory,
				Field[] fields, DataProvider provider) {
			super(mask | BoundTableExt.NO_SELECT_CELL_MASK
					| BoundTableExt.NO_SELECT_COL_MASK, factory, fields,
					provider);
		}

		@Override
		public void init(Collection c, int numberOfChunks) {
			super.init(c, numberOfChunks);
			beautify();
		}

		@Override
		public void setValue(Object value) {
			super.setValue(value);
		}

		@Override
		public void sortColumn(int index) {
			super.sortColumn(index);
			beautify();
		}

		@Override
		protected void beautify() {
			super.beautify();
			if (table.getRowCount() == 0) {
				return;
			}
			Field[] fields = getColumns();
			int i = 0;
			for (Field field : fields) {
				if (i == 0) {
					getCellFormatter().addStyleName(0, 0, "first");
				}
				BoundWidgetProvider provider = field.getCellProvider() != null ? field
						.getCellProvider() : factory.getWidgetProvider(
						field.getPropertyName(), null);
				if (provider instanceof HasMaxWidth) {
					HasMaxWidth hmw = (HasMaxWidth) provider;
					if (hmw.isForceColumnWidth() && hmw.getMaxWidth() != 0) {
						Element element = (Element) getCellFormatter()
								.getElement(0, i).getFirstChildElement();
						int widthEm = hmw.getMaxWidth() + 8;
						element.getStyle().setProperty("width",
								Math.round(widthEm * EM_WIDTH) + "em");
						element = (Element) getCellFormatter().getElement(0, i);
						element.getStyle().setProperty("width",
								Math.round(widthEm * EM_WIDTH) + "em");
					}
					int pct = hmw.getMinPercentOfTable();
					if (pct != 0) {
						int offsetWidth = table.getOffsetWidth();
						int pxWidth = pct * offsetWidth / 100;
						// IE won't like set-to-zero
						if (pxWidth != 0) {
							getCellFormatter().setWidth(0, i, pxWidth + "px");
						}
					}
					String strw = hmw.getColumnWidthString();
					if(strw!=null){
						getCellFormatter().setWidth(0, i, strw);
					}
				}
				i++;
				if (i >= table.getCellCount(0)) {
					break;
				}
			}
		}

		@Override
		/**
		 * handles beautification of incrementally rendered tables
		 */
		public void renderBottom() {
			super.renderBottom();
			beautify();
		}

		@Override
		protected void onAttach() {
			super.onAttach();
			beautify();
		}
	}

	public static class PaneWrapperWithObjects extends FlowPanel implements
			ClickHandler, PermissibleActionEvent.PermissibleActionSource {
		Validator propertyChangeBeanValidator;

		public boolean editable;

		private Validator beanValidator;

		private boolean alwaysDisallowOkIfInvalid;

		private Object bean;

		private HasBinding boundWidget;

		Collection objects;

		Collection initialObjects;

		private boolean provisionalObjects;

		PermissibleActionEvent.PermissibleActionSupport support = new PermissibleActionEvent.PermissibleActionSupport();

		private Button okButton;

		private FocusPanel preDetachFocus = new FocusPanel();

		private ActionTableHolder actionTableHolder;

		private FlowPanel propertyChangeValidatorResultPanel;

		private PropertyChangeListener validationListener = new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				String message = "";
				try {
					propertyChangeBeanValidator.validate(getModelSpce());
				} catch (ValidationException e) {
					message = e.getMessage();
				}
				propertyChangeValidatorResultPanel.clear();
				propertyChangeValidatorResultPanel.add(new HTML(message));
				propertyChangeValidatorResultPanel.setVisible(!message
						.isEmpty());
			}
		};

		public PaneWrapperWithObjects() {
			getElement().getStyle().setProperty("position", "relative");
			preDetachFocus.setVisible(false);
			add(preDetachFocus);
		}

		@Override
		protected void onLoad() {
			super.onLoad();
			if (propertyChangeBeanValidator != null) {
				propertyChangeValidatorResultPanel = new FlowPanel();
				propertyChangeValidatorResultPanel
						.setStyleName("property-change-validation-result");
				propertyChangeValidatorResultPanel.setVisible(false);
				add(propertyChangeValidatorResultPanel);
				getModelSpce().addPropertyChangeListener(validationListener);
			}
		}

		protected SourcesPropertyChangeEvents getModelSpce() {
			return (SourcesPropertyChangeEvents) ((AbstractBoundWidget) boundWidget)
					.getModel();
		}

		@Override
		protected void onUnload() {
			if (propertyChangeBeanValidator != null) {
				getModelSpce().removePropertyChangeListener(validationListener);
				remove(propertyChangeValidatorResultPanel);
			}
			super.onUnload();
		}

		public void addActionTable(ActionTableHolder actionTableHolder) {
			add(actionTableHolder);
			this.actionTableHolder = actionTableHolder;
		}

		public void addExtraActions(Widget w) {
			add(w);
		}

		public void addVetoableActionListener(PermissibleActionListener listener) {
			this.support.addVetoableActionListener(listener);
		}

		public void fireVetoableActionEvent(PermissibleActionEvent event) {
			this.support.fireVetoableActionEvent(event);
		}

		public HasBinding getBoundWidget() {
			return boundWidget;
		}

		public Collection getInitialObjects() {
			return this.initialObjects;
		}

		public Collection getObjects() {
			return this.objects;
		}

		public Button getOkButton() {
			return this.okButton;
		}

		public boolean isAlwaysDisallowOkIfInvalid() {
			return this.alwaysDisallowOkIfInvalid;
		}

		public boolean isProvisionalObjects() {
			return provisionalObjects;
		}

		public void onClick(ClickEvent clickEvent) {
			final Widget sender = (Widget) clickEvent.getSource();
			if (sender == okButton) {
				validateAndCommit(sender, null);
				return;
			} else {
				if (isProvisionalObjects()) {
					TransformManager.get()
							.deregisterProvisionalObjects(objects);
				}
				final PermissibleActionEvent action = new PermissibleActionEvent(
						initialObjects, CancelAction.INSTANCE);
				Scheduler.get().scheduleDeferred(new ScheduledCommand() {
					public void execute() {
						fireVetoableActionEvent(action);
					}
				});
			}
		}

		public void removeVetoableActionListener(
				PermissibleActionListener listener) {
			this.support.removeVetoableActionListener(listener);
		}

		public void setAlwaysDisallowOkIfInvalid(
				boolean alwaysDisallowOkIfInvalid) {
			this.alwaysDisallowOkIfInvalid = alwaysDisallowOkIfInvalid;
		}

		public void setBean(Object bean) {
			this.bean = bean;
		}

		public void setBeanValidator(Validator beanValidator) {
			this.beanValidator = beanValidator;
		}

		public void setBoundWidget(HasBinding boundWidget) {
			this.boundWidget = boundWidget;
		}

		public void setInitialObjects(Collection initialObjects) {
			this.initialObjects = initialObjects;
		}

		public void setObjects(Collection objects) {
			if (isProvisionalObjects()) {
				if (this.objects != null) {
					TransformManager.get()
							.deregisterProvisionalObjects(objects);
				}
				this.objects = objects;
				if (this.objects != null) {
					TransformManager.get().registerProvisionalObject(objects);
				}
			}
			if (this.initialObjects == null) {
				initialObjects = new ArrayList(objects);
			}
		}

		public void setProvisionalObjects(boolean promote) {
			this.provisionalObjects = promote;
		}

		public void setOkButton(Button okButton) {
			this.okButton = okButton;
		}

		public boolean validateBean() {
			if (beanValidator == null) {
				return true;
			}
			try {
				beanValidator.validate(bean);
				return true;
			} catch (ValidationException e) {
				ClientLayerLocator.get().notifications()
						.showWarning(e.getMessage());
				return false;
			}
		}

		private void commitChanges(boolean fireViewEvent) {
			if (isProvisionalObjects()) {
				TransformManager.get().promoteToDomainObject(objects);
				objects.clear();
			}
			if (!fireViewEvent) {
				return;
			}
			final PermissibleActionEvent action = new PermissibleActionEvent(
					initialObjects, ClientReflector.get().newInstance(
							ViewAction.class));
			Scheduler.get().scheduleDeferred(new ScheduledCommand() {
				public void execute() {
					fireVetoableActionEvent(action);
				}
			});
		}

		public boolean validateAndCommit(final Widget sender,
				final AsyncCallback<Void> serverValidationCallback) {
			if (!validateBean()) {
				return false;
			}
			ServerValidator.performingBeanValidation = true;
			boolean bindingValid = false;
			try {
				bindingValid = getBoundWidget().getBinding().validate();
			} finally {
				ServerValidator.performingBeanValidation = false;
			}
			List<Validator> validators = GwittirUtils.getAllValidators(
					getBoundWidget().getBinding(), null);
			if (!bindingValid) {
				for (Validator v : validators) {
					if (v instanceof ServerValidator) {
						ServerValidator sv = (ServerValidator) v;
						if (sv.isValidating()) {
							final CancellableRemoteDialog crd = new NonCancellableRemoteDialog(
									"Checking values", null);
							new Timer() {
								@Override
								public void run() {
									crd.hide();
									if (serverValidationCallback == null) {
										DomEvent.fireNativeEvent(
												WidgetUtils.createZeroClick(),
												sender);
									} else {
										validateAndCommit(sender,
												serverValidationCallback);
									}
								}
							}.schedule(500);
							return false;
						}
					}
				}
				if (PermissionsManager.get().isMemberOfGroup(
						PermissionsManager.getAdministratorGroupName())
						&& sender != null) {
					if (ClientLayerLocator.get().getGeneralProperties()
							.isAllowAdminInvalidObjectWrite()
							&& !alwaysDisallowOkIfInvalid) {
						ClientLayerLocator
								.get()
								.notifications()
								.confirm(
										"Administrative option: save the changed items "
												+ "on this form (even though some are invalid)?",
										new OkCallback() {
											public void ok() {
												commitChanges(true);
											}
										});
						return false;
					}
				}
				if (sender != null) {
					ClientLayerLocator
							.get()
							.notifications()
							.showWarning(
									"Please correct the problems in the form");
				} else {
				}
				return false;
			}// not valid
			if (serverValidationCallback != null) {
				for (Validator v : validators) {
					if (v instanceof ServerValidator) {
						serverValidationCallback.onSuccess(null);
						return true;
					}
				}
			}
			commitChanges(sender != null);
			return true;
		}

		@Override
		protected void onDetach() {
			preDetachFocus.setVisible(true);
			preDetachFocus.setFocus(true);
			if (editable && isVisible()) {
				GwittirUtils.refreshTextBoxes(getBoundWidget().getBinding(),
						null, false, false, true);
			}
			super.onDetach();// inter alia, detach children, forcing commit of
								// richtexts etc
			if (objects != null && TransformManager.get().dirty(objects)) {
				boolean save = Window
						.confirm("You are closing a form that"
								+ " has unsaved changes. Please press 'OK' to save the changes"
								+ ", or 'Cancel' to ignore them.");
				if (save) {
					boolean result = validateAndCommit(null, null);
					if (!result) {
						Window.alert("Unable to save changes due to form validation error.");
						TransformManager.get().deregisterProvisionalObjects(
								objects);
					}
				} else {
					TransformManager.get()
							.deregisterProvisionalObjects(objects);
				}
			}
		}

		public ActionTableHolder getActionTableHolder() {
			return this.actionTableHolder;
		}
	}

	public static class OkCancelPanel extends FlowPanel {
		private Button okButton;

		private Button cancelButton;

		public OkCancelPanel(String okButtonName, ClickHandler clickHandler,
				boolean hasCancel) {
			FlowPanel fp = this;
			fp.setStyleName("alcina-SavePanel");
			this.okButton = new Button(okButtonName);
			okButton.addClickHandler(clickHandler);
			fp.add(okButton);
			if (hasCancel) {
				fp.add(UsefulWidgetFactory.createSpacer(2));
				this.cancelButton = new Button("Cancel");
				cancelButton.addClickHandler(clickHandler);
				fp.add(cancelButton);
			}
		}

		public Button getCancelButton() {
			return this.cancelButton;
		}

		public Button getOkButton() {
			return this.okButton;
		}
	}
}
