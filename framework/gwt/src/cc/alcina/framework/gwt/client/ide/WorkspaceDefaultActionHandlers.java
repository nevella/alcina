package cc.alcina.framework.gwt.client.ide;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.actions.PermissibleActionEvent;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.ClientTransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationSupport;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.permissions.IVersionableOwnable;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.ClientReflector;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.provider.TextProvider;
import cc.alcina.framework.common.client.util.DomainObjectCloner;
import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.client.ide.ContentViewFactory.PaneWrapperWithObjects;
import cc.alcina.framework.gwt.client.ide.WorkspaceActionHandler.CloneActionHandler;
import cc.alcina.framework.gwt.client.ide.WorkspaceActionHandler.CreateActionHandler;
import cc.alcina.framework.gwt.client.ide.WorkspaceActionHandler.DeleteActionHandler;
import cc.alcina.framework.gwt.client.ide.WorkspaceActionHandler.EditActionHandler;
import cc.alcina.framework.gwt.client.ide.WorkspaceActionHandler.ViewActionHandler;
import cc.alcina.framework.gwt.client.logic.OkCallback;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;

public class WorkspaceDefaultActionHandlers {
	@ClientInstantiable
	public abstract static class WorkspaceDefaultActionHandlerBase {
		protected ContentViewFactory getContentViewFactory() {
			ContentViewFactory viewFactory = new ContentViewFactory();
			viewFactory.setCancelButton(true);
			return viewFactory;
		}

		protected boolean isAutoSave() {
			return ClientLayerLocator.get().getGeneralProperties().isAutoSave();
		}

		protected void handleParentLinks(Workspace workspace, Object node,
				HasIdAndLocalId newObj) {
			workspace.handleParentLinks(node, newObj);
		}
	}

	@RegistryLocation( registryPoint = ViewActionHandler.class)
	public static class DefaultViewActionHandler extends
			WorkspaceDefaultActionHandlerBase implements ViewActionHandler {
		public void performAction(PermissibleActionEvent event, Object node,
				Object object, Workspace workspace, Class nodeObjectClass) {
			Widget view = null;
			if (object instanceof Collection) {
				view = workspace.createMultipleBeanView((Collection) object,
						nodeObjectClass, editView(), workspace, isAutoSave(),
						false);
			} else {
				PaneWrapperWithObjects paneWrapper = getContentViewFactory()
						.createBeanView(object, editView(), workspace,
								isAutoSave(), false);
				view = paneWrapper;
				Widget widge = getContentViewFactory()
						.createExtraActionsWidget(object);
				if (widge != null) {
					paneWrapper.add(widge);
				}
			}
			workspace.getVisualiser().setContentWidget(view);
			workspace.fireVetoableActionEvent(event);
		}

		protected boolean editView() {
			return false;
		}
	}

	@RegistryLocation( registryPoint = CreateActionHandler.class)
	public static class DefaultCreateActionHandler extends
			WorkspaceDefaultActionHandlerBase implements CreateActionHandler {
		protected HasIdAndLocalId newObj;

		public void performAction(PermissibleActionEvent event, Object node,
				Object object, Workspace workspace, Class nodeObjectClass) {
			try {
				CollectionModificationSupport.queue(true);
				newObj = isAutoSave() ? TransformManager.get()
						.createDomainObject(nodeObjectClass) : TransformManager
						.get().createProvisionalObject(nodeObjectClass);
				handleParentLinks(workspace, node, newObj);
				ClientTransformManager.cast().prepareObject(newObj,
						isAutoSave(), true, false);
				TextProvider.get().setDecorated(false);
				String tdn = ClientReflector.get()
						.beanInfoForClass(nodeObjectClass).getTypeDisplayName();
				TextProvider.get().setDecorated(true);
				TextProvider.get().setObjectName(newObj, "New " + tdn);
				if (newObj instanceof IVersionableOwnable) {
					((IVersionableOwnable) newObj).setOwner(PermissionsManager
							.get().getUser());
				}
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			} finally {
				CollectionModificationSupport.queue(false);
			}
			workspace.getVisualiser().selectNodeForObject(newObj, true);
			PaneWrapperWithObjects view = getContentViewFactory()
					.createBeanView(newObj, true, workspace, isAutoSave(),
							false);
			workspace.getVisualiser().setContentWidget(view);
			workspace.fireVetoableActionEvent(new PermissibleActionEvent(
					newObj, event.getAction()));
		}
	}

	@RegistryLocation( registryPoint = EditActionHandler.class)
	public static class DefaultEditActionHandler extends
			DefaultViewActionHandler implements ViewActionHandler {
		@Override
		protected boolean editView() {
			return true;
		}
	}

	@RegistryLocation( registryPoint = DeleteActionHandler.class)
	/*
	 * TODO - Alcina - the separation of 'deletion of reffing' and 'deletion'
	 * into two transactions was caused by weird EJB3 behaviour - it works, and
	 * works offline->online (for 'fromofflinepersistence') -- but there's
	 * something dodgy going on with Hibernate PersistentSets if they're
	 * combined in the one transaction and problem needs to be fixd
	 */
	public static class DefaultDeleteActionHandler extends
			WorkspaceDefaultActionHandlerBase implements DeleteActionHandler {
		private class DoDeleteCallback implements OkCallback {
			private final PermissibleActionEvent event;

			private final Workspace workspace;

			private final HasIdAndLocalId hili;

			private final WorkspaceDeletionChecker workspaceDeletionChecker;

			private DoDeleteCallback(PermissibleActionEvent event,
					Workspace workspace, HasIdAndLocalId hili,
					WorkspaceDeletionChecker workspaceDeletionChecker) {
				this.event = event;
				this.workspace = workspace;
				this.hili = hili;
				this.workspaceDeletionChecker = workspaceDeletionChecker;
			}

			public void ok() {
				AsyncCallback deleteObjectCallback = new AsyncCallback<Void>() {
					@Override
					public void onFailure(Throwable caught) {
						finish();
					}

					@Override
					public void onSuccess(Void result) {
						finish();
					}

					private void finish() {
						TransformManager.get().deleteObject(hili);
						if (workspace != null) {
							workspace.getVisualiser().setContentWidget(
									new HorizontalPanel());
							workspace
									.fireVetoableActionEvent(new PermissibleActionEvent(
											hili, event.getAction()));
						}
					}
				};
				if (!this.workspaceDeletionChecker.cascadedDeletions.isEmpty()) {
					for (HasIdAndLocalId cascade : this.workspaceDeletionChecker.cascadedDeletions) {
						TransformManager.get().deleteObject(cascade);
					}
					ClientLayerLocator.get()
							.getCommitToStorageTransformListener()
							.flushWithOneoffCallback(deleteObjectCallback);
				} else {
					deleteObjectCallback.onSuccess(null);
				}
			}
		}

		public void performAction(final PermissibleActionEvent event,
				Object node, Object object, final Workspace workspace,
				Class nodeObjectClass) {
			final HasIdAndLocalId hili = (HasIdAndLocalId) object;
			final WorkspaceDeletionChecker workspaceDeletionChecker = new WorkspaceDeletionChecker();
			if (WorkspaceDeletionChecker.enabled) {
				if (!workspaceDeletionChecker.checkPropertyRefs(hili)) {
					return;
				} else {
				}
			}
			ClientLayerLocator
					.get()
					.notifications()
					.confirm(
							"Are you sure you want to delete the selected object",
							new DoDeleteCallback(event, workspace, hili,
									workspaceDeletionChecker));
		}
	}

	@RegistryLocation( registryPoint = CloneActionHandler.class)
	public static class DefaultCloneActionHandler extends
			WorkspaceDefaultActionHandlerBase implements CloneActionHandler {
		public void performAction(final PermissibleActionEvent event,
				Object node, Object object, final Workspace workspace,
				Class nodeObjectClass) {
			final HasIdAndLocalId hili = (HasIdAndLocalId) object;
			HasIdAndLocalId newObj = null;
			List<Object> provisionals = new ArrayList<Object>();
			try {
				DomainObjectCloner cloner = new DomainObjectCloner();
				newObj = cloner.deepBeanClone(hili);
				if (isAutoSave()) {
					ClientTransformManager.cast().promoteToDomainObject(
							cloner.getProvisionalObjects());
					newObj = ClientTransformManager.cast().getObject(newObj);
				} else {
					provisionals.addAll(cloner.getProvisionalObjects());
				}
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
			handleParentLinks(workspace, node, newObj);
			provisionals.addAll(ClientTransformManager.cast().prepareObject(
					newObj, isAutoSave(), true, false));
			TextProvider.get().setDecorated(true);
			String newName = TextProvider.get().getObjectName(newObj)
					+ " (copy)";
			TextProvider.get().setDecorated(false);
			TextProvider.get().setObjectName(newObj, newName);
			Widget view = getContentViewFactory().createBeanView(newObj, true,
					workspace, isAutoSave(), false, provisionals, true);
			workspace.getVisualiser().setContentWidget(view);
			workspace.fireVetoableActionEvent(new PermissibleActionEvent(
					newObj, event.getAction()));
			return;
		}
	}
}
