package cc.alcina.framework.common.client.view;

import javax.persistence.Transient;

import cc.alcina.framework.common.client.logic.domain.VersionableEntity;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.reflection.DomainProperty;
import cc.alcina.framework.common.client.publication.ContentDefinition;

public abstract class DomainView extends VersionableEntity<DomainView> {
	private ContentDefinition entityDefinition;

	private String entityDefinitionSerialized;

	private EntityTransformModel entityTransformModel;

	private String entityTransformModelSerialized;

	@Transient
	@DomainProperty(serialize = true)
	public ContentDefinition getEntityDefinition() {
		entityDefinition = TransformManager.resolveMaybeDeserialize(
				entityDefinition, this.entityDefinitionSerialized, null);
		return this.entityDefinition;
	}

	public String getEntityDefinitionSerialized() {
		return this.entityDefinitionSerialized;
	}

	@Transient
	@DomainProperty(serialize = true)
	public EntityTransformModel getEntityTransformModel() {
		entityTransformModel = TransformManager.resolveMaybeDeserialize(
				entityTransformModel, this.entityTransformModelSerialized,
				null);
		return this.entityTransformModel;
	}

	public String getEntityTransformModelSerialized() {
		return this.entityTransformModelSerialized;
	}

	public void setEntityDefinition(ContentDefinition entityDefinition) {
		ContentDefinition old_entityDefinition = this.entityDefinition;
		this.entityDefinition = entityDefinition;
		propertyChangeSupport().firePropertyChange("entityDefinition",
				old_entityDefinition, entityDefinition);
	}

	public void
			setEntityDefinitionSerialized(String entityDefinitionSerialized) {
		String old_entityDefinitionSerialized = this.entityDefinitionSerialized;
		this.entityDefinitionSerialized = entityDefinitionSerialized;
		propertyChangeSupport().firePropertyChange("entityDefinitionSerialized",
				old_entityDefinitionSerialized, entityDefinitionSerialized);
	}

	public void
			setEntityTransformModel(EntityTransformModel entityTransformModel) {
		EntityTransformModel old_entityTransformModel = this.entityTransformModel;
		this.entityTransformModel = entityTransformModel;
		propertyChangeSupport().firePropertyChange("entityTransformModel",
				old_entityTransformModel, entityTransformModel);
	}

	public void setEntityTransformModelSerialized(
			String entityTransformModelSerialized) {
		String old_entityTransformModelSerialized = this.entityTransformModelSerialized;
		this.entityTransformModelSerialized = entityTransformModelSerialized;
		propertyChangeSupport().firePropertyChange(
				"entityTransformModelSerialized",
				old_entityTransformModelSerialized,
				entityTransformModelSerialized);
	}
}
