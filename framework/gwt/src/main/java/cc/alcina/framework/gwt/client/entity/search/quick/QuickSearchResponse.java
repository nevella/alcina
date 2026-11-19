package cc.alcina.framework.gwt.client.entity.search.quick;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.logic.domain.VersionableEntity;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;

@Bean
public class QuickSearchResponse implements Serializable {
	private List<VersionableEntity> results = new ArrayList<>();

	private int resultCount;

	private QuickSearchRequest request;

	public QuickSearchRequest getRequest() {
		return request;
	}

	public int getResultCount() {
		return resultCount;
	}

	public List<VersionableEntity> getResults() {
		return results;
	}

	public void setRequest(QuickSearchRequest request) {
		this.request = request;
	}

	public void setResultCount(int resultCount) {
		this.resultCount = resultCount;
	}

	public void setResults(List<VersionableEntity> results) {
		this.results = results;
	}
}
