package cc.alcina.framework.common.client.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.permissions.IUser;

public abstract class CacheDescriptor {
	public Map<Class, CacheItemDescriptor> perClass = new LinkedHashMap<Class, CacheItemDescriptor>();

	public List<CacheTask> postLoadTasks = new ArrayList<CacheTask>();

	public List<PreProvideTask> preProvideTasks = new ArrayList<PreProvideTask>();

	public List<ComplexFilter> complexFilters = new ArrayList<>();

	public CacheDescriptor() {
	}

	public void addClasses(Class[] classes) {
		for (Class clazz : classes) {
			perClass.put(clazz, new CacheItemDescriptor(clazz));
		}
	}

	public void addComplexFilter(ComplexFilter complexFilter) {
		complexFilters.add(complexFilter);
	}

	public void addItemDescriptor(CacheItemDescriptor itemDescriptor) {
		perClass.put(itemDescriptor.clazz, itemDescriptor);
	}

	public void addItemDescriptor(Class clazz, String... indexProperties) {
		CacheItemDescriptor itemDescriptor = new CacheItemDescriptor(clazz,
				indexProperties);
		addItemDescriptor(itemDescriptor);
	}

	public boolean cachePostTransform(Class clazz, DomainTransformEvent o) {
		return perClass.containsKey(clazz);
	}

	public abstract Class<? extends IUser> getIUserClass();

	public boolean joinPropertyCached(Class clazz) {
		return perClass.containsKey(clazz);
	}

	public static interface CacheTask {
		/**
		 * @return the lock object, if any
		 */
		public void run() throws Exception;
	}

	public static interface PreProvideTask<T> {
		/**
		 * @return true if cached data was modified
		 */
		public void run(Class clazz, Collection<T> objects) throws Exception;
		
		public void writeLockedCleanup();
	}
}
