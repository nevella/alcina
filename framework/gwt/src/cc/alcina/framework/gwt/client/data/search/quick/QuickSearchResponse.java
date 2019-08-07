package cc.alcina.framework.gwt.client.data.search.quick;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.gwt.client.data.entity.DataDomainBase;

public class QuickSearchResponse implements Serializable {
	public List<DataDomainBase> results = new ArrayList<>();

	public int resultCount;

	public QuickSearchRequest request;
}
