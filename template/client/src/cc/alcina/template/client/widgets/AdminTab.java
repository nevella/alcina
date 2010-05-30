package cc.alcina.template.client.widgets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import cc.alcina.framework.common.client.actions.PermissibleAction;
import cc.alcina.framework.common.client.actions.instances.CreateAction;
import cc.alcina.framework.common.client.actions.instances.DeleteAction;
import cc.alcina.framework.common.client.actions.instances.EditAction;
import cc.alcina.framework.common.client.actions.instances.ViewAction;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationEvent;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.template.AlcinaTemplate;
import cc.alcina.framework.common.client.logic.template.AlcinaTemplateType;
import cc.alcina.framework.common.client.provider.TextProvider;
import cc.alcina.framework.gwt.client.ide.Workspace;
import cc.alcina.framework.gwt.client.ide.Workspace.WSVisualModel;
import cc.alcina.framework.gwt.client.ide.WorkspaceView.DataTreeView;
import cc.alcina.framework.gwt.client.ide.node.ActionDisplayNode;
import cc.alcina.framework.gwt.client.ide.node.ContainerNode;
import cc.alcina.framework.gwt.client.ide.provider.ActionViewProvider;
import cc.alcina.framework.gwt.client.ide.provider.SearchViewProvider;
import cc.alcina.framework.gwt.client.logic.StandardAsyncCallback;
import cc.alcina.framework.gwt.client.stdlayout.TabDisplaysAsFullHeight;
import cc.alcina.framework.gwt.client.stdlayout.image.StandardDataImages;
import cc.alcina.framework.gwt.client.widget.BaseTab;
import cc.alcina.framework.gwt.client.widget.dialog.CancellableRemoteDialog;
import cc.alcina.framework.gwt.client.widget.dialog.NonCancellableRemoteDialog;
import cc.alcina.framework.gwt.client.widget.layout.HasLayoutInfo;
import cc.alcina.framework.gwt.client.widget.layout.LayoutEvents;
import cc.alcina.framework.gwt.client.widget.layout.LayoutEvents.LayoutEvent;
import cc.alcina.framework.gwt.client.widget.layout.LayoutEvents.LayoutEventType;
import cc.alcina.template.client.AlcinaTemplateClient;
import cc.alcina.template.cs.actions.search.DomainTransformRecordSearchAction;
import cc.alcina.template.cs.constants.AlcinaTemplateAccessConstants;
import cc.alcina.template.cs.history.AlcinaTemplateHistoryTokens;
import cc.alcina.template.cs.persistent.AlcinaTemplateGroup;
import cc.alcina.template.cs.persistent.AlcinaTemplateUser;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TreeItem;

@AlcinaTemplate(AlcinaTemplateType.ALCINA_LAYOUT)
public class AdminTab extends BaseTab implements HasLayoutInfo,
		TabDisplaysAsFullHeight {
	protected static final StandardDataImages images = GWT
			.create(StandardDataImages.class);

	protected Workspace workspace;

	private HorizontalPanel panel;

	private DTUsers dtUsers;

	private DTGroups dtGroups;

	private boolean allUserInfoLoaded = false;

	private boolean loadingUserInfo = false;

	public boolean initialisingTab;

	public AdminTab() {
		minimumAccessLevel = AccessLevel.ADMIN;
		this.name = "Admin";
		panel = new HorizontalPanel();
		panel.setWidth("100%");
		this.scroller = new ScrollPanel(panel);
		scroller.setSize("100%", "100%");
		scroller.setStyleName("adminTab");
		initWidget(scroller);
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
		initialiseViewProviders();
		workspace.visualise(panel);
		if (!visualModel.getViews().isEmpty()) {
			workspace.showView(visualModel.getViews().get(0));
		}
		workspace.redraw();
		initialisingTab = false;
	}

	protected void initialiseContentArea(WSVisualModel visualModel) {
		visualModel.setContentWidget(new AdminIntro());
	}

	protected void initialiseViews(WSVisualModel visualModel) {
		PermissibleAction[] acar = { new ViewAction(), new CreateAction(),
				new EditAction(), new DeleteAction() };
		List<PermissibleAction> treeActions = Arrays.asList(acar);
		dtUsers = new DTUsers();
		dtGroups = new DTGroups();
		dtUsers.getToolbar().setActions(treeActions);
		dtGroups.getToolbar().setActions(treeActions);
		DataTreeView dtActionsEtc = new DTActionsEtc();
		visualModel.getViews().add(dtActionsEtc);
		visualModel.getViews().add(dtUsers);
		visualModel.getViews().add(dtGroups);
	}

	protected void initialiseViewProviders() {
		ActionViewProvider avp = new ActionViewProvider();
		// workspace.registerViewProvider(avp,
		// ArticleRenderPreparationAction.class);
		SearchViewProvider svp = new SearchViewProvider();
		workspace.registerViewProvider(svp,
				DomainTransformRecordSearchAction.class);
	}

	@Override
	public String getHistoryToken() {
		return AlcinaTemplateHistoryTokens.ADMIN_TAB;
	}

	public LayoutInfo getLayoutInfo() {
		return (workspace == null) ? new LayoutInfo() : workspace
				.getLayoutInfo();
	}

	protected ActionDisplayNode getNodeForAction(PermissibleAction action,
			ImageResource imageResource) {
		ActionDisplayNode node = new ActionDisplayNode(action.getDisplayName(),
				imageResource);
		node.setUserObject(action);
		return node;
	}

	private void loadAllUsersAndGroups() {
		if (allUserInfoLoaded || loadingUserInfo || initialisingTab) {
			return;
		}
		loadingUserInfo = true;
		final CancellableRemoteDialog crd = new NonCancellableRemoteDialog(
				"Loading all users and groups...", null);
		AsyncCallback<List<AlcinaTemplateGroup>> callback = new StandardAsyncCallback<List<AlcinaTemplateGroup>>() {
			@Override
			public void onFailure(Throwable caught) {
				crd.hide();
				loadingUserInfo = false;
				super.onFailure(caught);
			}

			public void onSuccess(List<AlcinaTemplateGroup> result) {
				TransformManager.get().getDomainObjects().registerObjects(
						result);
				for (AlcinaTemplateGroup jg : result) {
					TransformManager.get().getDomainObjects().registerObjects(
							jg.getMemberUsers());
				}
				Collection<AlcinaTemplateUser> c = TransformManager.get()
						.getCollection(AlcinaTemplateUser.class);
				ArrayList<AlcinaTemplateUser> cl = new ArrayList<AlcinaTemplateUser>(
						c);
				Collections.sort(cl);
				CollectionModificationEvent event = new CollectionModificationEvent(
						this, AlcinaTemplateUser.class, null);
				TransformManager.get().fireCollectionModificationEvent(event);
				event = new CollectionModificationEvent(this,
						AlcinaTemplateGroup.class, null);
				TransformManager.get().fireCollectionModificationEvent(event);
				crd.hide();
				LayoutEvents.get().fireLayoutEvent(
						new LayoutEvent(
								LayoutEventType.REQUIRES_GLOBAL_RELAYOUT));
				workspace.showView(dtUsers.isVisible() ? dtUsers : dtGroups);
				loadingUserInfo = false;
				allUserInfoLoaded = true;
			}
		};
		LayoutEvents.get().fireLayoutEvent(
				new LayoutEvent(LayoutEventType.REQUIRES_GLOBAL_RELAYOUT));
		crd.show();
		AlcinaTemplateClient.theApp.getAppRemoteService()
				.getAllGroups(callback);
	}

	@Override
	public String rule() {
		return AlcinaTemplateAccessConstants.ADMINISTRATORS_GROUP_NAME;
	}

	class DTGroups extends DataTreeView {
		public DTGroups() {
			super("Groups");
			setAllowEditCollections(true);
		}

		protected TreeItem getTopLevelItems() {
			return getBasicCollectionNode("Groups", AlcinaTemplateGroup.class,
					images.folder());
		}

		@Override
		public void setVisible(boolean visible) {
			super.setVisible(visible);
			if (!initialisingTab) {
				loadAllUsersAndGroups();
			}
		}
	}

	class DTUsers extends DataTreeView {
		public DTUsers() {
			super("Users");
			setAllowEditCollections(false);
		}

		protected TreeItem getTopLevelItems() {
			return getBasicCollectionNode("Users", AlcinaTemplateUser.class,
					images.folder());
		}

		@Override
		public void setVisible(boolean visible) {
			super.setVisible(visible);
			loadAllUsersAndGroups();
		}
	}

	class DTActionsEtc extends DataTreeView {
		public DTActionsEtc() {
			super("Actions &c & c");
			getToolbar().setVisible(false);
		}

		protected TreeItem getTopLevelItems() {
			ContainerNode root = new ContainerNode("All", images.folder());
			ContainerNode actions = new ContainerNode("Actions", images
					.folder());
			root.addItem(actions);
			ContainerNode search = new ContainerNode(TextProvider.get()
					.getUiObjectText(AdminTab.class, "actionsSectionSearch",
							"Search"), images.folder());
			root.addItem(search);
			search.addItem(getNodeForAction(
					new DomainTransformRecordSearchAction(), images.file()));
			return root;
		}
		@Override
		public void resetTree() {
			super.resetTree();
			getDataTree().expandAll();
		}
	}
}
