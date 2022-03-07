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
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.provider.TextProvider;
import cc.alcina.framework.common.client.util.DomainObjectCloner;
import cc.alcina.framework.gwt.client.ClientNotifications;
import cc.alcina.framework.gwt.client.entity.GeneralProperties;
import cc.alcina.framework.gwt.client.gwittir.RenderedClass;
import cc.alcina.framework.gwt.client.ide.ContentViewFactory.PaneWrapperWithObjects;
import cc.alcina.framework.gwt.client.ide.WorkspaceActionHandler.CloneActionHandler;
import cc.alcina.framework.gwt.client.ide.WorkspaceActionHandler.CreateActionHandler;
import cc.alcina.framework.gwt.client.ide.WorkspaceActionHandler.DeleteActionHandler;
import cc.alcina.framework.gwt.client.ide.WorkspaceActionHandler.EditActionHandler;
import cc.alcina.framework.gwt.client.ide.WorkspaceActionHandler.ViewActionHandler;
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

	@Registration(CloneActionHandler.class)
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
				TextProvider.get().putDisplayName(newObj, newName);
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

	@Registration(CreateActionHandler.class)
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
				String tdn = RenderedClass.getTypeDisplayName(nodeObjectClass);
				TextProvider.get().setDecorated(true);
				TextProvider.get().putDisplayName(newObj, "New " + tdn);
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

	@Registration(DeleteActionHandler.class)
	public static class DefaultDeleteActionHandler extends
			WorkspaceDefaultActionHandlerBase implements DeleteActionHandler {
		@Override
		public void performAction(final PermissibleActionEvent event,
				Object node, Object object, final Workspace workspace,
				Class nodeObjectClass) {
			final Entity entity = (Entity) object;
			Registry.impl(ClientNotifications.class).confirm(
					"Are you sure you want to delete the selected object",
					new DoDeleteCallback(event, workspace, entity));
		}

		private class DoDeleteCallback implements OkCallback {
			private final PermissibleActionEvent event;

			private final Workspace workspace;

			private final Entity entity;

			private DoDeleteCallback(PermissibleActionEvent event,
					Workspace workspace, Entity entity) {
				this.event = event;
				this.workspace = workspace;
				this.entity = entity;
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
				deleteObjectCallback.onSuccess(null);
			}
		}
	}

	@Registration(EditActionHandler.class)
	public static class DefaultEditActionHandler extends BaseViewActionHandler
			implements EditActionHandler {
		@Override
		protected boolean editView() {
			return true;
		}
	}

	@Registration(ViewActionHandler.class)
	public static class DefaultViewActionHandler extends BaseViewActionHandler
			implements ViewActionHandler {
		@Override
		protected boolean editView() {
			return false;
		}
	}

	@Reflected
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
			return GeneralProperties.get().isAutoSave();
		}
	}
}
