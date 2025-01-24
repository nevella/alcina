package cc.alcina.framework.entity.persistence.domain.segment;

import cc.alcina.framework.entity.persistence.domain.DomainStoreLoaderDatabase.ConnResultsReuse;

public interface DomainSegmentLoader extends ConnResultsReuse.Has {
	void init();
}
