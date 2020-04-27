package cc.alcina.framework.gwt.client.entity.search.quick;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.logic.domain.VersionableEntity;

public class QuickSearchResponse implements Serializable {
	public List<VersionableEntity> results = new ArrayList<>();

	public int resultCount;

	public QuickSearchRequest request;
}
