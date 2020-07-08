package cc.alcina.framework.gwt.client.ide;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.actions.PermissibleActionEvent;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.ClientTransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationSupport;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.permissions.IVersionableOwnable;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.ClientReflector;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.provider.TextProvider;
import cc.alcina.framework.common.client.util.DomainObjectCloner;
import cc.alcina.framework.gwt.client.ClientBase;
import cc.alcina.framework.gwt.client.ClientNotifications;
import cc.alcina.framework.gwt.client.ide.ContentViewFactory.PaneWrapperWithObjects;
import cc.alcina.framework.gwt.client.ide.WorkspaceActionHandler.CloneActionHandler;
import cc.alcina.framework.gwt.client.ide.WorkspaceActionHandler.CreateActionHandler;
import cc.alcina.framework.gwt.client.ide.WorkspaceActionHandler.DeleteActionHandler;
import cc.alcina.framework.gwt.client.ide.WorkspaceActionHandler.EditActionHandler;
import cc.alcina.framework.gwt.client.ide.WorkspaceActionHandler.ViewActionHandler;
import cc.alcina.framework.gwt.client.logic.CommitToStorageTransformListener;
import cc.alcina.framework.gwt.client.logic.OkCallback;

public class WorkspaceDefaultActionHandlers {
	public static abstract class BaseViewActionHandler
			extends WorkspaceDefaultActionHandlerBase {
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

		protected abstract boolean editView();
	}

	@RegistryLocation(registryPoint = CloneActionHandler.class)
	public static class DefaultCloneActionHandler extends
			WorkspaceDefaultActionHandlerBase implements CloneActionHandler {
		@Override
		public void performAction(final PermissibleActionEvent event,
				Object node, Object object, final Workspace workspace,
				Class nodeObjectClass) {
			final Entity entity = (Entity) object;
			Entity newObj = null;
			List<Object> provisionals = new ArrayList<Object>();
			try {
				CollectionModificationSupport.queue(true);
				DomainObjectCloner cloner = new DomainObjectCloner();
				newObj = cloner.deepBeanClone(entity);
				if (isAutoSave()) {
					ClientTransformManager.cast().promoteToDomainObject(
							cloner.getProvisionalObjects());
					newObj = ClientTransformManager.cast().getObject(newObj);
				} else {
					provisionals.addAll(cloner.getProvisionalObjects());
				}
				handleParentLinks(workspace, node, newObj);
				provisionals.addAll(ClientTransformManager.cast()
						.prepareObject(newObj, isAutoSave(), true, false));
				TextProvider.get().setDecorated(true);
				String newName = TextProvider.get().getObjectName(newObj)
						+ " (copy)";
				TextProvider.get().setDecorated(false);
				TextProvider.get().setObjectName(newObj, newName);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			} finally {
				CollectionModificationSupport.queue(false);
			}
			if (isAutoSave()) {
				workspace.getVisualiser().selectNodeForObject(newObj, true);
			}
			Widget view = getContentViewFactory().editable(true)
					.actionListener(workspace).autoSave(isAutoSave())
					.doNotClone(false).additionalProvisional(provisionals)
					.doNotPrepare(true).createBeanView(newObj);
			workspace.getVisualiser().setContentWidget(view);
			workspace.fireVetoableActionEvent(
					new PermissibleActionEvent(newObj, event.getAction()));
			return;
		}
	}

	@RegistryLocation(registryPoint = CreateActionHandler.class)
	public static class DefaultCreateActionHandler extends
			WorkspaceDefaultActionHandlerBase implements CreateActionHandler {
		protected Entity newObj;

		@Override
		public void performAction(PermissibleActionEvent event, Object node,
				Object object, Workspace workspace, Class nodeObjectClass) {
			try {
				CollectionModificationSupport.queue(true);
				newObj = isAutoSave()
						? TransformManager.get()
								.createDomainObject(nodeObjectClass)
						: TransformManager.get()
								.createProvisionalObject(nodeObjectClass);
				handleParentLinks(workspace, node, newObj);
				ClientTransformManager.cast().prepareObject(newObj,
						isAutoSave(), true, false);
				TextProvider.get().setDecorated(false);
				String tdn = ClientReflector.get()
						.beanInfoForClass(nodeObjectClass).getTypeDisplayName();
				TextProvider.get().setDecorated(true);
				TextProvider.get().setObjectName(newObj, "New " + tdn);
				if (newObj instanceof IVersionableOwnable) {
					((IVersionableOwnable) newObj)
							.setOwner(PermissionsManager.get().getUser());
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
			workspace.fireVetoableActionEvent(
					new PermissibleActionEvent(newObj, event.getAction()));
		}
	}

	@RegistryLocation(registryPoint = DeleteActionHandler.class)
	/*
	 * FIXME - mvcc.adjunct - Alcina - the separation of 'deletion of reffing'
	 * and 'deletion' into two transactions was caused by weird EJB3 behaviour -
	 * it works, and works offline->online (for 'fromofflinepersistence') -- but
	 * there's something dodgy going on with Hibernate PersistentSets if they're
	 * combined in the one transaction and problem needs to be fixd
	 */
	public static class DefaultDeleteActionHandler extends
			WorkspaceDefaultActionHandlerBase implements DeleteActionHandler {
		@Override
		public void performAction(final PermissibleActionEvent event,
				Object node, Object object, final Workspace workspace,
				Class nodeObjectClass) {
			final Entity entity = (Entity) object;
			final WorkspaceDeletionChecker workspaceDeletionChecker = new WorkspaceDeletionChecker();
			if (WorkspaceDeletionChecker.enabled) {
				if (!workspaceDeletionChecker.checkPropertyRefs(entity)) {
					return;
				} else {
				}
			}
			Registry.impl(ClientNotifications.class).confirm(
					"Are you sure you want to delete the selected object",
					new DoDeleteCallback(event, workspace, entity,
							workspaceDeletionChecker));
		}

		private class DoDeleteCallback implements OkCallback {
			private final PermissibleActionEvent event;

			private final Workspace workspace;

			private final Entity entity;

			private final WorkspaceDeletionChecker workspaceDeletionChecker;

			private DoDeleteCallback(PermissibleActionEvent event,
					Workspace workspace, Entity entity,
					WorkspaceDeletionChecker workspaceDeletionChecker) {
				this.event = event;
				this.workspace = workspace;
				this.entity = entity;
				this.workspaceDeletionChecker = workspaceDeletionChecker;
			}

			@Override
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
						entity.delete();
						if (workspace != null) {
							workspace.getVisualiser()
									.setContentWidget(new HorizontalPanel());
							workspace.fireVetoableActionEvent(
									new PermissibleActionEvent(entity,
											event.getAction()));
						}
					}
				};
				if (!this.workspaceDeletionChecker.cascadedDeletions
						.isEmpty()) {
					for (Entity cascade : this.workspaceDeletionChecker.cascadedDeletions) {
						cascade.delete();
					}
					Registry.impl(CommitToStorageTransformListener.class)
							.flushWithOneoffCallback(deleteObjectCallback);
				} else {
					deleteObjectCallback.onSuccess(null);
				}
			}
		}
	}

	@RegistryLocation(registryPoint = EditActionHandler.class)
	public static class DefaultEditActionHandler extends BaseViewActionHandler
			implements EditActionHandler {
		@Override
		protected boolean editView() {
			return true;
		}
	}

	@RegistryLocation(registryPoint = ViewActionHandler.class)
	public static class DefaultViewActionHandler extends BaseViewActionHandler
			implements ViewActionHandler {
		@Override
		protected boolean editView() {
			return false;
		}
	}

	@ClientInstantiable
	public abstract static class WorkspaceDefaultActionHandlerBase {
		protected ContentViewFactory getContentViewFactory() {
			ContentViewFactory viewFactory = new ContentViewFactory();
			viewFactory.setCancelButton(true);
			return viewFactory;
		}

		protected void handleParentLinks(Workspace workspace, Object node,
				Entity newObj) {
			workspace.handleParentLinks(node, newObj);
		}

		protected boolean isAutoSave() {
			return ClientBase.getGeneralProperties().isAutoSave();
		}
	}
}
