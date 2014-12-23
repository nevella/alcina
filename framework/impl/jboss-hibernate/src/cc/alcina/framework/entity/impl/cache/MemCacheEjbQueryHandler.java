package cc.alcina.framework.entity.impl.cache;

import java.util.List;

import org.hibernate.Criteria;

import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.entityaccess.cache.AlcinaMemCache;

public class MemCacheEjbQueryHandler {
	AlcinaMemCache cache = AlcinaMemCache.get();

	private MemCacheQueryTranslator translator;

	public MemCacheEjbQueryHandler(MemCacheQueryTranslator translator) {
		this.translator = translator;
		translator.cache=cache;
	}

	public List list(Criteria criteria) {
		if (ResourceUtilities
				.is(MemCacheEjbQueryHandler.class, "resolveWithDb")) {
			return criteria.list();
		} else {
			try {
				return translator.list(criteria);
			} catch (NotHandledException ex) {
				return criteria.list();
			}
		}
	}
}
