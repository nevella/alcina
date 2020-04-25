package cc.alcina.framework.gwt.client.entity.search.quick;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.gwt.client.entity.VersionableDomainBase;

public class QuickSearchResponse implements Serializable {
	public List<VersionableDomainBase> results = new ArrayList<>();

	public int resultCount;

	public QuickSearchRequest request;
}
