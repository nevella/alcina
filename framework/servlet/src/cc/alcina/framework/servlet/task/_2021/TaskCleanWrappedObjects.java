package cc.alcina.framework.servlet.task._2021;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.persistence.Table;

import com.google.api.client.util.Preconditions;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.domain.search.EntityCriteriaGroup;
import cc.alcina.framework.common.client.domain.search.EntitySearchDefinition;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.EntityHelper;
import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.domain.UserProperty;
import cc.alcina.framework.common.client.logic.domain.UserPropertyPersistable;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.PersistentImpl;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.publication.DeliveryModel;
import cc.alcina.framework.common.client.publication.Publication;
import cc.alcina.framework.common.client.publication.Publication.Definition;
import cc.alcina.framework.common.client.search.CriteriaGroup;
import cc.alcina.framework.common.client.serializer.flat.TreeSerializable;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.persistence.CommonPersistenceBase.UnwrapWithExceptionsResult;
import cc.alcina.framework.entity.persistence.CommonPersistenceProvider;
import cc.alcina.framework.entity.persistence.WrappedObject;
import cc.alcina.framework.entity.persistence.domain.ClassIdLock;
import cc.alcina.framework.entity.persistence.domain.DataSourceAdapter;
import cc.alcina.framework.entity.persistence.domain.DomainStore;
import cc.alcina.framework.entity.persistence.domain.LazyLoadProvideTask.SimpleLoaderTask;
import cc.alcina.framework.entity.persistence.domain.LockUtils;
import cc.alcina.framework.entity.persistence.domain.descriptor.PropertiesDomain;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.projection.EntityPersistenceHelper;
import cc.alcina.framework.entity.projection.GraphProjection.GraphProjectionContext;
import cc.alcina.framework.entity.projection.GraphWalker;
import cc.alcina.framework.entity.transform.policy.TransformPropagationPolicy;
import cc.alcina.framework.entity.util.SqlUtils;
import cc.alcina.framework.servlet.schedule.ServerTask;

public class TaskCleanWrappedObjects
		extends ServerTask<TaskCleanWrappedObjects> {
	private transient State state;

	private transient Map<Long, Long> contentDefinitionPublication = new LinkedHashMap<>();

	private transient Map<Long, Long> deliveryModelPublication = new LinkedHashMap<>();

	private transient Statement statement;

	private transient Set<String> unknownClassNames = new LinkedHashSet<>();

	private boolean resetMaxCleanedId = true;

	private String publicationTableName;

	private String wrappedObjectTableName;

	public boolean isResetMaxCleanedId() {
		return this.resetMaxCleanedId;
	}

	public void setResetMaxCleanedId(boolean resetMaxCleanedId) {
		this.resetMaxCleanedId = resetMaxCleanedId;
	}

	private boolean convertProperties(WrappedObject wrappedObject) {
		Class clazz = null;
		try {
			clazz = Reflections.forName(wrappedObject.getClassName());
		} catch (Exception e) {
			unknownClassNames.add(wrappedObject.getClassName());
			return false;
		}
		long id = wrappedObject.getId();
		if (UserPropertyPersistable.class.isAssignableFrom(clazz)) {
			// non-optimised but not fussed. This forces a locked property
			// storage conversion
			UserPropertyPersistable persistable = PropertiesDomain.get()
					.getProperties(clazz);
			Preconditions.checkState(persistable.getUserPropertySupport()
					.getProperty().domain().wasPersisted());
			return true;
		}
		return false;
	}

	private void updateSerializable(TreeSerializable serializable) {
		new GraphWalker().walk(serializable, null, (context, object) -> {
			if (object instanceof TreeSerializable) {
				TreeSerializable treeSerializable = (TreeSerializable) object;
				Optional<TsUpdater> updater = Registry.optional(TsUpdater.class,
						treeSerializable.getClass());
				if (updater.isPresent()) {
					Registry.optional(TsUpdater.class,
							treeSerializable.getClass());
					updater.get().update(context, treeSerializable);
				}
			}
		});
	}

	protected void loadLookups() throws Exception {
		Class<? extends Publication> implementation = PersistentImpl
				.getImplementation(Publication.class);
		publicationTableName = implementation.getAnnotation(Table.class).name();
		ResultSet rs = statement.executeQuery(Ax.format(
				"select id,contentDefinitionWrapperId,deliveryModelWrapperId from %s",
				publicationTableName));
		while (rs.next()) {
			long id = rs.getLong("id");
			long contentDefinitionWrapperId = rs
					.getLong("contentDefinitionWrapperId");
			long deliveryModelWrapperId = rs.getLong("deliveryModelWrapperId");
			contentDefinitionPublication.put(contentDefinitionWrapperId, id);
			deliveryModelPublication.put(deliveryModelWrapperId, id);
		}
		logger.info("Loaded {} publication ids",
				contentDefinitionPublication.size());
	}

	@Override
	protected void performAction0(TaskCleanWrappedObjects task)
			throws Exception {
		state = UserProperty.ensure(State.class).deserialize();
		Connection conn = Registry.impl(DataSourceAdapter.class)
				.getConnection();
		statement = conn.createStatement();
		statement.setFetchSize(50000);
		Class<? extends WrappedObject> implementation = PersistentImpl
				.getImplementation(WrappedObject.class);
		wrappedObjectTableName = implementation.getAnnotation(Table.class)
				.name();
		logger.info("Loading stats");
		if (state.size == 0) {
			state.size = SqlUtils.getValue(statement, Ax
					.format("select count(id) from %s", wrappedObjectTableName),
					long.class);
			state.maxId = SqlUtils.getValue(statement,
					Ax.format("select max(id) from %s", wrappedObjectTableName),
					long.class);
		}
		logger.info("Loading lookups");
		loadLookups();
		logger.info("Beginning run - from: {}; max: {}; size; {}",
				state.maxCleanedId, state.maxId, state.size);
		int sliceSize = 1000;
		if (resetMaxCleanedId) {
			state.maxCleanedId = 0;
		}
		UserProperty.ensure(State.class).serializeObject(state);
		Transaction.commit();
		long cursor = state.maxCleanedId;
		boolean hadNotHandled = false;
		SimpleLoaderTask loader = new SimpleLoaderTask();
		loader.registerStore(DomainStore.writableStore());
		Set<String> notDeserializedWrappedObjectClasses = new TreeSet<>();
		Predicate<DomainTransformEvent> propagationFilter = evt -> {
			Class clazz = evt.getObjectClass();
			return Publication.class.isAssignableFrom(clazz)
					|| WrappedObject.class.isAssignableFrom(clazz);
		};
		LooseContext.set(TransformPropagationPolicy.CONTEXT_PROPAGATION_FILTER,
				propagationFilter);
		while (cursor <= state.maxId) {
			String key = "clean-loop:" + cursor;
			MetricLogging.get().start(key);
			logger.info("Processing [{}-{}]", cursor, cursor + sliceSize);
			Set<WrappedObject> wrappedObjects = CommonPersistenceProvider.get()
					.getCommonPersistence()
					.getWrappedObjects(cursor, cursor + sliceSize).stream()
					.collect(AlcinaCollectors.toLinkedHashSet());
			Iterator<WrappedObject> itr = wrappedObjects.iterator();
			Set<Long> publicationIds = new LinkedHashSet<>();
			Set<WrappedObject> fromPublications = new LinkedHashSet<>();
			Multimap<Long, List<WrappedObject>> wrappedByPublicationId = new Multimap<>();
			while (itr.hasNext()) {
				WrappedObject wrappedObject = itr.next();
				long id = wrappedObject.getId();
				if (convertProperties(wrappedObject)) {
					((Entity) wrappedObject).delete();
					itr.remove();
				} else {
					Long publicationId = contentDefinitionPublication.get(id);
					if (publicationId == null) {
						publicationId = deliveryModelPublication.get(id);
					}
					if (publicationId != null) {
						publicationIds.add(publicationId);
						wrappedByPublicationId.add(publicationId,
								wrappedObject);
					}
				}
			}
			Class<? extends Publication> pubImpl = PersistentImpl
					.getImplementation(Publication.class);
			ClassIdLock lock = LockUtils
					.obtainClassIdLock(TaskCleanWrappedObjects.class, 0);
			String sqlFilter = Ax.format(" id in %s",
					EntityPersistenceHelper.toInClause(publicationIds));
			List<Publication> publications = loader.loadTableTyped(pubImpl,
					sqlFilter, lock);
			Map<Long, Publication> idMap = EntityHelper.toIdMap(publications);
			UnwrapWithExceptionsResult<Publication> unwrapped = CommonPersistenceProvider
					.get().getCommonPersistence()
					.unwrapWithExceptions(publications);
			unwrapped.unwrapped.forEach(pub -> {
				DeliveryModel deliveryModel = pub.getDeliveryModel();
				Preconditions.checkState(deliveryModel != null);
				updateSerializable(deliveryModel);
				idMap.get(pub.getId())
						.setDefinition((Definition) deliveryModel);
				wrappedByPublicationId.get(pub.getId()).forEach(w -> {
					wrappedObjects.remove(w);
					((Entity) w).delete();
				});
			});
			Transaction.commit();
			hadNotHandled |= wrappedObjects.size() > 0;
			if (wrappedObjects.size() > 0) {
				logger.warn("Not handled: {}", wrappedObjects.stream()
						.map(HasId::getId).collect(Collectors.toList()));
			}
			wrappedObjects.stream().forEach(w -> {
				notDeserializedWrappedObjectClasses.add(w.getClassName());
			});
			cursor += sliceSize;
			if (!hadNotHandled) {
				state.maxCleanedId = cursor;
				UserProperty.ensure(State.class).serializeObject(state);
				Transaction.commit();
			}
			MetricLogging.get().end(key);
		}
		Transaction.commit();
		conn.close();
		Ax.out("Not handled deser classes:");
		Ax.out("==========================");
		Ax.out(notDeserializedWrappedObjectClasses);
	}

	public static class State implements TreeSerializable {
		private long maxId;

		private long maxCleanedId;

		private long size;

		public long getMaxCleanedId() {
			return this.maxCleanedId;
		}

		public long getMaxId() {
			return this.maxId;
		}

		public long getSize() {
			return this.size;
		}

		public void setMaxCleanedId(long maxCleanedId) {
			this.maxCleanedId = maxCleanedId;
		}

		public void setMaxId(long maxId) {
			this.maxId = maxId;
		}

		public void setSize(long size) {
			this.size = size;
		}
	}

	@RegistryLocation(registryPoint = TsUpdater.class)
	public static abstract class TsUpdater<TS extends TreeSerializable> {
		public abstract void update(GraphProjectionContext context, TS ts);
	}

	@RegistryLocation(registryPoint = TsUpdater.class, targetClass = EntityCriteriaGroup.class)
	public static class TsUpdater_EntityCriteriaGroup
			extends TsUpdater<EntityCriteriaGroup> {
		@Override
		public void update(GraphProjectionContext context,
				EntityCriteriaGroup ts) {
			if (ts.getClass() == EntityCriteriaGroup.class) {
				EntitySearchDefinition def = context.parent.get();
				CriteriaGroup replacement = Reflections
						.newInstance(def.getClass()).getCriteriaGroups()
						.iterator().next();
				ResourceUtilities.copyBeanProperties(ts, replacement, null,
						false);
				def.getCriteriaGroups().clear();
				def.getCriteriaGroups().add(replacement);
			}
		}
	}
}
