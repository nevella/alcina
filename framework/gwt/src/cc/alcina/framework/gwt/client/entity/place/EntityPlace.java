package cc.alcina.framework.gwt.client.entity.place;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlTransient;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.domain.search.EntitySearchDefinition;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.HasDisplayName;
import cc.alcina.framework.gwt.client.entity.EntityAction;
import cc.alcina.framework.gwt.client.entity.HasEntityAction;
import cc.alcina.framework.gwt.client.place.BasePlace;
import cc.alcina.framework.gwt.client.place.BindablePlace;
import cc.alcina.framework.gwt.client.place.RegistryHistoryMapper;

@XmlAccessorType(XmlAccessType.FIELD)
public abstract class EntityPlace<SD extends EntitySearchDefinition>
		extends BindablePlace<SD> implements ClearableIdPlace, HasEntityAction {
	public static EntityPlace forClass(Class clazz) {
		return (EntityPlace) RegistryHistoryMapper.get()
				.getPlaceByModelClass(clazz);
	}

	public static EntityPlace forClassAndId(Class clazz, long id) {
		return (EntityPlace) forClass(clazz).withId(id);
	}

	public static EntityPlace forEntity(Entity entity) {
		return forClassAndId(entity.getClass(), entity.getId())
				.withEntity(entity);
	}

	public transient Entity entity;

	public EntityAction action = EntityAction.VIEW;

	public EntityLocator asLocator() {
		return entity != null ? entity.toLocator()
				: new EntityLocator(provideEntityClass(), id, 0);
	}

	@Override
	public void clearIds() {
		id = 0;
	}

	@Override
	@XmlTransient
	public EntityAction getAction() {
		return this.action;
	}

	@Override
	public SD getSearchDefinition() {
		return super.getSearchDefinition();
	}

	public String provideCategoryString() {
		return CommonUtils.pluralise(provideEntityClass().getSimpleName(), 0,
				false);
	}

	public String provideCategoryString(int size, boolean withCount) {
		return CommonUtils.pluralise(provideEntityClass().getSimpleName(), size,
				withCount);
	}

	public <E extends Entity> E provideEntity() {
		return entity != null ? (E) entity
				: (E) Domain.find(provideEntityClass(), id);
	}

	public Class<? extends Entity> provideEntityClass() {
		return RegistryHistoryMapper.get().getEntityClass(getClass());
	}

	public boolean provideHasEntity() {
		return entity != null;
	}

	@Override
	public boolean provideIsDefaultDefs() {
		return def.provideHasNoCriteria() && def.getGroupingParameters() == null
				&& def.provideIsDefaultSortOrder() && def.getPageNumber() == 0;
	}

	@Override
	public void setAction(EntityAction action) {
		this.action = action;
	}

	@Override
	public String toNameString() {
		Entity modelObject = provideEntity();
		if (modelObject != null) {
			if (modelObject instanceof HasDisplayName) {
				return ((HasDisplayName) modelObject).displayName();
			} else if (modelObject instanceof HasDisplayName) {
				return ((HasDisplayName) modelObject).displayName();
			} else {
				return super.toString();
			}
		} else {
			return provideCategoryString();
		}
	}

	@Override
	public String toTitleString() {
		String category = super.toTitleString();
		if (id != 0) {
			EntityPlaceTokenizer tokenizer = (EntityPlaceTokenizer) RegistryHistoryMapper
					.get().getTokenizer(this);
			Entity modelObject = TransformManager.get().getObjectStore()
					.getObject(tokenizer.getModelClass(), id, 0);
			if (modelObject == null
					|| !(modelObject instanceof HasDisplayName)) {
				return Ax.format("%s #%s", category, id);
			} else {
				String text = HasDisplayName.displayName(modelObject);
				return Ax.format("%s %s", category,
						CommonUtils.trimToWsChars(text, 20, true));
			}
		}
		if (def != null) {
			if (def.provideIsSimpleTextSearch()) {
				String text = def.provideSimpleTextSearchCriterion().getValue();
				return Ax.format("%s '%s'", category,
						CommonUtils.trimToWsChars(text, 20, true));
			}
		}
		return provideCategoryString();
	}

	@Override
	public void updateFrom(BasePlace other) {
		Preconditions.checkArgument(other.getClass() == getClass());
		entity = ((EntityPlace) other).entity;
	}

	public <EP extends EntityPlace> EP withAction(EntityAction action) {
		this.action = action;
		return (EP) this;
	}

	public <EP extends EntityPlace> EP withEntity(Entity entity) {
		this.entity = entity;
		withHasId(entity);
		return (EP) this;
	}
}