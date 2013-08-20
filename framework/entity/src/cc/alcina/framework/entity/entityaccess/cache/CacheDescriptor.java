package cc.alcina.framework.entity.entityaccess.cache;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.logic.permissions.IUser;

public abstract class CacheDescriptor {
	Map<Class, CacheItemDescriptor> perClass = new LinkedHashMap<Class, CacheItemDescriptor>();

	public static interface CacheTask {
		/**
		 * @return the lock object, if any
		 */
		public void run(AlcinaMemCache alcinaMemCache) throws Exception;
	}

	public static interface PreProvideTask<T> {
		/**
		 * @return true if cached data was modified
		 */
		public void run(AlcinaMemCache alcinaMemCache, Class clazz,
				List<T> objects) throws Exception;
	}

	public List<CacheTask> postLoadTasks = new ArrayList<CacheTask>();

	public List<PreProvideTask> preProvideTasks = new ArrayList<PreProvideTask>();

	public CacheDescriptor() {
	}

	public void addClasses(Class[] classes) {
		for (Class clazz : classes) {
			perClass.put(clazz, new CacheItemDescriptor(clazz));
		}
	}

	public void addItemDescriptor(CacheItemDescriptor itemDescriptor) {
		perClass.put(itemDescriptor.clazz, itemDescriptor);
	}

	public boolean cachePostTransform(Class clazz) {
		return perClass.containsKey(clazz);
	}

	public boolean joinPropertyCached(Class clazz) {
		return perClass.containsKey(clazz);
	}

	public void addItemDescriptor(Class clazz, String... indexProperties) {
		CacheItemDescriptor itemDescriptor = new CacheItemDescriptor(clazz,
				indexProperties);
		addItemDescriptor(itemDescriptor);
	}

	public abstract Class<? extends IUser> getIUserClass() ;
}
