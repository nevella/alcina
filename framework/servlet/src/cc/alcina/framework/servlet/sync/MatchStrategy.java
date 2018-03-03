package cc.alcina.framework.servlet.sync;

import java.util.Collection;

import org.apache.log4j.Logger;

import cc.alcina.framework.common.client.collections.CollectionFilter;

public interface MatchStrategy<T> {
	SyncItemMatch<T> getRight(T left);

	Collection<T> getAmbiguousRightElements();



	void log(CollectionFilter<T> ignoreAmbiguityForReportingFilter,
			Logger logger, Class<T> mergedClass);
}