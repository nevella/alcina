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
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.HasName;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.actions.PermissibleAction;
import cc.alcina.framework.common.client.actions.PermissibleActionEvent;
import cc.alcina.framework.common.client.actions.PermissibleActionListener;
import cc.alcina.framework.common.client.actions.instances.CreateAction;
import cc.alcina.framework.common.client.actions.instances.DeleteAction;
import cc.alcina.framework.common.client.actions.instances.EditAction;
import cc.alcina.framework.common.client.actions.instances.ViewAction;
import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationListener;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.permissions.AnnotatedPermissible;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.ClientBeanReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientReflector;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.gwt.client.dirndl.RenderContext;
import cc.alcina.framework.gwt.client.ide.node.ActionDisplayNode;
import cc.alcina.framework.gwt.client.ide.node.CollectionProviderNode;
import cc.alcina.framework.gwt.client.ide.node.ContainerNode;
import cc.alcina.framework.gwt.client.ide.node.DomainNode;
import cc.alcina.framework.gwt.client.ide.node.HasVisibleCollection;
import cc.alcina.framework.gwt.client.ide.node.NodeFactory;
import cc.alcina.framework.gwt.client.ide.node.UmbrellaProviderNode;
import cc.alcina.framework.gwt.client.ide.provider.SimpleCollectionProvider;
import cc.alcina.framework.gwt.client.ide.provider.UmbrellaCollectionProviderMultiplexer;
import cc.alcina.framework.gwt.client.ide.provider.UmbrellaProvider;
import cc.alcina.framework.gwt.client.ide.widget.Toolbar;
import cc.alcina.framework.gwt.client.logic.ExtraTreeEvent.ExtraTreeEventEvent;
import cc.alcina.framework.gwt.client.logic.ExtraTreeEvent.ExtraTreeEventListener;
import cc.alcina.framework.gwt.client.logic.ExtraTreeEvent.ExtraTreeEventType;
import cc.alcina.framework.gwt.client.stdlayout.image.StandardDataImages;
import cc.alcina.framework.gwt.client.util.WidgetUtils;
import cc.alcina.framework.gwt.client.widget.FilterWidget;
import cc.alcina.framework.gwt.client.widget.HasFirstFocusable;
import cc.alcina.framework.gwt.client.widget.layout.FlowPanel100pcHeight;
import cc.alcina.framework.gwt.client.widget.layout.HasLayoutInfo;
import cc.alcina.framework.gwt.client.widget.layout.ScrollPanel100pcHeight;

/**
 *
 * @author Nick Reddel
 */
public class WorkspaceView extends Composite implements HasName,
		PermissibleActionEvent.PermissibleActionSource, HasLayoutInfo {
	protected static final StandardDataImages images = GWT
			.create(StandardDataImages.class);

	public static final String DEBUG_ID_PREFIX = "WorkspaceView-";

	private String name;

	protected String id;

	private Widget widget;

	private PermissibleActionEvent.PermissibleActionSupport vetoableActionSupport = new PermissibleActionEvent.PermissibleActionSupport();

	public WorkspaceView() {
	}

	public WorkspaceView(Widget widget, String name) {
		this.widget = widget;
		this.name = name;
		initWidget(widget);
	}

	@Override
	public void addVetoableActionListener(PermissibleActionListener listener) {
		this.vetoableActionSupport.addVetoableActionListener(listener);
	}

	public void fireVetoableActionEvent(PermissibleActionEvent event) {
		this.vetoableActionSupport.fireVetoableActionEvent(event);
	}

	@Override
	public LayoutInfo getLayoutInfo() {
		return new LayoutInfo();
	}

	@Override
	public String getName() {
		return this.name;
	}

	public Widget getPageWidget() {
		return this.widget;
	}

	@Override
	public void
			removeVetoableActionListener(PermissibleActionListener listener) {
		this.vetoableActionSupport.removeVetoableActionListener(listener);
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	protected void onEnsureDebugId(String baseID) {
		ensureDebugId(getElement(), DEBUG_ID_PREFIX + (id == null ? name : id));
	}

	public static abstract class DataTreeView extends WorkspaceView
			implements ExtraTreeEventListener, PermissibleActionListener,
			HasLayoutInfo, SelectionHandler<TreeItem>, HasFirstFocusable {
		public static final String CONTEXT_IGNORE_TREE_SELECTION = DataTreeView.class
				.getName() + ".CONTEXT_IGNORE_TREE_SELECTION";

		public static final String CONTEXT_CLEAR_FILTER_IF_INVISIBLE = DataTreeView.class
				.getName() + ".CONTEXT_CLEAR_FILTER_IF_INVISIBLE";

		private boolean showCollapseButton;

		private DataTree dataTree;

		protected Toolbar toolbar;

		private FlowPanel fp;

		private ScrollPanel scroller;

		private boolean allowEditCollections = true;

		private FilterWidget filter;

		private Image collapse;

		protected boolean treeInitialised = false;

		private CollectionModificationListener listener;

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
				@Override
				public void onClick(ClickEvent event) {
					dataTree.collapseToFirstLevel();
				}
			});
			filter.getHolder().add(collapse);
			this.dataTree = createTree();
			dataTree.ensureDebugId(DataTree.DEBUG_ID);
			dataTree.addExtraTreeEventListener(this);
			filter.registerFilterable(dataTree);
			this.fp = new FlowPanel100pcHeight();
			fp.add(toolbar);
			Widget w = getPreFilterWidget();
			if (w != null) {
				fp.add(w);
			}
			fp.add(filter);
			w = getPostFilterWidget();
			if (w != null) {
				fp.add(w);
			}
			this.scroller = new ScrollPanel100pcHeight(dataTree);
			this.scroller.setStyleName("data-tree-view-scroller");
			fp.add(scroller);
			fp.setWidth("100%");
			dataTree.addSelectionHandler(this);
			initWidget(fp);
		}

		@Override
		public Focusable firstFocusable() {
			return filter.getTextBox();
		}

		public DataTree getDataTree() {
			return this.dataTree;
		}

		public FilterWidget getFilter() {
			return this.filter;
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

		@Override
		public void onExtraTreeEvent(ExtraTreeEventEvent evt) {
			List<Class<? extends PermissibleAction>> actions = getAvailableActions(
					evt.getSource());
			boolean canEdit = actions.contains(EditAction.class);
			if (actions.contains(ViewAction.class)
					&& evt.getType() == ExtraTreeEventType.DBL_CLICK) {
				Class<? extends PermissibleAction> actionClass = canEdit
						&& (allowEditCollections || evt.getSource()
								.getUserObject() instanceof Entity)
										? EditAction.class
										: ViewAction.class;
				vetoableAction(new PermissibleActionEvent(evt.getSource(),
						ClientReflector.get().newInstance(actionClass)));
			}
		}

		@Override
		public void onSelection(SelectionEvent<TreeItem> event) {
			if (LooseContext.getBoolean(CONTEXT_IGNORE_TREE_SELECTION)) {
				return;
			}
			TreeItem item = event.getSelectedItem();
			onTreeItemSelected(item);
		}

		public void resetTree() {
			try {
				this.scroller.remove(dataTree);
				getDataTree().removeItems();
				Object items = getTopLevelItems();
				if (items instanceof TreeItem) {
					TreeItem root = (TreeItem) items;
					if (showTopLevelNode()) {
						getDataTree().addItem(root);
						root.setState(true);
					} else {
						List<TreeItem> children = new ArrayList<>();
						for (int idx = 0; idx < root.getChildCount(); idx++) {
							children.add(root.getChild(idx));
						}
						children.forEach(getDataTree()::addItem);
						if (getDefaultEntityClass() != null
								&& listener == null) {
							listener = e -> {
								if (isAttached()) {
									Scheduler.get()
											.scheduleFinally(() -> resetTree());
								}
							};
							TransformManager.get()
									.addCollectionModificationListener(listener,
											getDefaultEntityClass());
						}
					}
				} else {
					Collection<TreeItem> roots = (Collection<TreeItem>) items;
					for (TreeItem root : roots) {
						getDataTree().addItem(root);
						root.setState(true);
					}
				}
			} finally {
				if (showTopLevelNode()
						|| expandFirstLevelNodesOnInitialRender()) {
					dataTree.collapseToFirstLevel();
				}
				// localdom opt - attach out of browser tree
				Panel parent = (Panel) this.scroller.getParent();
				if (parent != null) {
					this.scroller.removeFromParent();
					this.scroller.setWidget(dataTree);
					parent.add(this.scroller);
				} else {
					this.scroller.setWidget(dataTree);
				}
			}
		}

		public TreeItem selectNodeForObject(Object object) {
			return dataTree.selectNodeForObject(object);
		}

		public void setAllowEditCollections(boolean allowEditCollections) {
			this.allowEditCollections = allowEditCollections;
		}

		public void setFilter(FilterWidget filter) {
			this.filter = filter;
		}

		public void setShowCollapseButton(boolean showCollapseButton) {
			this.showCollapseButton = showCollapseButton;
			collapse.setVisible(showCollapseButton);
		}

		@Override
		public void setVisible(boolean visible) {
			super.setVisible(visible);
			if (!treeInitialised && visible && isAttached()) {
				treeInitialised = true;
				resetTree();
			}
			filter.getTextBox().setFocus(true);
		}

		@Override
		public void vetoableAction(PermissibleActionEvent evt) {
			TreeItem item = dataTree.getSelectedItem();
			if (evt.getAction().getClass() == DeleteAction.class) {
				onTreeItemSelected(item);
			}
			// hack - do better when we rework this for dirndl
			if (item == null) {
				item = new DomainNode(Reflections.classLookup()
						.newInstance(getDefaultEntityClass()));
			}
			fireVetoableActionEvent(
					new PermissibleActionEvent(item, evt.getAction()));
		}

		protected DataTree createTree() {
			return new DataTree(useCssTreeImages(), useNodeImages());
		}

		protected boolean expandFirstLevelNodesOnInitialRender() {
			return true;
		}

		protected List<Class<? extends PermissibleAction>>
				getAvailableActions(TreeItem item) {
			List<Class<? extends PermissibleAction>> actions = new ArrayList<Class<? extends PermissibleAction>>();
			Class domainClass = null;
			Object obj = null;
			Object parentObject = null;
			if (item instanceof DomainNode) {
				DomainNode dn = (DomainNode) item;
				SourcesPropertyChangeEvents userObject = dn.getUserObject();
				obj = userObject;
				domainClass = userObject.getClass();
				ClientBeanReflector info = ClientReflector.get()
						.beanInfoForClass(domainClass);
				actions.addAll(info.getActions(userObject));
			}
			if (item == null) {
				actions.addAll(getUnselectedActions());
			}
			if (item instanceof HasVisibleCollection) {
				if (!item.getState()) {
					item.setState(true, false);
					item.setState(false, false);
				}
				HasVisibleCollection hvc = (HasVisibleCollection) item;
				domainClass = hvc.getCollectionMemberClass();
				int size = hvc.getVisibleCollection().size();
				ClientBeanReflector info = ClientReflector.get()
						.beanInfoForClass(domainClass);
				List<Class<? extends PermissibleAction>> availableActions = info
						.getActions(null);
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
				obj = Reflections.classLookup().newInstance(domainClass);
				if (item.getParentItem() instanceof DomainNode) {
					parentObject = item.getParentItem().getUserObject();
				}
			}
			if (item instanceof ActionDisplayNode) {
				if (PermissionsManager.get().isPermissible(
						(PermissibleAction) item.getUserObject())) {
					actions.add(ViewAction.class);
				}
			}
			if (domainClass != null) {
				ObjectPermissions op = 
						
						PermissionsManager.getObjectPermissions(domainClass);
				try {
					LooseContext.pushWithKey(
							PermissionsManager.CONTEXT_CREATION_PARENT,
							parentObject);
					for (Iterator<Class<? extends PermissibleAction>> itr = actions
							.iterator(); itr.hasNext();) {
						Class<? extends PermissibleAction> actionClass = itr
								.next();
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
				} finally {
					LooseContext.pop();
				}
			}
			return actions;
		}

		protected <C> ContainerNode getBasicCollectionNode(String name,
				Class<C> clazz, ImageResource imageResource) {
			return getBasicCollectionNode(name, clazz, imageResource, null);
		}

		protected <C> ContainerNode getBasicCollectionNode(String name,
				Class<C> clazz, ImageResource imageResource,
				NodeFactory nodeFactory) {
			Collection domainCollection = TransformManager.get()
					.getCollection(clazz);
			SimpleCollectionProvider<C> provider = new SimpleCollectionProvider<C>(
					domainCollection, clazz);
			CollectionProviderNode node = new CollectionProviderNode(provider,
					name, imageResource, false, nodeFactory);
			TransformManager.get().addCollectionModificationListener(provider,
					clazz);
			return node;
		}

		protected <C> ContainerNode getFilteredCollectionNode(String name,
				Class<C> clazz, ImageResource imageResource,
				CollectionFilter cf) {
			return getFilteredCollectionNode(name, clazz, imageResource, cf,
					null);
		}

		protected <C> ContainerNode getFilteredCollectionNode(String name,
				Class<C> clazz, ImageResource imageResource,
				CollectionFilter cf, NodeFactory nodeFactory) {
			return getFilteredCollectionNode(name, clazz, imageResource, cf,
					nodeFactory, null);
		}

		protected <C> ContainerNode getFilteredCollectionNode(String name,
				Class<C> clazz, ImageResource imageResource,
				CollectionFilter cf, NodeFactory nodeFactory,
				Comparator<C> comparator) {
			Collection domainCollection = TransformManager.get()
					.getCollection(clazz);
			SimpleCollectionProvider<C> provider = new SimpleCollectionProvider<C>(
					domainCollection, clazz);
			provider.setComparator(comparator);
			// will fire change event
			provider.setFilter(cf);
			CollectionProviderNode node = new CollectionProviderNode(provider,
					name, imageResource, comparator != null, nodeFactory);
			TransformManager.get().addCollectionModificationListener(provider,
					clazz, true);
			return node;
		}

		protected Widget getPostFilterWidget() {
			return null;
		}

		protected Widget getPreFilterWidget() {
			return null;
		}

		protected abstract Object getTopLevelItems();

		protected <C> ContainerNode getUmbrellaProviderNode(String name,
				Class<C> clazz, ImageResource imageResource,
				UmbrellaProvider umbrellaProvider,
				CollectionFilter collectionFilter, NodeFactory nodeFactory) {
			Collection domainCollection = TransformManager.get()
					.getCollection(clazz);
			UmbrellaCollectionProviderMultiplexer<C> collectionProvider = new UmbrellaCollectionProviderMultiplexer<C>(
					domainCollection, clazz, umbrellaProvider, collectionFilter,
					3);
			UmbrellaProviderNode node = new UmbrellaProviderNode(
					collectionProvider.getRootSubprovider(), name,
					imageResource, nodeFactory);
			TransformManager.get().addCollectionModificationListener(
					collectionProvider, clazz, true);
			return node;
		}

		@Override
		protected void onLoad() {
			super.onLoad();
			Scheduler.get().scheduleDeferred(new ScheduledCommand() {
				@Override
				public void execute() {
					if (isVisible() && WidgetUtils.isVisibleAncestorChain(
							DataTreeView.this) && !treeInitialised) {
						treeInitialised = true;
						resetTree();
					}
				}
			});
			if (toolbar != null) {
				toolbar.processAvailableActions(getAvailableActions(null));
			}
		}

		protected void onTreeItemSelected(TreeItem item) {
			if (!item.isVisible()) {
				if (LooseContext.is(CONTEXT_CLEAR_FILTER_IF_INVISIBLE)) {
					filter.clear();
					filter.filter();
				}
				if (!item.isVisible()) {
					return;
				}
			}
			List<Class<? extends PermissibleAction>> actions = getAvailableActions(
					item);
			toolbar.processAvailableActions(actions);
			if (actions.contains(ViewAction.class)
					&& (item instanceof DomainNode
							|| item instanceof ActionDisplayNode)) {
				RenderContext.get().pushWithKey(
						RenderContext.CONTEXT_IGNORE_AUTOFOCUS,
						!(item.getUserObject() instanceof ForceAutofocus));
				fireVetoableActionEvent(new PermissibleActionEvent(item,
						ClientReflector.get().newInstance(ViewAction.class)));
				RenderContext.get().pop();
			}
		}

		protected boolean showTopLevelNode() {
			return true;
		}

		protected boolean useCssTreeImages() {
			return false;
		}

		protected boolean useNodeImages() {
			return false;
		}
	}

	protected Class<? extends Entity> getDefaultEntityClass() {
		return null;
	}

	protected Collection<? extends Class<? extends PermissibleAction>>
			getUnselectedActions() {
		return getDefaultEntityClass() == null ? Collections.emptyList()
				: Collections.singletonList(CreateAction.class);
	}
}
