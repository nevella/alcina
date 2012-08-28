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

import com.google.gwt.user.cellview.client.TreeNode;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;

public class WorkspaceDefaultActionHandlers {
	@ClientInstantiable
	protected abstract static class WorkspaceDefaultActionHandlerBase {
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

	@RegistryLocation(j2seOnly = false, registryPoint = ViewActionHandler.class)
	public static class DefaultViewActionHandler extends
			WorkspaceDefaultActionHandlerBase implements ViewActionHandler {
		public void performAction(PermissibleActionEvent event, Object node,
				Object object, Workspace workspace, Class nodeObjectClass) {
			Widget view = null;
			if (object instanceof Collection) {
				view=workspace.createMultipleBeanView(
						(Collection) object, nodeObjectClass, editView(),
						workspace, isAutoSave(), false);
			} else {
				PaneWrapperWithObjects paneWrapper = getContentViewFactory().createBeanView(object,
						editView(), workspace, isAutoSave(), false);
				view=paneWrapper;
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

	@RegistryLocation(j2seOnly = false, registryPoint = CreateActionHandler.class)
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
				e.printStackTrace(System.err);
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

	@RegistryLocation(j2seOnly = false, registryPoint = EditActionHandler.class)
	public static class DefaultEditActionHandler extends
			DefaultViewActionHandler implements ViewActionHandler {
		@Override
		protected boolean editView() {
			return true;
		}
	}

	@RegistryLocation(j2seOnly = false, registryPoint = DeleteActionHandler.class)
	public static class DefaultDeleteActionHandler extends
			WorkspaceDefaultActionHandlerBase implements DeleteActionHandler {
		public void performAction(final PermissibleActionEvent event,
				Object node, Object object, final Workspace workspace,
				Class nodeObjectClass) {
			final HasIdAndLocalId hili = (HasIdAndLocalId) object;
			if (WorkspaceDeletionChecker.enabled) {
				if (!new WorkspaceDeletionChecker().checkPropertyRefs(hili)) {
					return;
				}
			}
			ClientLayerLocator
					.get()
					.notifications()
					.confirm(
							"Are you sure you want to delete the selected object",
							new OkCallback() {
								public void ok() {
									TransformManager.get().deleteObject(hili);
									workspace.getVisualiser().setContentWidget(
											new HorizontalPanel());
									workspace
											.fireVetoableActionEvent(new PermissibleActionEvent(
													hili, event.getAction()));
								}
							});
		}
	}

	@RegistryLocation(j2seOnly = false, registryPoint = CloneActionHandler.class)
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
