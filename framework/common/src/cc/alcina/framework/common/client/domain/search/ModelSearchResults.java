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
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.logic.reflection.Bean;
import cc.alcina.framework.common.client.search.grouping.GroupedResult;
import cc.alcina.framework.common.client.util.Ax;

@Bean
public class ModelSearchResults<B extends IsBindable & SearchResult>
		implements Serializable {
	private List<B> queriedResultObjects;

	private String resultClassName;

	private GroupedResult groupedResult;

	private int recordCount;

	private BindableSearchDefinition def;

	private int pageNumber;

	private DomainTransformCommitPosition transformLogPosition;

	private transient Entity rawEntity;

	private List<Entity> filteringEntities = new ArrayList<>();

	public BindableSearchDefinition getDef() {
		return def;
	}

	public List<Entity> getFilteringEntities() {
		return filteringEntities;
	}

	public GroupedResult getGroupedResult() {
		return groupedResult;
	}

	public int getPageNumber() {
		return pageNumber;
	}

	public List<B> getQueriedResultObjects() {
		return queriedResultObjects;
	}

	@AlcinaTransient
	public Entity getRawEntity() {
		return rawEntity;
	}

	public int getRecordCount() {
		return recordCount;
	}

	public String getResultClassName() {
		return resultClassName;
	}

	public DomainTransformCommitPosition getTransformLogPosition() {
		return transformLogPosition;
	}

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

	public void setDef(BindableSearchDefinition def) {
		this.def = def;
	}

	public void setFilteringEntities(List<Entity> filteringEntities) {
		this.filteringEntities = filteringEntities;
	}

	public void setGroupedResult(GroupedResult groupedResult) {
		this.groupedResult = groupedResult;
	}

	public void setPageNumber(int pageNumber) {
		this.pageNumber = pageNumber;
	}

	public void setQueriedResultObjects(List<B> queriedResultObjects) {
		this.queriedResultObjects = queriedResultObjects;
	}

	public void setRawEntity(Entity rawEntity) {
		this.rawEntity = rawEntity;
	}

	public void setRecordCount(int recordCount) {
		this.recordCount = recordCount;
	}

	public void setResultClassName(String resultClassName) {
		this.resultClassName = resultClassName;
	}

	public void setTransformLogPosition(
			DomainTransformCommitPosition transformLogPosition) {
		this.transformLogPosition = transformLogPosition;
	}

	@Override
	public String toString() {
		return Ax.format("Search: %s\n%s results", def,
				queriedResultObjects.size());
	}
}
