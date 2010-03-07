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
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.actions.VetoableAction;
import cc.alcina.framework.common.client.actions.VetoableActionEvent;
import cc.alcina.framework.common.client.actions.VetoableActionExtra.VetoableActionListener;
import cc.alcina.framework.common.client.actions.VetoableActionExtra.VetoableActionSource;
import cc.alcina.framework.common.client.actions.VetoableActionExtra.VetoableActionSupport;
import cc.alcina.framework.common.client.actions.instances.CreateAction;
import cc.alcina.framework.common.client.actions.instances.DeleteAction;
import cc.alcina.framework.common.client.actions.instances.EditAction;
import cc.alcina.framework.common.client.actions.instances.ViewAction;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.permissions.AnnotatedPermissible;
import cc.alcina.framework.common.client.logic.permissions.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.ClientBeanReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientReflector;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.gwt.client.ide.node.ActionDisplayNode;
import cc.alcina.framework.gwt.client.ide.node.CollectionProviderNode;
import cc.alcina.framework.gwt.client.ide.node.DomainNode;
import cc.alcina.framework.gwt.client.ide.provider.CollectionFilter;
import cc.alcina.framework.gwt.client.ide.provider.SimpleCollectionProvider;
import cc.alcina.framework.gwt.client.ide.widget.DataTree;
import cc.alcina.framework.gwt.client.ide.widget.Toolbar;
import cc.alcina.framework.gwt.client.logic.ExtraTreeEvent.ETEType;
import cc.alcina.framework.gwt.client.logic.ExtraTreeEvent.ExtraTreeEventEvent;
import cc.alcina.framework.gwt.client.logic.ExtraTreeEvent.ExtraTreeEventListener;
import cc.alcina.framework.gwt.client.stdlayout.image.StandardDataImages;
import cc.alcina.framework.gwt.client.widget.FilterWidget;
import cc.alcina.framework.gwt.client.widget.layout.FlowPanel100pcHeight;
import cc.alcina.framework.gwt.client.widget.layout.HasLayoutInfo;
import cc.alcina.framework.gwt.client.widget.layout.ScrollPanel100pcHeight;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasName;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.beans.Bindable;

/**
 *
 * @author <a href="mailto:nick@alcina.cc">Nick Reddel</a>
 */

 public class WorkspaceView extends Composite implements HasName,
		VetoableActionSource, HasLayoutInfo {
	protected static final StandardDataImages images = GWT
			.create(StandardDataImages.class);

	public static final String DEBUG_ID_PREFIX = "WorkspaceView-";

	private String name;

	protected String id;

	private Widget widget;

	private VetoableActionSupport vetoableActionSupport = new VetoableActionSupport();

	public WorkspaceView() {
	}

	public WorkspaceView(Widget widget, String name) {
		this.widget = widget;
		this.name = name;
		initWidget(widget);
	}

	public void addVetoableActionListener(VetoableActionListener listener) {
		this.vetoableActionSupport.addVetoableActionListener(listener);
	}

	public void fireVetoableActionEvent(VetoableActionEvent event) {
		this.vetoableActionSupport.fireVetoableActionEvent(event);
	}

	public LayoutInfo getLayoutInfo() {
		return new LayoutInfo();
	}

	public String getName() {
		return this.name;
	}

	public Widget getWidget() {
		return this.widget;
	}

	public void removeVetoableActionListener(VetoableActionListener listener) {
		this.vetoableActionSupport.removeVetoableActionListener(listener);
	}

	public void setName(String name) {
		this.name = name;
	}

	protected void onEnsureDebugId(String baseID) {
		ensureDebugId(getElement(), DEBUG_ID_PREFIX + (id == null ? name : id));
	}

	public static abstract class DataTreeView extends WorkspaceView implements
			ExtraTreeEventListener, VetoableActionListener, HasLayoutInfo,
			SelectionHandler<TreeItem> {
		private boolean showCollapseButton;

		private DataTree dataTree;

		private Toolbar toolbar;

		private FlowPanel fp;

		private ScrollPanel scroller;

		private boolean allowEditCollections = true;

		private FilterWidget filter;

		private Image collapse;

		public DataTreeView(String name) {
			this(name, null);
		}

		public DataTreeView(String name, String debugId) {
			setName(name);
			this.toolbar = new Toolbar();
			toolbar.addVetoableActionListener(this);
			filter = new FilterWidget();
			this.collapse = AbstractImagePrototype.create(images.collapse())
					.createImage();
			collapse.setVisible(false);
			collapse.setStyleName("alcina-Filter-collapse");
			collapse.addClickHandler(new ClickHandler() {
				public void onClick(ClickEvent event) {
					dataTree.collapseToFirstLevel();
				}
			});
			filter.getHolder().add(collapse);
			this.dataTree = new DataTree();
			dataTree.ensureDebugId(DataTree.DEBUG_ID);
			dataTree.addExtraTreeEventListener(this);
			filter.registerFilterable(dataTree);
			this.fp = new FlowPanel100pcHeight();
			fp.add(toolbar);
			fp.add(filter);
			this.scroller = new ScrollPanel100pcHeight(dataTree);
			fp.add(scroller);
			fp.setWidth("100%");
			dataTree.addSelectionHandler(this);
			resetTree();
			initWidget(fp);
		}

		public DataTree getDataTree() {
			return this.dataTree;
		}

		@Override
		public LayoutInfo getLayoutInfo() {
			return new LayoutInfo() {
				@Override
				public int getClientAdjustHeight() {
					return 0;// 2
				}

				@Override
				public Iterator<Widget> getLayoutWidgets() {
					return Arrays.asList(new Widget[] { fp }).iterator();
				}

				@Override
				public boolean to100percentOfAvailableHeight() {
					return true;
				}
			};
		}

		public ScrollPanel getScroller() {
			return this.scroller;
		}

		public Toolbar getToolbar() {
			return this.toolbar;
		}

		public boolean isAllowEditCollections() {
			return allowEditCollections;
		}

		public boolean isShowCollapseButton() {
			return showCollapseButton;
		}

		public void onExtraTreeEvent(ExtraTreeEventEvent evt) {
			List<Class<? extends VetoableAction>> actions = getAvailableActions(evt
					.getSource());
			boolean canEdit = actions.contains(EditAction.class);
			if (actions.contains(ViewAction.class)
					&& evt.getType() == ETEType.DBL_CLICK) {
				Class<? extends VetoableAction> actionClass = canEdit
						&& (allowEditCollections || evt.getSource()
								.getUserObject() instanceof HasIdAndLocalId) ? EditAction.class
						: ViewAction.class;
				vetoableAction(new VetoableActionEvent(evt.getSource(),
						ClientReflector.get().newInstance(actionClass)));
			}
		}

		public void onSelection(SelectionEvent<TreeItem> event) {
			TreeItem item = event.getSelectedItem();
			onTreeItemSelected(item);
		}

		private void onTreeItemSelected(TreeItem item) {
			if (!item.isVisible()) {
				return;
			}
			List<Class<? extends VetoableAction>> actions = getAvailableActions(item);
			toolbar.processAvailableActions(actions);
			if (actions.contains(ViewAction.class)
					&& (item instanceof DomainNode || item instanceof ActionDisplayNode)) {
				fireVetoableActionEvent(new VetoableActionEvent(item,
						ClientReflector.get().newInstance(ViewAction.class)));
			}
		}

		@SuppressWarnings("unchecked")
		public void resetTree() {
			getDataTree().removeItems();
			Object items = getTopLevelItems();
			if (items instanceof TreeItem) {
				TreeItem root = (TreeItem) items;
				getDataTree().addItem(root);
				root.setState(true);
			} else {
				Collection<TreeItem> roots = (Collection<TreeItem>) items;
				for (TreeItem root : roots) {
					getDataTree().addItem(root);
					root.setState(true);
				}
			}
		}

		public TreeItem selectNodeForObject(Object object) {
			return dataTree.selectNodeForObject(object);
		}

		public void setAllowEditCollections(boolean allowEditCollections) {
			this.allowEditCollections = allowEditCollections;
		}

		public void setShowCollapseButton(boolean showCollapseButton) {
			this.showCollapseButton = showCollapseButton;
			collapse.setVisible(showCollapseButton);
		}

		@Override
		public void setVisible(boolean visible) {
			super.setVisible(visible);
			filter.getTextBox().setFocus(true);
		}

		public void vetoableAction(VetoableActionEvent evt) {
			TreeItem item = dataTree.getSelectedItem();
			fireVetoableActionEvent(new VetoableActionEvent(item, evt
					.getAction()));
			if (evt.getAction().getClass() == DeleteAction.class) {
				onTreeItemSelected(item);
			}
		}

		protected List<Class<? extends VetoableAction>> getAvailableActions(
				TreeItem item) {
			List<Class<? extends VetoableAction>> actions = new ArrayList<Class<? extends VetoableAction>>();
			Class domainClass = null;
			Object obj = null;
			if (item instanceof DomainNode) {
				DomainNode dn = (DomainNode) item;
				Bindable userObject = dn.getUserObject();
				obj = userObject;
				domainClass = userObject.getClass();
				ClientBeanReflector info = ClientReflector.get()
						.beanInfoForClass(domainClass);
				actions.addAll(info.getActions());
			}
			if (item instanceof CollectionProviderNode) {
				CollectionProviderNode cpn = (CollectionProviderNode) item;
				domainClass = cpn.getCollectionProvider().getCollectionClass();
				int size = cpn.getCollectionProvider().getCollection().size();
				ClientBeanReflector info = ClientReflector.get()
						.beanInfoForClass(domainClass);
				List<Class<? extends VetoableAction>> availableActions = info
						.getActions();
				if (availableActions.contains(CreateAction.class)) {
					actions.add(CreateAction.class);
				}
				if (availableActions.contains(ViewAction.class) && size != 0) {
					actions.add(ViewAction.class);
				}
				if (availableActions.contains(EditAction.class) && size != 0
						&& isAllowEditCollections()) {
					actions.add(EditAction.class);
				}
			}
			if (item instanceof ActionDisplayNode) {
				if (PermissionsManager.get().isPermissible(
						(VetoableAction) item.getUserObject())) {
					actions.add(ViewAction.class);
				}
			}
			if (domainClass != null) {
				ObjectPermissions op = CommonLocator.get().classLookup()
						.getAnnotationForClass(domainClass,
								ObjectPermissions.class);
				op = op == null ? PermissionsManager.get()
						.getDefaultObjectPermissions() : op;
				for (Iterator<Class<? extends VetoableAction>> itr = actions
						.iterator(); itr.hasNext();) {
					Class<? extends VetoableAction> actionClass = itr.next();
					Permission p = null;
					if (actionClass == CreateAction.class) {
						p = op.create();
					}
					if (actionClass == EditAction.class) {
						p = op.write();
					}
					if (actionClass == ViewAction.class) {
						p = op.read();
					}
					if (actionClass == DeleteAction.class) {
						p = op.delete();
					}
					if (p != null) {
						if (!PermissionsManager.get().isPermissible(obj,
								new AnnotatedPermissible(p))) {
							itr.remove();
						}
					}
				}
			}
			return actions;
		}

		@SuppressWarnings("unchecked")
		protected <C> CollectionProviderNode getBasicCollectionNode(
				String name, Class<C> clazz, ImageResource imageResource) {
			Collection domainCollection = TransformManager.get()
					.getDomainObjects().getCollection(clazz);
			SimpleCollectionProvider<C> provider = new SimpleCollectionProvider<C>(
					domainCollection, clazz);
			CollectionProviderNode node = new CollectionProviderNode(provider,
					name, imageResource);
			TransformManager.get().addCollectionModificationListener(provider,
					clazz);
			return node;
		}

		@SuppressWarnings("unchecked")
		protected <C> CollectionProviderNode getFilteredCollectionNode(
				String name, Class<C> clazz, ImageResource imageResource,
				CollectionFilter cf) {
			Collection domainCollection = TransformManager.get()
					.getDomainObjects().getCollection(clazz);
			SimpleCollectionProvider<C> provider = new SimpleCollectionProvider<C>(
					domainCollection, clazz);
			provider.setFilter(cf);
			CollectionProviderNode node = new CollectionProviderNode(provider,
					name, imageResource);
			TransformManager.get().addCollectionModificationListener(provider,
					clazz, true);
			return node;
		}

		protected abstract Object getTopLevelItems();
	}
}
