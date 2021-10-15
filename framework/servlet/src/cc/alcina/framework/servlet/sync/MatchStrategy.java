package cc.alcina.framework.servlet.sync;

import java.util.Collection;
import java.util.function.Predicate;

import org.apache.log4j.Logger;

public interface MatchStrategy<T> {
	Collection<T> getAmbiguousRightElements();

	SyncItemMatch<T> getRight(T left);

	void log(Predicate<T> ignoreAmbiguityForReportingFilter, Logger logger,
			Class<T> mergedClass);
}