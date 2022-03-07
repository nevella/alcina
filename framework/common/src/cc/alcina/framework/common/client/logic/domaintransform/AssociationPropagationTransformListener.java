package cc.alcina.framework.common.client.logic.domaintransform;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager.ApplyToken;
import cc.alcina.framework.common.client.logic.reflection.Association;
import cc.alcina.framework.common.client.logic.reflection.resolution.Annotations;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;

/*
 * Called directly on the client (from
 * CommitToLocalDomainTransformListener), during mvcc transactions after
 * property changes (server)
 */
public class AssociationPropagationTransformListener
		implements DomainTransformListener {
	private CommitType filterType;

	public AssociationPropagationTransformListener(CommitType filterType) {
		this.filterType = filterType;
	}

	@Override
	public void domainTransform(DomainTransformEvent event)
			throws DomainTransformException {
		if (event.getCommitType() != filterType) {
			return;
		}
		TransformManager tm = TransformManager.get();
		if (!tm.handlesAssociationsFor(event.getObjectClass())) {
			return;
		}
		// FIXME - mvcc.adjunct (since the object will be reachable)
		if (Objects.equals("id", event.getPropertyName())) {
			return;
		}
		{
			// early exit
			switch (event.getTransformType()) {
			case CREATE_OBJECT:
				return;
			case NULL_PROPERTY_REF:
			case CHANGE_PROPERTY_REF: {
				Association association = Annotations.resolve(
						event.getObjectClass(), event.getPropertyName(),
						Association.class);
				if (association == null) {
					return;
				}
			}
			}
		}
		ApplyToken token = tm.createApplyToken(event);
		Entity<?> entity = token.object;
		switch (token.transformType) {
		case ADD_REF_TO_COLLECTION:
		case REMOVE_REF_FROM_COLLECTION: {
			Association association = Reflections.at(entity.entityClass())
					.property(event.getPropertyName())
					.annotation(Association.class);
			if (!Reflections.at(association.implementationClass())
					.provideIsReflective()) {
				return;
			}
		}
		}
		switch (token.transformType) {
		case NULL_PROPERTY_REF:
		case CHANGE_PROPERTY_REF: {
			tm.updateAssociation(event.getPropertyName(), entity,
					token.existingTargetEntity, true);
			tm.updateAssociation(event.getPropertyName(), entity,
					token.newTargetEntity, false);
			break;
		}
		case ADD_REF_TO_COLLECTION: {
			tm.updateAssociation(event.getPropertyName(), entity,
					token.newTargetEntity, false);
			break;
		}
		case REMOVE_REF_FROM_COLLECTION: {
			tm.updateAssociation(event.getPropertyName(), entity,
					token.newTargetEntity, true);
			break;
		}
		case DELETE_OBJECT: {
			Reflections.at(entity.entityClass()).properties().stream()
					.filter(p -> p.has(Association.class)).forEach(property -> {
						Association association = property
								.annotation(Association.class);
						Object associated = property.get(entity);
						if (tm.markedForDeletion.contains(associated)) {
							return;
						}
						if (association.cascadeDeletes()) {
							// parent.children
							if (associated instanceof Set) {
								// copy to avoid recursive concurrent
								// modification
								((Set<? extends Entity>) associated).stream()
										.collect(Collectors.toList())
										.forEach(Entity::delete);
								// child.parent (!!)
							} else if (associated instanceof Entity) {
								((Entity) associated).delete();
							} else {
								Preconditions.checkArgument(associated == null);
							}
						} else if (association.dereferenceOnDelete()) {
							if (!Reflections
									.at(association.implementationClass())
									.provideIsReflective()) {
								return;
							}
							Property associatedProperty = Reflections
									.at(association.implementationClass())
									.property(association.propertyName());
							if (associated instanceof Set) {
								Set<? extends Entity> associatedSet = (Set<? extends Entity>) associated;
								for (Entity associatedEntity : associatedSet) {
									associatedEntity.domain().register();
									Object associatedAssociationValue = associatedProperty
											.get(associatedEntity);
									// many-to-many
									if (associatedAssociationValue instanceof Set) {
										associatedEntity.domain()
												.removeFromProperty(
														association
																.propertyName(),
														entity);
									} else {
										// parent.children
										associatedProperty.set(associatedEntity,
												null);
									}
								}
							} else if (associated instanceof Entity) {
								((Entity) associated).domain().register();
								Object associatedAssociationValue = associatedProperty
										.get(associated);
								if (associatedAssociationValue instanceof Set) {
									// child.parent - optimised
									tm.updateAssociation(property.getName(),
											entity, (Entity) associated, true);
									// ((Entity) associated).domain()
									// .removeFromProperty(
									// association.propertyName(),
									// entity);
								} else if (associated instanceof Entity) {
									// one-one(!!)
									associatedProperty.set(associated, null);
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
							// entity.entityClass()
							// .getSimpleName(),
							// property.getPropertyName());
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