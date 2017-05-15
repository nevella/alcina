package cc.alcina.framework.entity.entityaccess.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import cc.alcina.framework.common.client.cache.CacheDescriptor.PreProvideTask;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.util.AlcinaTopics;

public abstract class LazyLoadProvideTask<T extends HasIdAndLocalId> implements
		PreProvideTask<T> {
	private long minEvictionAge;

	private int minEvictionSize;

	protected Class<T> clazz;

	public LazyLoadProvideTask(long minEvictionAge, int minEvictionSize,
			Class<T> clazz) {
		this.minEvictionAge = minEvictionAge;
		this.minEvictionSize = minEvictionSize;
		this.clazz = clazz;
	}

	private LinkedHashMap<Long, Long> idEvictionAge = new LinkedHashMap<Long, Long>();

	private synchronized List<T> requireLazyLoad(Collection<T> objects) {
		List<T> result = new ArrayList<T>();
		for (T t : objects) {
			Long evictionAge = idEvictionAge.get(t.getId());
			if (evictionAge == null) {
				result.add(t);
			}
		}
		return result;
	}

	@Override
	public void run( Class clazz,
			Collection<T> objects) throws Exception {
		AlcinaMemCache cache = AlcinaMemCache.get();
		if (clazz != this.clazz) {
			return;
		}
		List<T> requireLoad = requireLazyLoad(objects);
		if (!requireLoad.isEmpty()) {
			if (!beforeLazyLoad(requireLoad)) {
				return;
			}
			synchronized (getLockObject()) {
				// reget, just in case of interim eviction
				requireLoad = requireLazyLoad(objects);
				lazyLoad(cache, requireLoad);
				registerLoaded(cache, requireLoad);
				loadDependents(cache, requireLoad);
			}
		}
	}

	protected Object getLockObject() {
		return this;
	}

	protected void loadDependents(AlcinaMemCache alcinaMemCache,
			List<T> requireLoad) throws Exception {
		
	}

	private void registerLoaded(AlcinaMemCache alcinaMemCache, List<T> requireLoad) {
		for (T t : requireLoad) {
			idEvictionAge.put(t.getId(), System.currentTimeMillis());
		}
		if(isEvictionDisabled()){
			return;
		}
		Iterator<Entry<Long, Long>> itr = idEvictionAge.entrySet().iterator();
		while (idEvictionAge.size() > minEvictionSize && itr.hasNext()) {
			Entry<Long, Long> entry = itr.next();
			if ((System.currentTimeMillis() - entry.getValue()) > minEvictionAge) {
				try {
					evict(alcinaMemCache,entry.getKey());
				} catch (Exception e) {
					AlcinaTopics.notifyDevWarning(e);
				}
				itr.remove();
			}
		}
	}

	protected boolean isEvictionDisabled() {
		//current implementation faulty - it should only happen in a write-locked thread, for a start.
		return true;
	}

	protected abstract void evict(AlcinaMemCache alcinaMemCache, Long key);

	protected abstract void lazyLoad(AlcinaMemCache alcinaMemCache,
			Collection<T> objects) throws Exception;

	protected abstract boolean beforeLazyLoad(List<T> toLoad);
}
