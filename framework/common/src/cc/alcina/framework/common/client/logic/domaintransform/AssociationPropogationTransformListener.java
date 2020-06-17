package cc.alcina.framework.common.client.logic.domaintransform;

import java.util.Set;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager.ApplyToken;
import cc.alcina.framework.common.client.logic.reflection.Association;
import cc.alcina.framework.common.client.logic.reflection.PropertyReflector;

/*
 * Called directly on the client (from
 * CommitToLocalDomainTransformListener), during mvcc transactions after
 * property changes (server)
 */
public class AssociationPropogationTransformListener
		implements DomainTransformListener {
	private CommitType filterType;

	public AssociationPropogationTransformListener(CommitType filterType) {
		this.filterType = filterType;
	}

	@Override
	public void domainTransform(DomainTransformEvent event)
			throws DomainTransformException {
		if (event.getCommitType() != filterType) {
			return;
		}
		TransformManager tm = TransformManager.get();
		ApplyToken token = tm.createApplyToken(event);
		Entity entity = token.object;
		switch (token.transformType) {
		case NULL_PROPERTY_REF:
		case CHANGE_PROPERTY_REF: {
			tm.updateAssociation(event, entity, token.existingTargetObject,
					true);
			tm.updateAssociation(event, entity, token.newTargetObject, false);
			break;
		}
		case ADD_REF_TO_COLLECTION: {
			tm.updateAssociation(event, entity, token.newTargetObject, false);
			break;
		}
		case REMOVE_REF_FROM_COLLECTION: {
			tm.updateAssociation(event, entity, token.newTargetObject, true);
			break;
		}
		case DELETE_OBJECT: {
			Reflections.iterateForPropertyWithAnnotation(
					entity.provideEntityClass(), Association.class,
					(association, propertyReflector) -> {
						Object associated = propertyReflector
								.getPropertyValue(entity);
						if (association.cascadeDeletes()) {
							// parent.children
							if (associated instanceof Set) {
								((Set<? extends Entity>) associated)
										.forEach(Entity::delete);
								// child.parent (!!)
							} else if (associated instanceof Entity) {
								((Entity) associated).delete();
							} else {
								Preconditions.checkArgument(associated == null);
							}
						} else if (association.dereferenceOnDelete()) {
							PropertyReflector associatedObjectAccessor = Reflections
									.propertyAccessor().getPropertyReflector(
											association.implementationClass(),
											association.propertyName());
							if (associated instanceof Set) {
								Set<? extends Entity> associatedSet = (Set<? extends Entity>) associated;
								for (Entity associatedEntity : associatedSet) {
									associatedEntity.domain().register();
									Object associatedAssociationValue = associatedObjectAccessor
											.getPropertyValue(associatedEntity);
									// many-to-many
									if (associatedAssociationValue instanceof Set) {
										associatedEntity.domain()
												.removeFromProperty(
														association
																.propertyName(),
														entity);
									} else {
										// parent.children
										associatedObjectAccessor
												.setPropertyValue(
														associatedEntity, null);
									}
								}
							} else if (associated instanceof Entity) {
								((Entity) associated).domain().register();
								Object associatedAssociationValue = associatedObjectAccessor
										.getPropertyValue(associated);
								if (associatedAssociationValue instanceof Set) {
									// child.parent
									((Entity) associated).domain()
											.removeFromProperty(
													association.propertyName(),
													entity);
								} else if (associated instanceof Entity) {
									// one-one(!!)
									associatedObjectAccessor
											.setPropertyValue(associated, null);
								} else {
									throw new UnsupportedOperationException();
								}
							} else {
								Preconditions.checkArgument(associated == null);
							}
						} else {
							// this means that a dependent object won't be
							// dereffed/deleted - so will block deletion if
							// existing
							//
							// throw Ax.runtimeException(
							// "Association with no delete behaviour:
							// %s.%s",
							// entity.provideEntityClass()
							// .getSimpleName(),
							// propertyReflector.getPropertyName());
						}
					});
			break;
		}
		default:
			// non-associiation
			break;
		}
	}
}