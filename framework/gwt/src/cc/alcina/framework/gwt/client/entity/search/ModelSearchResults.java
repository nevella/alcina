package cc.alcina.framework.gwt.client.entity.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.VersionableEntity;
import cc.alcina.framework.common.client.logic.domaintransform.DomainUpdate.DomainTransformCommitPosition;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.search.grouping.GroupedResult;

public class ModelSearchResults<B extends Bindable> implements Serializable {
	public List<B> queriedResultObjects;

	public Class<B> resultClass() {
		return Reflections.classLookup().getClassForName(resultClassName);
	}

	public String resultClassName;

	public GroupedResult groupedResult;

	public int recordCount;

	public SearchDefinition def;

	public int pageNumber;

	public DomainTransformCommitPosition transformLogPosition;

	public transient VersionableEntity rawEntity;

	public List<Entity> filteringEntities = new ArrayList<>();

	public Entity provideFilteringEntity(EntityLocator locator) {
		return filteringEntities.stream().filter(locator::matches).findFirst()
				.orElse(null);
	}
}
