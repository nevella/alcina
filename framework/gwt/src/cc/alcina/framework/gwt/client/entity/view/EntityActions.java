package cc.alcina.framework.gwt.client.entity.view;

import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.user.client.Window;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.annotation.ActionRef;
import cc.alcina.framework.gwt.client.dirndl.annotation.ActionRef.ActionHandler;
import cc.alcina.framework.gwt.client.dirndl.annotation.ActionRef.ActionRefHandler;
import cc.alcina.framework.gwt.client.dirndl.annotation.Ref;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.entity.EntityAction;
import cc.alcina.framework.gwt.client.entity.place.ActionRefPlace;
import cc.alcina.framework.gwt.client.entity.place.EntityPlace;

public class EntityActions {
	@Ref("edit")
	@ActionRefHandler(EditHandler.class)
	public static class EditRef extends ActionRef {
	}

	public static class EditHandler extends ActionHandler {
		@Override
		public void handleAction(Node node, GwtEvent event,
				ActionRefPlace place) {
			EntityPlace entityPlace = ((EntityPlace) ClientFactory
					.currentPlace()).copy();
			entityPlace.action = EntityAction.EDIT;
			ClientFactory.goTo(entityPlace);
		}
	}

	public static class VoidHandler extends ActionHandler {
		@Override
		public void handleAction(Node node, GwtEvent event,
				ActionRefPlace place) {
		}
	}

	@Ref("delete")
	@ActionRefHandler(DeleteHandler.class)
	public static class DeleteRef extends ActionRef {
	}

	public static class DeleteHandler extends ActionHandler {
		@Override
		public void handleAction(Node node, GwtEvent event,
				ActionRefPlace place) {
			EntityPlace entityPlace = ((EntityPlace) ClientFactory
					.currentPlace()).copy();
			if (Window.confirm(
					Ax.format(
							"Are you sure you want to delete the selected %s",
					entityPlace.provideCategoryString()))) {
				entityPlace.provideEntity().delete();
			}
		}
	}

	@Ref("create")
	@ActionRefHandler(CreateHandler.class)
	public static class CreateRef extends ActionRef {
	}
	public static class CreateHandler extends ActionHandler {
		@Override
		public void handleAction(Node node, GwtEvent event,
				ActionRefPlace place) {
			EntityPlace entityPlace = ((EntityPlace) ClientFactory
					.currentPlace()).copy();
			entityPlace.action = EntityAction.CREATE;
			ClientFactory.goTo(entityPlace);
		}
	}
	public static class ViewHandler extends ActionHandler {
		@Override
		public void handleAction(Node node, GwtEvent event,
				ActionRefPlace place) {
			EntityPlace entityPlace = ((EntityPlace) ClientFactory
					.currentPlace()).copy();
			entityPlace.action = EntityAction.VIEW;
			ClientFactory.goTo(entityPlace);
		}
	}
	@Ref("view")
	@ActionRefHandler(ViewHandler.class)
	public static class ViewRef extends ActionRef {
	}

	@Ref("preview")
	@ActionRefHandler(VoidHandler.class)
	public static class PreviewRef extends ActionRef {
	}
}
