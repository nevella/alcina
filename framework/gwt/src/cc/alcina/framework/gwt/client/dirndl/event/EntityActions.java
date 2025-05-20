package cc.alcina.framework.gwt.client.dirndl.event;

import java.util.Objects;
import java.util.Optional;

import com.google.gwt.user.client.Window;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.Association;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.search.EntityCriterion;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent.TopLevelHandler;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Create;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Delete;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Edit;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.View;
import cc.alcina.framework.gwt.client.entity.EntityAction;
import cc.alcina.framework.gwt.client.entity.place.EntityPlace;
import cc.alcina.framework.gwt.client.logic.MessageManager;

/**
 * These are enabled by default, disable by overriding with a higher-priority
 * registration
 *
 * 
 *
 */
public class EntityActions {
	public static class CreateHandler implements
			TopLevelHandler<ModelEvents.Create>, ModelEvents.Create.Handler {
		@Override
		public void onCreate(Create event) {
			EntityPlace currentPlace = (EntityPlace) Client.currentPlace();
			EntityPlace entityPlace = Reflections
					.newInstance(currentPlace.getClass());
			Optional<EntityCriterion> o_ownerCriterion = Entity.Ownership
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
				EntityCriterion ownerCriterion = o_ownerCriterion.get();
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

	public static class DeleteHandler implements
			TopLevelHandler<ModelEvents.Delete>, ModelEvents.Delete.Handler {
		@Override
		public void onDelete(Delete event) {
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

	public static class EditHandler implements
			TopLevelHandler<ModelEvents.Edit>, ModelEvents.Edit.Handler {
		@Override
		public void onEdit(Edit event) {
			EntityPlace entityPlace = ((EntityPlace) Client.currentPlace())
					.copy();
			entityPlace.action = EntityAction.EDIT;
			entityPlace.go();
		}
	}

	public static class ViewHandler implements
			TopLevelHandler<ModelEvents.View>, ModelEvents.View.Handler {
		@Override
		public void onView(View event) {
			EntityPlace entityPlace = ((EntityPlace) Client.currentPlace())
					.copy();
			entityPlace.action = EntityAction.VIEW;
			entityPlace.go();
		}
	}
}
