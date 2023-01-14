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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;

import cc.alcina.framework.common.client.actions.PermissibleAction;
import cc.alcina.framework.common.client.actions.PermissibleActionEvent;
import cc.alcina.framework.common.client.actions.PermissibleActionListener;
import cc.alcina.framework.common.client.actions.instances.CancelAction;
import cc.alcina.framework.common.client.actions.instances.CloneAction;
import cc.alcina.framework.common.client.actions.instances.CreateAction;
import cc.alcina.framework.common.client.actions.instances.DeleteAction;
import cc.alcina.framework.common.client.actions.instances.EditAction;
import cc.alcina.framework.common.client.actions.instances.ViewAction;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.HasOrderValue;
import cc.alcina.framework.common.client.logic.domain.HasOrderValue.HasOrderValueHelper;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.reflection.Association;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.gwt.client.entity.GeneralProperties;
import cc.alcina.framework.gwt.client.ide.WorkspaceActionHandler.CloneActionHandler;
import cc.alcina.framework.gwt.client.ide.WorkspaceActionHandler.CreateActionHandler;
import cc.alcina.framework.gwt.client.ide.WorkspaceActionHandler.DeleteActionHandler;
import cc.alcina.framework.gwt.client.ide.WorkspaceActionHandler.EditActionHandler;
import cc.alcina.framework.gwt.client.ide.WorkspaceActionHandler.ViewActionHandler;
import cc.alcina.framework.gwt.client.ide.node.ActionDisplayNode;
import cc.alcina.framework.gwt.client.ide.node.DomainNode;
import cc.alcina.framework.gwt.client.ide.node.HasVisibleCollection;
import cc.alcina.framework.gwt.client.ide.node.ProvidesParenting;
import cc.alcina.framework.gwt.client.ide.provider.PropertyCollectionProvider;
import cc.alcina.framework.gwt.client.ide.provider.ViewProvider;
import cc.alcina.framework.gwt.client.widget.layout.HasLayoutInfo;

/**
 * Hooks up the various navigation views and the editor widget
 *
 * @author nick@alcina.cc
 *
 */
public class Workspace implements HasLayoutInfo, PermissibleActionListener,
		PermissibleActionEvent.PermissibleActionSource {
	private WSVisualModel visualModel;

	protected WorkspaceVisualiser visualiser;

	private PermissibleActionEvent.PermissibleActionSupport vetoableActionSupport = new PermissibleActionEvent.PermissibleActionSupport();

	private Map<Class<? extends PermissibleAction>, ViewProvider> viewProviderMap = new HashMap<Class<? extends PermissibleAction>, ViewProvider>();

	private PermissibleActionEvent lastEvent;

	public Workspace() {
		this.visualModel = new WSVisualModel();
	}

	@Override
	public void addVetoableActionListener(PermissibleActionListener listener) {
		this.vetoableActionSupport.addVetoableActionListener(listener);
	}

	public Widget createMultipleBeanView(Collection beans, Class beanClass,
			boolean editable, PermissibleActionListener actionListener,
			boolean autoSave, boolean doNotClone) {
		return getContentViewFactory().createMultipleBeanView(beans, beanClass,
				editable, actionListener, autoSave, false);
	}

	public void fireVetoableActionEvent(PermissibleActionEvent event) {
		this.vetoableActionSupport.fireVetoableActionEvent(event);
	}

	public void focusVisibleView() {
		visualiser.focusVisibleView();
	}

	public ContentViewFactory getContentViewFactory() {
		ContentViewFactory viewFactory = new ContentViewFactory();
		viewFactory.setCancelButton(true);
		return viewFactory;
	}

	public PermissibleActionEvent getLastEvent() {
		return this.lastEvent;
	}

	@Override
	public LayoutInfo getLayoutInfo() {
		if (visualiser == null) {
			return new LayoutInfo();
		} else {
			return visualiser.getLayoutInfo();
		}
	}

	public SourcesPropertyChangeEvents getParentDomainObject(Object node) {
		if (node instanceof DomainNode
				&& !(node instanceof ProvidesParenting)) {
			DomainNode dn = (DomainNode) node;
			node = dn.getParentItem();
		}
		if (node instanceof ProvidesParenting) {
			PropertyCollectionProvider pcp = ((ProvidesParenting) node)
					.getPropertyCollectionProvider();
			if (pcp != null) {
				return pcp.getDomainObject();
			}
		}
		return null;
	}

	public Widget getViewForAction(PermissibleAction action) {
		return viewProviderMap.get(action.getClass()).getViewForObject(action);
	}

	public WorkspaceVisualiser getVisualiser() {
		return this.visualiser;
	}

	public WSVisualModel getVisualModel() {
		return this.visualModel;
	}

	public void handleParentLinks(Object node, Entity newObj) {
		if (node instanceof DomainNode
				&& !(node instanceof ProvidesParenting)) {
			DomainNode dn = (DomainNode) node;
			node = dn.getParentItem();
		}
		if (node instanceof ProvidesParenting) {
			PropertyCollectionProvider pcp = ((ProvidesParenting) node)
					.getPropertyCollectionProvider();
			if (pcp != null) {
				String associationPropertyName = pcp.getProperty()
						.annotation(Association.class).propertyName();
				Reflections.at(newObj)
						.property(associationPropertyName)
						.set(newObj, pcp.getDomainObject());
			}
		}
		handleHasOrderValue(node, newObj);
	}

	public void redraw() {
		if (visualiser == null) {
			return;
		}
		visualiser.redraw();
	}

	public void registerViewProvider(ViewProvider v,
			Class<? extends PermissibleAction> actionClass) {
		viewProviderMap.put(actionClass, v);
	}

	public void removeAllListeners() {
		this.vetoableActionSupport.removeAllListeners();
	}

	@Override
	public void
			removeVetoableActionListener(PermissibleActionListener listener) {
		this.vetoableActionSupport.removeVetoableActionListener(listener);
	}

	public void setVisualModel(WSVisualModel visualModel) {
		this.visualModel = visualModel;
	}

	public void showView(WorkspaceView view) {
		visualiser.showView(view);
	}

	@Override
	public void vetoableAction(final PermissibleActionEvent evt) {
		lastEvent = evt;
		if (evt.getAction().getClass() == CancelAction.class) {
			visualiser.setContentWidget(new Label("Action cancelled"));
			fireVetoableActionEvent(evt);
			return;
		}
		Object obj = evt.getSource();
		Collection colln = null;
		Object singleObj = null;
		Class clazz = null;
		if (obj instanceof HasVisibleCollection) {
			colln = ((HasVisibleCollection) obj).getVisibleCollection();
			clazz = ((HasVisibleCollection) obj).getCollectionMemberClass();
		} else if (obj instanceof DomainNode) {
			singleObj = ((DomainNode) obj).getUserObject();
			clazz = singleObj.getClass();
		} else if (obj instanceof ActionDisplayNode) {
			singleObj = ((ActionDisplayNode) obj).getAction();
		} else if (obj != null && (obj instanceof Collection)) {
			Collection c = (Collection) obj;
			if (c.size() == 1) {
				singleObj = c.iterator().next();
				// quite possibly a provisional object
				// if (singleObj instanceof Entity) {
				// Entity entity = (Entity) singleObj;
				// singleObj = TransformManager.get().getObject(entity);
				// }
				// but provisionalobj.equals(domain)=true
				TreeItem item = visualiser.selectNodeForObject(singleObj, true);
				if (item != null) {
					clazz = singleObj.getClass();
				}
			}
		}
		if (colln != null && colln.size() == 1) {
			singleObj = colln.iterator().next();
		}
		boolean autoSave = GeneralProperties.get().isAutoSave();
		if (singleObj instanceof PermissibleAction) {
			Widget view = getViewForAction((PermissibleAction) singleObj);
			visualiser.setContentWidget(view);
			fireVetoableActionEvent(evt);
			return;
		}
		if (singleObj instanceof Entity) {
			Entity entity = (Entity) singleObj;
			if (!entity.domain().isNonDomain()) {
				singleObj = TransformManager.get().getObject(entity);
			}
		}
		Class<? extends WorkspaceActionHandler> handlerClass = null;
		if (singleObj != null) {
			if (evt.getAction().getClass() == ViewAction.class) {
				handlerClass = ViewActionHandler.class;
			} else if (evt.getAction().getClass() == EditAction.class) {
				handlerClass = EditActionHandler.class;
			} else if (evt.getAction().getClass() == DeleteAction.class) {
				handlerClass = DeleteActionHandler.class;
			} else if (evt.getAction().getClass() == CloneAction.class) {
				handlerClass = CloneActionHandler.class;
			} else if (evt.getAction().getClass() == CreateAction.class) {
				handlerClass = CreateActionHandler.class;
			}
		} else if (colln != null) {
			if (evt.getAction().getClass() == ViewAction.class) {
				handlerClass = ViewActionHandler.class;
			} else if (evt.getAction().getClass() == EditAction.class) {
				handlerClass = EditActionHandler.class;
			} else if (evt.getAction().getClass() == CreateAction.class) {
				handlerClass = CreateActionHandler.class;
			}
		}
		Class objectTargetClass = singleObj == null
				? clazz == null ? Object.class : clazz
				: singleObj.getClass();
		WorkspaceActionHandler handler = (WorkspaceActionHandler) Registry
				.query(WorkspaceActionHandler.class)
				.setKeys(handlerClass, objectTargetClass).impl();
		handler.performAction(evt, obj, singleObj != null ? singleObj : colln,
				this, clazz);
	}

	public void visualise(ComplexPanel container) {
		if (visualiser != null) {
			container.remove(visualiser);
		}
		this.visualiser = createVisualiser();
		container.add(visualiser);
	}

	protected WorkspaceVisualiser createVisualiser() {
		return new SimpleWorkspaceVisualiser(visualModel, this);
	}

	protected void handleHasOrderValue(Object node, Entity newObj) {
		if (node instanceof DomainNode
				&& !(node instanceof ProvidesParenting)) {
			DomainNode dn = (DomainNode) node;
			node = dn.getParentItem();
		}
		if (node instanceof ProvidesParenting
				&& newObj instanceof HasOrderValue) {
			Collection siblings = null;
			ProvidesParenting pp = (ProvidesParenting) node;
			PropertyCollectionProvider pcp = (PropertyCollectionProvider) pp
					.getPropertyCollectionProvider();
			if (pcp == null) {
				if (node instanceof HasVisibleCollection) {
					siblings = ((HasVisibleCollection) node)
							.getVisibleCollection();
				}
			} else {
				Object obj = pcp.getProperty().get(pcp.getDomainObject());
				if (obj instanceof Collection) {
					siblings = (Collection) obj;
				}
			}
			if (siblings != null) {
				int maxOrderValue = HasOrderValueHelper.maxValue(siblings,
						newObj);
				((HasOrderValue) newObj).setOrderValue(maxOrderValue + 10);
			}
		}
	}

	public static class WSVisualModel {
		private List<WorkspaceView> views = new ArrayList<WorkspaceView>();

		private String viewAreaClassName = "";

		private boolean toolbarVisible = true;

		private Widget contentWidget;

		private List<PermissibleAction> toolbarActions = new ArrayList<PermissibleAction>();

		public Widget getContentWidget() {
			return this.contentWidget;
		}

		public List<PermissibleAction> getToolbarActions() {
			return this.toolbarActions;
		}

		public String getViewAreaClassName() {
			return viewAreaClassName;
		}

		public List<WorkspaceView> getViews() {
			return this.views;
		}

		public boolean isToolbarVisible() {
			return this.toolbarVisible;
		}

		public void setContentWidget(Widget contentWidget) {
			this.contentWidget = contentWidget;
		}

		public void setToolbarActions(List<PermissibleAction> toolbarActions) {
			this.toolbarActions = toolbarActions;
		}

		public void setToolbarVisible(boolean toolbarVisible) {
			this.toolbarVisible = toolbarVisible;
		}

		public void setViewAreaClassName(String viewAreaClassName) {
			this.viewAreaClassName = viewAreaClassName;
		}

		public void setViews(List<WorkspaceView> views) {
			this.views = views;
		}
	}
}
