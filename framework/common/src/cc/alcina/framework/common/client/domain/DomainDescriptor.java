package cc.alcina.framework.common.client.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.reflection.DefaultAnnotationResolver;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.Registration.Priority;
import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.common.client.util.CachingMap;

public abstract class DomainDescriptor {
	public Map<Class, DomainClassDescriptor<?>> perClass = new LinkedHashMap<>();

	public List<DomainStoreTask> postLoadTasks = new ArrayList<DomainStoreTask>();

	public List<PreProvideTask> preProvideTasks = new ArrayList<PreProvideTask>();

	public List<ComplexFilter> complexFilters = new ArrayList<>();

	private CachingMap<Class, List<PreProvideTask>> perClassTasks = new CachingMap<Class, List<PreProvideTask>>(
			clazz -> preProvideTasks.stream().filter(
					task -> task.forClazz() == null || task.forClazz() == clazz)
					.collect(Collectors.toList()));

	protected final StorePropertyResolver storePropertyResolver;

	public DomainDescriptor() {
		this(new StorePropertyResolver());
	}

	public DomainDescriptor(StorePropertyResolver storePropertyResolver) {
		this.storePropertyResolver = storePropertyResolver;
	}

	public <T extends Entity> DomainClassDescriptor<T>
			addClassDescriptor(Class<T> clazz, String... indexProperties) {
		DomainClassDescriptor classDescriptor = new DomainClassDescriptor(clazz,
				indexProperties);
		addClassDescriptor(classDescriptor);
		return classDescriptor;
	}

	public void addClassDescriptor(DomainClassDescriptor classDescriptor) {
		Preconditions.checkArgument(
				!perClass.containsKey(classDescriptor.clazz),
				classDescriptor.toString());
		perClass.put(classDescriptor.clazz, classDescriptor);
	}

	public void addClasses(Class[] classes) {
		for (Class clazz : classes) {
			perClass.put(clazz, new DomainClassDescriptor(clazz));
		}
	}

	public void addComplexFilter(ComplexFilter complexFilter) {
		complexFilters.add(complexFilter);
	}

	public boolean applyPostTransform(Class clazz, DomainTransformEvent o) {
		return perClass.containsKey(clazz);
	}

	public synchronized <T> List<PreProvideTask<T>>
			getPreProvideTasks(Class<T> clazz) {
		return (List) perClassTasks.get(clazz);
	}

	public StorePropertyResolver getStorePropertyResolver() {
		return this.storePropertyResolver;
	}

	public void initialise() {
	}

	public boolean joinPropertyCached(Class clazz) {
		return perClass.containsKey(clazz);
	}

	public void registerStore(IDomainStore domainStore) {
		preProvideTasks.stream()
				.forEach(task -> task.registerStore(domainStore));
		postLoadTasks.stream().forEach(task -> task.registerStore(domainStore));
	}

	public static interface DomainStoreTask {
		/**
		 * @return the lock object, if any
		 */
		public void run() throws Exception;

		default void registerStore(IDomainStore domainStore) {
		}
	}

	public static interface PreProvideTask<T> {
		/**
		 * @return true if cached data was modified
		 */
		public void run(Class clazz, Collection<T> objects, boolean topLevel)
				throws Exception;

		default boolean filter(T t) {
			return true;
		}

		Class<T> forClazz();

		default void registerStore(IDomainStore domainStore) {
		}

		Stream<T> wrap(Stream<T> stream);
	}

	@Registration(
		value = AnnotationLocation.Resolver.class,
		priority = Priority.REMOVE)
	public static class StorePropertyResolver
			extends DefaultAnnotationResolver {
	}
}
