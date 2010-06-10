package cc.alcina.template.client.widgets;

import java.util.Arrays;
import java.util.List;

import cc.alcina.framework.common.client.actions.PermissibleAction;
import cc.alcina.framework.common.client.actions.PermissibleActionEvent;
import cc.alcina.framework.common.client.actions.instances.CreateAction;
import cc.alcina.framework.common.client.actions.instances.DeleteAction;
import cc.alcina.framework.common.client.actions.instances.EditAction;
import cc.alcina.framework.common.client.actions.instances.ViewAction;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager.CollectionModificationType;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.provider.TextProvider;
import cc.alcina.framework.gwt.client.ide.Workspace;
import cc.alcina.framework.gwt.client.ide.Workspace.WSVisualModel;
import cc.alcina.framework.gwt.client.ide.WorkspaceActionHandler.CreateActionHandler;
import cc.alcina.framework.gwt.client.ide.WorkspaceDefaultActionHandlers.DefaultCreateActionHandler;
import cc.alcina.framework.gwt.client.ide.WorkspaceView.DataTreeView;
import cc.alcina.framework.gwt.client.ide.node.CollectionProviderNode;
import cc.alcina.framework.gwt.client.ide.node.DomainCollectionProviderNode;
import cc.alcina.framework.gwt.client.ide.node.DomainNode;
import cc.alcina.framework.gwt.client.ide.node.NodeFactory.NodeCreator;
import cc.alcina.framework.gwt.client.ide.provider.CollectionFilter;
import cc.alcina.framework.gwt.client.ide.provider.DataImageProvider;
import cc.alcina.framework.gwt.client.logic.AlcinaHistory;
import cc.alcina.framework.gwt.client.stdlayout.TabDisplaysAsFullHeight;
import cc.alcina.framework.gwt.client.stdlayout.image.StandardDataImages;
import cc.alcina.framework.gwt.client.widget.BaseTab;
import cc.alcina.framework.gwt.client.widget.HeadingPanel;
import cc.alcina.framework.gwt.client.widget.Link;
import cc.alcina.framework.gwt.client.widget.layout.HasLayoutInfo;
import cc.alcina.template.cs.AlcinaTemplateHistory;
import cc.alcina.template.cs.persistent.AlcinaTemplateUser;
import cc.alcina.template.cs.persistent.Bookmark;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TreeItem;
import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;

public class BookmarksTab extends BaseTab implements HasLayoutInfo,
		TabDisplaysAsFullHeight {
	protected static StandardDataImages getImages() {
		return DataImageProvider.get().getDataImages();
	}

	protected Workspace workspace;

	private HorizontalPanel panel;

	public boolean initialisingTab;

	private DTBookmarks dtBookmarks;

	public BookmarksTab() {
		minimumAccessLevel = AccessLevel.EVERYONE;
		this.name = AlcinaTemplateHistory.BOOKMARKS_TAB;
		this.displayName = TextProvider.get().getUiObjectText(getClass(),
				TextProvider.DISPLAY_NAME,
				AlcinaHistory.get().getTokenDisplayName(this.name));
		panel = new HorizontalPanel();
		panel.setWidth("100%");
		this.scroller = new ScrollPanel(panel);
		scroller.setSize("100%", "100%");
		scroller.setStyleName("adminTab");
		initWidget(scroller);
	}

	@Override
	public String getHistoryToken() {
		return AlcinaTemplateHistory.BOOKMARKS_TAB;
	}

	public LayoutInfo getLayoutInfo() {
		return (workspace == null) ? new LayoutInfo() : workspace
				.getLayoutInfo();
	}

	@Override
	protected void ensureWidget() {
		initialisingTab = true;
		this.workspace = new Workspace();
		WSVisualModel visualModel = workspace.getVisualModel();
		visualModel.setToolbarVisible(false);
		visualModel.setViewAreaClassName("alcina-ContentLeft");
		initialiseViews(visualModel);
		initialiseContentArea(visualModel);
		workspace.visualise(panel);
		if (!visualModel.getViews().isEmpty()) {
			workspace.showView(visualModel.getViews().get(0));
		}
		workspace.redraw();
		initialisingTab = false;
	}

	protected void initialiseContentArea(WSVisualModel visualModel) {
		visualModel.setContentWidget(new BookmarksHome());
	}

	protected void initialiseViews(WSVisualModel visualModel) {
		PermissibleAction[] acar = { new ViewAction(), new CreateAction(),
				new EditAction(), new DeleteAction() };
		List<PermissibleAction> treeActions = Arrays.asList(acar);
		dtBookmarks = new DTBookmarks();
		dtBookmarks.getScroller().setAlwaysShowScrollBars(true);
		dtBookmarks.getToolbar().setActions(treeActions);
		visualModel.getViews().add(dtBookmarks);
	}

	public class BookmarksHome extends Composite {
		public BookmarksHome() {
			HorizontalPanel panel = new HorizontalPanel();
			FlowPanel p2 = new FlowPanel();
			p2.getElement().getStyle().setMarginLeft(100, Unit.PX);
			HeadingPanel h3 = new HeadingPanel(3);
			h3.add(new Label("Add some bookmarks"));
			p2.add(h3);
			p2.add(new Link("Click here", new ClickHandler() {
				public void onClick(ClickEvent event) {
					Bookmark folder = TransformManager.get()
							.createDomainObject(Bookmark.class);
					folder.setTitle("Worth a look");
					AlcinaTemplateUser user = (AlcinaTemplateUser) PermissionsManager
							.get().getUser();
					folder.setUser(user);
					Bookmark bookmark = TransformManager.get()
							.createDomainObject(Bookmark.class);
					bookmark.setUser(user);
					bookmark.setUrl("http://en.wikipedia.org/wiki/Alcina");
					bookmark
							.setTitle("Alcina - Wikipedia, the free encyclopedia");
					bookmark = TransformManager.get().createDomainObject(
							Bookmark.class);
					bookmark.setUser(user);
					bookmark
							.setUrl("http://video.google.com/videoplay?docid=-4779697496133297566#");
					bookmark.setTitle("History of oil");
					TransformManager.get().modifyCollectionProperty(folder,
							"children", bookmark,
							CollectionModificationType.ADD);
					bookmark = TransformManager.get().createDomainObject(
							Bookmark.class);
					bookmark.setUser(user);
					bookmark
							.setUrl("http://www.cs.umd.edu/~pugh/java/memoryModel/DoubleCheckedLocking.html");
					bookmark
							.setTitle("The 'Double-Checked Locking is Broken' Declaration");
					TransformManager.get().modifyCollectionProperty(folder,
							"children", bookmark,
							CollectionModificationType.ADD);
					bookmark = TransformManager.get().createDomainObject(
							Bookmark.class);
					bookmark.setUser(user);
					bookmark
							.setUrl("http://docs.jboss.org/ejb3/docs/tutorial/1.0.7/html/Caching_EJB3_Entities.html");
					bookmark.setTitle("Caching EJB3 Entities");
					TransformManager.get().modifyCollectionProperty(folder,
							"children", bookmark,
							CollectionModificationType.ADD);
					DeferredCommand.addCommand(new Command() {
						public void execute() {
							dtBookmarks.getDataTree().expandAll();
						}
					});
				}
			}));
			panel.add(p2);
			panel.setWidth("400px");
			initWidget(panel);
		}
	}

	@Override
	public AccessLevel accessLevel() {
		return AccessLevel.LOGGED_IN;
	}

	class DTBookmarks extends DataTreeView {
		public DTBookmarks() {
			super(TextProvider.get().getUiObjectText(BookmarksTab.class,
					"bookmarksTreeTabName", "Bookmarks"));
			setAllowEditCollections(false);
		}

		protected TreeItem getTopLevelItems() {
			CollectionFilter<Bookmark> cf = new CollectionFilter<Bookmark>() {
				public boolean allow(Bookmark o) {
					return o.getParent() == null;
				}
			};
			CollectionProviderNode cpn = getFilteredCollectionNode(TextProvider
					.get().getUiObjectText(BookmarksTab.class,
							"bookmarksTreeRootNode", "Bookmarks"),
					Bookmark.class, getImages().folder(), cf);
			return cpn;
		}
	}

	@RegistryLocation(j2seOnly = false, registryPoint = CreateActionHandler.class, targetClass = Bookmark.class)
	public static class BookmarkCreateActionHandler extends
			DefaultCreateActionHandler implements CreateActionHandler {
		public void performAction(PermissibleActionEvent event, Object node,
				Object object, Workspace workspace, Class nodeClass) {
			super.performAction(event, node, object, workspace, nodeClass);
			((Bookmark) newObj).setUser((AlcinaTemplateUser) PermissionsManager
					.get().getUser());
		}
	}

	@RegistryLocation(j2seOnly = false, registryPoint = NodeCreator.class, targetClass = Bookmark.class)
	@ClientInstantiable
	public static class BookmarkNodeCreator implements NodeCreator {
		@SuppressWarnings("unchecked")
		public DomainNode createDomainNode(
				SourcesPropertyChangeEvents domainObject) {
			return new BookmarkNode((Bookmark) domainObject);
		}
	}

	public static class BookmarkNode extends
			DomainCollectionProviderNode<Bookmark> {
		public BookmarkNode(Bookmark object) {
			super(object);
		}

		@Override
		protected String imageItemHTML(AbstractImagePrototype imageProto,
				String title) {
			ImageResource res = !getUserObject().getChildren().isEmpty() ? getImages()
					.folder()
					: getImages().file();
			return super.imageItemHTML(AbstractImagePrototype.create(res),
					title);
		}
	}
}
