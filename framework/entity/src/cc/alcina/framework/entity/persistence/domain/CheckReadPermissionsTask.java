package cc.alcina.framework.entity.persistence.domain;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.permissions.Permissions;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;

public class CheckReadPermissionsTask<T extends Entity>
		extends LazyLoadProvideTask<T> implements Predicate<T> {
	public CheckReadPermissionsTask(Class<T> clazz, DomainStore domainStore) {
		super(clazz);
		registerStore(domainStore);
	}

	@Override
	protected boolean checkShouldLazyLoad(List<T> toLoad) {
		return false;
	}

	@Override
	protected Object getLockObject() {
		// transactional, no lock
		return null;
	}

	@Override
	protected void lazyLoad(Collection<T> objects) {
	}

	@Override
	protected void loadDependents(List<T> requireLoad) throws Exception {
	}

	@Override
	protected synchronized List requireLazyLoad(Collection objects) {
		return (List) objects.stream().distinct().collect(Collectors.toList());
	}

	@Override
	public boolean test(T t) {
		if (t == null) {
			return false;
		}
		ObjectPermissions op = t.getClass()
				.getAnnotation(ObjectPermissions.class);
		return Permissions.get().checkEffectivePropertyPermission(op, null, t,
				true);
	}

	@Override
	public Stream<T> wrap(Stream<T> stream) {
		if (Permissions.isRoot()) {
			return stream;
		}
		return stream.filter(this);
	}
}
