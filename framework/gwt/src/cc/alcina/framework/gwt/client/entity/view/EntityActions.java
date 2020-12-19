package cc.alcina.framework.gwt.client.entity.view;

import java.util.Objects;
import java.util.Optional;

import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.user.client.Window;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.reflection.Association;
import cc.alcina.framework.common.client.logic.reflection.ClientReflector;
import cc.alcina.framework.common.client.search.TruncatedObjectCriterion;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.annotation.ActionRef;
import cc.alcina.framework.gwt.client.dirndl.annotation.ActionRef.ActionHandler;
import cc.alcina.framework.gwt.client.dirndl.annotation.ActionRef.ActionRefHandler;
import cc.alcina.framework.gwt.client.dirndl.annotation.Ref;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.entity.EntityAction;
import cc.alcina.framework.gwt.client.entity.place.ActionRefPlace;
import cc.alcina.framework.gwt.client.entity.place.EntityPlace;
import cc.alcina.framework.gwt.client.logic.MessageManager;

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
			if (Window.confirm(Ax.format(
					"Are you sure you want to delete the selected %s?",
					entityPlace.provideCategoryString(1, false)))) {
				entityPlace.provideEntity().delete();
				MessageManager.get().icyMessage(Ax.format("%s deleted",
						entityPlace.provideCategoryString(1, false)));
				ClientFactory.refreshCurrentPlace();
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
			EntityPlace currentPlace = (EntityPlace) ClientFactory
					.currentPlace();
			EntityPlace entityPlace = Reflections
					.newInstance(currentPlace.getClass());
			Optional<TruncatedObjectCriterion> o_ownerCriterion = ClientReflector
					.get().beanInfoForClass(entityPlace.provideEntityClass())
					.getOwnerReflectors().map(
							ownerReflector -> currentPlace.def
									.provideTruncatedObjectCriterion(
											ownerReflector
													.getAnnotation(
															Association.class)
													.implementationClass())
									.orElse(null))
					.filter(Objects::nonNull).findFirst();
			if (o_ownerCriterion.isPresent()) {
				TruncatedObjectCriterion ownerCriterion = o_ownerCriterion
						.get();
				entityPlace.fromId = ownerCriterion.getId();
				EntityPlace fromPlace = EntityPlace
						.forClass(ownerCriterion.getObjectClass());
				entityPlace.fromClass = fromPlace.toTokenString();
			}
			entityPlace.action = EntityAction.CREATE;
			entityPlace.withId(0);
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
