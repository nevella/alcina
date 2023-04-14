package cc.alcina.framework.common.client.csobjects.view;

import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import com.google.gwt.user.client.rpc.GwtTransient;

import cc.alcina.framework.common.client.logic.domain.VersionableEntity;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.reflection.DomainProperty;
import cc.alcina.framework.common.client.logic.reflection.PropertyEnum;
import cc.alcina.framework.common.client.publication.ContentDefinition;

/**
 * A persistent view of the domain (entity graph), characterised by its
 * basis/source (entityDefinition) and its transformation (entityTransformModel)
 *
 * @author nick@alcina.cc
 *
 * @param <V>
 */
@MappedSuperclass
public abstract class DomainView<V extends DomainView>
		extends VersionableEntity<V> {
	@GwtTransient
	private ContentDefinition entityDefinition;

	private String entityDefinitionSerialized;

	@GwtTransient
	private EntityTransformModel entityTransformModel;

	private String entityTransformModelSerialized;

	private String name;

	@Transient
	@DomainProperty(serialize = true)
	public ContentDefinition getEntityDefinition() {
		entityDefinition = TransformManager.resolveMaybeDeserialize(
				entityDefinition, this.entityDefinitionSerialized, null);
		return this.entityDefinition;
	}

	@Transient
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

	@Transient
	public String getEntityTransformModelSerialized() {
		return this.entityTransformModelSerialized;
	}

	public String getName() {
		return this.name;
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

	public void setName(String name) {
		String old_name = this.name;
		this.name = name;
		propertyChangeSupport().firePropertyChange("name", old_name, name);
	}

	public enum Property implements PropertyEnum {
		entityDefinitionSerialized, entityTransformModelSerialized, name
	}
}
