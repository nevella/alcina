package cc.alcina.framework.servlet.sync;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.J8Utils;
import cc.alcina.framework.servlet.sync.FlatDeltaPersister.DeltaItemPersister;
import cc.alcina.framework.servlet.sync.FlatDeltaPersisterResult.FlatDeltaPersisterResultType;
import cc.alcina.framework.servlet.sync.SyncPair.SyncAction;

public class DetachedToDomainPersister<T extends Entity>
		implements DeltaItemPersister<T> {
	public DetachedToDomainPersister() {
		detachedToPersisted = new LinkedHashMap<>();
	}

	public void mergePersisted(DetachedToDomainPersister other) {
		detachedToPersisted.putAll(other.detachedToPersisted);
	}

	protected Map<Entity, Entity> detachedToPersisted;

	@Override
	public FlatDeltaPersisterResultType performSyncAction(SyncAction syncAction,
			T object) throws Exception {
		detachedToPersisted = new LinkedHashMap<>();
		switch (syncAction) {
		case DELETE:
			object.delete();
			return FlatDeltaPersisterResultType.DELETED;
		case CREATE:
			if (object == null) {
				System.err.println("Create with null object");
				return FlatDeltaPersisterResultType.UNMATCHED;
			}
			return detachedToDomainHasDelta(object)
					? FlatDeltaPersisterResultType.CREATED
					: FlatDeltaPersisterResultType.UNMODIFIED;
		case UPDATE:
			return detachedToDomainHasDelta(object)
					? FlatDeltaPersisterResultType.MERGED
					: FlatDeltaPersisterResultType.UNMODIFIED;
		default:
			throw new UnsupportedOperationException();
		}
	}

	private List<ReparentInstruction> reparentInstructions = new ArrayList<>();

	protected <V> void addReparentInstruction(Function<T, V> supplier,
			BiConsumer<T, V> reparentFunction, BiConsumer<T, V> putter) {
		reparentInstructions.add(
				new ReparentInstruction(supplier, reparentFunction, putter));
	}

	public class ReparentInstruction<V> {
		private V toReparent;

		private Function<T, V> getter;

		private BiConsumer<T, V> reparentFunction;

		@SuppressWarnings("unused")
		private BiConsumer<T, V> putter;

		public ReparentInstruction(Function<T, V> getter,
				BiConsumer<T, V> reparentFunction, BiConsumer<T, V> putter) {
			this.getter = getter;
			this.reparentFunction = reparentFunction;
			this.putter = putter;
		}

		public void prepare(T t) {
			toReparent = getter.apply(t);
			if (toReparent instanceof Set) {
				Set replaceReparent = new LinkedHashSet((Set) toReparent);
				Set filtered = ((Set<Entity>) toReparent).stream()
						.filter(o -> {
							if (o.getId() != 0) {
								return true;
							} else {
								o.setLocalId(0);// need to recreate, detached
								return false;
							}
						}).collect(J8Utils.toLiSet());
				((Set) toReparent).removeIf(o -> !filtered.contains(o));
				toReparent = (V) replaceReparent;
			} else {
				throw new UnsupportedOperationException();
			}
		}

		public void withAttached(T attached) {
			reparentFunction.accept(attached, toReparent);
			CommonUtils.wrapInCollection(toReparent).stream().forEach(o -> {
				Entity adb = (Entity) o;
				if (adb.getId() == 0) {
					adb.setLocalId(0);
				}
				Entity toDomain = adb.domain().detachedToDomain();
				detachedToPersisted.put(adb, toDomain);
			});
		}
	}
	protected boolean detachedToDomainHasDelta(T object) {
		return detachedToDomainHasDelta(object, null);
	}
	protected boolean detachedToDomainHasDelta(T object, List<String> ignorePropertyNames) {
		int preCount = TransformManager.get().getTransforms().size();
		reparentInstructions.forEach(i -> i.prepare(object));
		T attached = (T) Domain.detachedToDomain(object,ignorePropertyNames);
		detachedToPersisted.put(object, attached);
		reparentInstructions.forEach(i -> i.withAttached(attached));
		return TransformManager.get().getTransforms().size() != preCount;
	}
}