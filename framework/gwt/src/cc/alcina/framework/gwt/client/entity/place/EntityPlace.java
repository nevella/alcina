package cc.alcina.framework.gwt.client.entity.place;

import java.util.Collections;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlTransient;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.HasDisplayName;
import cc.alcina.framework.gwt.client.entity.EntityAction;
import cc.alcina.framework.gwt.client.entity.HasEntityAction;
import cc.alcina.framework.gwt.client.entity.search.EntitySearchDefinition;
import cc.alcina.framework.gwt.client.place.BasePlaceTokenizer;
import cc.alcina.framework.gwt.client.place.GenericBasePlace;
import cc.alcina.framework.gwt.client.place.RegistryHistoryMapper;

@XmlAccessorType(XmlAccessType.FIELD)
public abstract class EntityPlace<SD extends EntitySearchDefinition> extends
		GenericBasePlace<SD> implements ClearableIdPlace, HasEntityAction {
	public EntityAction action;

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

	@Override
	public boolean provideIsDefaultDefs() {
		return def.provideHasNoCriteria()
				&& def.getGroupingParameters() == null;
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
			BasePlaceTokenizer tokenizer = RegistryHistoryMapper.get()
					.getTokenizer(this);
			Entity modelObject = TransformManager.get()
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
		return super.toTitleString();
	}

	public Class<? extends Entity> provideEntityClass() {
		return RegistryHistoryMapper.get().getEntityClass(getClass());
	}

	public <E extends Entity> E provideEntity() {
		return (E) TransformManager.get().getObject(provideEntityClass(), id,
				0);
	}

	public String provideCategoryString() {
		return CommonUtils.pluralise(provideEntityClass().getSimpleName(),
				Collections.emptyList());
	}
}