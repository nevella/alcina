package cc.alcina.framework.common.client.domain.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.csobjects.IsBindable;
import cc.alcina.framework.common.client.csobjects.SearchResult;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.DomainUpdate.DomainTransformCommitPosition;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.search.grouping.GroupedResult;

public class ModelSearchResults<B extends IsBindable & SearchResult>
		implements Serializable {
	public List<B> queriedResultObjects;

	public String resultClassName;

	public GroupedResult groupedResult;

	public int recordCount;

	public BindableSearchDefinition def;

	public int pageNumber;

	public DomainTransformCommitPosition transformLogPosition;

	public transient Entity rawEntity;

	public List<Entity> filteringEntities = new ArrayList<>();

	public Entity provideFilteringEntity(EntityLocator locator) {
		return filteringEntities.stream().filter(locator::matches).findFirst()
				.orElse(null);
	}

	public int provideLastPageNumber() {
		if (recordCount == 0) {
			return 0;
		}
		int pageCount = (recordCount - 1) / def.getResultsPerPage() + 1;
		return pageCount;
	}

	public Class<B> resultClass() {
		if (resultClassName == null && def instanceof EntitySearchDefinition) {
			return (Class<B>) (Class<?>) ((EntitySearchDefinition) def)
					.queriedEntityClass();
		}
		return Reflections.forName(resultClassName);
	}
}
