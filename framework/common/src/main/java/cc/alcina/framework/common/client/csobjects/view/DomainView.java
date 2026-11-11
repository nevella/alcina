package cc.alcina.framework.common.client.csobjects.view;

import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import cc.alcina.framework.common.client.logic.domain.VersionableEntity;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.logic.reflection.DomainProperty;
import cc.alcina.framework.common.client.logic.reflection.PropertyEnum;
import cc.alcina.framework.common.client.publication.ContentDefinition;
import cc.alcina.framework.common.client.reflection.TypedProperties;

/**
 * A persistent view of the domain (entity graph), characterised by its
 * basis/source (entityDefinition) and its transformation (entityTransformModel)
 *
 * 
 *
 * @param <V>
 */
@MappedSuperclass
@TypedProperties
public abstract class DomainView<V extends DomainView>
		extends VersionableEntity<V> {
	public static transient PackageProperties._DomainView properties = PackageProperties.domainView;

	private ContentDefinition entityDefinition;

	private String entityDefinitionSerialized;

	private EntityTransformModel entityTransformModel;

	private String entityTransformModelSerialized;

	private String name;

	@Transient
	@DomainProperty(serialize = true)
	@AlcinaTransient(AlcinaTransient.TransienceContext.CLIENT)
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
	@AlcinaTransient(AlcinaTransient.TransienceContext.CLIENT)
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
		set("entityDefinition", this.entityDefinition, entityDefinition,
				() -> this.entityDefinition = entityDefinition);
	}

	public void
			setEntityDefinitionSerialized(String entityDefinitionSerialized) {
		set("entityDefinitionSerialized", this.entityDefinitionSerialized,
				entityDefinitionSerialized,
				() -> this.entityDefinitionSerialized = entityDefinitionSerialized);
	}

	public void
			setEntityTransformModel(EntityTransformModel entityTransformModel) {
		set("entityTransformModel", this.entityTransformModel,
				entityTransformModel,
				() -> this.entityTransformModel = entityTransformModel);
	}

	public void setEntityTransformModelSerialized(
			String entityTransformModelSerialized) {
		set("entityTransformModelSerialized",
				this.entityTransformModelSerialized,
				entityTransformModelSerialized,
				() -> this.entityTransformModelSerialized = entityTransformModelSerialized);
	}

	public void setName(String name) {
		set("name", this.name, name, () -> this.name = name);
	}
}
