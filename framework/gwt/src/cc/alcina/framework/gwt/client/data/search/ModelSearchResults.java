package cc.alcina.framework.gwt.client.data.search;

import java.io.Serializable;
import java.util.List;

import cc.alcina.framework.common.client.logic.domaintransform.DomainUpdate.DomainTransformCommitPosition;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.search.grouping.GroupedResult;
import cc.alcina.framework.gwt.client.data.entity.DataDomainBase;

public class ModelSearchResults implements Serializable {
	static final transient long serialVersionUID = 1L;

	public List<? extends DataDomainBase> queriedResultObjects;

	public GroupedResult groupedResult;

	public int recordCount;

	public SearchDefinition def;

	public int pageNumber;

	public DomainTransformCommitPosition transformLogPosition;
}
