package cc.alcina.framework.entity.impl.cache;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.internal.CriteriaImpl;

import cc.alcina.framework.entity.entityaccess.cache.AlcinaMemCache;

public class MemCacheQueryTranslator {
	public AlcinaMemCache cache;

	public List list(Criteria criteria) throws NotHandledException {
		if (criteria instanceof CriteriaImpl) {
			CriteriaImpl impl = (CriteriaImpl) criteria;
			// query builder -- projections -> list of functors from rs()
			// criteria
			int debug = 3;
			// translate to a memcache query
			// ex
		}
		throw new NotHandledException();
	}
}
