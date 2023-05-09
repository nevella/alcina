package cc.alcina.framework.gwt.client.dirndl.event;

import java.util.Objects;
import java.util.Optional;

import com.google.gwt.user.client.Window;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.Association;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.search.TruncatedObjectCriterion;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent.TopLevelHandler;
import cc.alcina.framework.gwt.client.entity.EntityAction;
import cc.alcina.framework.gwt.client.entity.place.EntityPlace;
import cc.alcina.framework.gwt.client.logic.MessageManager;

/**
 * These are enabled by default, disable by overriding with a higher-priority
 * registration
 *
 * @author nick@alcina.cc
 *
 */
public class EntityActions {
	public static class CreateHandler
			implements TopLevelHandler<ModelEvents.Create> {
		@Override
		public void handle(ModelEvents.Create unhandledEvent) {
			EntityPlace currentPlace = (EntityPlace) Client.currentPlace();
			EntityPlace entityPlace = Reflections
					.newInstance(currentPlace.getClass());
			Optional<TruncatedObjectCriterion> o_ownerCriterion = Entity.Ownership
					.getOwnerReflectors(entityPlace.provideEntityClass()).map(
							ownerReflector -> currentPlace.def
									.provideTruncatedObjectCriterion(
											ownerReflector
													.annotation(
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
			entityPlace.go();
		}
	}

	public static class DeleteHandler
			implements TopLevelHandler<ModelEvents.Delete> {
		@Override
		public void handle(ModelEvents.Delete event) {
			EntityPlace entityPlace = ((EntityPlace) Client.currentPlace())
					.copy();
			if (Window.confirm(Ax.format(
					"Are you sure you want to delete the selected %s?",
					entityPlace.provideCategoryString(1, false)))) {
				entityPlace.provideEntity().delete();
				MessageManager.get().icyMessage(Ax.format("%s deleted",
						entityPlace.provideCategoryString(1, false)));
				Client.refreshCurrentPlace();
			}
		}
	}

	public static class EditHandler
			implements TopLevelHandler<ModelEvents.Edit> {
		@Override
		public void handle(ModelEvents.Edit event) {
			EntityPlace entityPlace = ((EntityPlace) Client.currentPlace())
					.copy();
			entityPlace.action = EntityAction.EDIT;
			entityPlace.go();
		}
	}

	public static class ViewHandler
			implements TopLevelHandler<ModelEvents.View> {
		@Override
		public void handle(ModelEvents.View event) {
			EntityPlace entityPlace = ((EntityPlace) Client.currentPlace())
					.copy();
			entityPlace.action = EntityAction.VIEW;
			entityPlace.go();
		}
	}
}
