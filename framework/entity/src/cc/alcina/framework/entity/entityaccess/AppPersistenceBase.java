package cc.alcina.framework.entity.entityaccess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;

import org.apache.log4j.Logger;

import cc.alcina.framework.common.client.entity.Iid;
import cc.alcina.framework.common.client.logic.FilterCombinator;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.permissions.IGroup;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.permissions.ReadOnlyException;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.domaintransform.ClassrefScanner;
import cc.alcina.framework.entity.domaintransform.ObjectPersistenceHelper;
import cc.alcina.framework.entity.entityaccess.updaters.DbUpdateRunner;
import cc.alcina.framework.entity.logic.AlcinaServerConfig;
import cc.alcina.framework.entity.registry.ClassMetadataCache;
import cc.alcina.framework.entity.registry.RegistryScanner;
import cc.alcina.framework.entity.util.ClasspathScanner.ServletClasspathScanner;

public abstract class AppPersistenceBase<CI extends ClientInstance, U extends IUser, G extends IGroup, IID extends Iid> {
	public static final String PERSISTENCE_TEST = AppPersistenceBase.class
			.getName() + ".PERSISTENCE_TEST";

	public static final String INSTANCE_READ_ONLY = AppPersistenceBase.class
			.getName() + ".INSTANCE_READ_ONLY";

	public static final String METRIC_LOGGER_PATTERN = "[%c{1}:%X{threadId}] %m%n";

	public static void checkNotReadOnly() throws ReadOnlyException {
		if (isInstanceReadOnly()) {
			throw new ReadOnlyException(System.getProperty(INSTANCE_READ_ONLY));
		}
	}

	public static boolean isInstanceReadOnly() {
		return Boolean.getBoolean(INSTANCE_READ_ONLY);
	}

	public static boolean isTest() {
		return Boolean.getBoolean(PERSISTENCE_TEST);
	}

	public static void setInstanceReadOnly(boolean readonly) {
		System.setProperty(INSTANCE_READ_ONLY, String.valueOf(readonly));
	}

	public static void setTest() {
		System.setProperty(PERSISTENCE_TEST, String.valueOf(true));
		Ax.setTest(true);
	}

	protected CommonPersistenceLocal commonPersistence;

	protected ServletClassMetadataCacheProvider classMetadataCacheProvider;

	protected AppPersistenceBase() {
	}

	@SuppressWarnings("unchecked")
	public String createGroupFilter(boolean userMembership,
			FilterCombinator combinator) {
		StringBuffer sb = new StringBuffer();
		if (userMembership) {
			Collection<G> secondaryGroups = (Collection<G>) PermissionsManager
					.get().getUserGroups((PermissionsManager.get().getUser()))
					.values();
			for (G group : secondaryGroups) {
				addCriteria(sb, String.format(" g.id = %s ", group.getId()),
						combinator);
			}
		}
		return sb.toString();
	}

	@SuppressWarnings("unchecked")
	public <A> Set<A> getAll(Class<A> clazz) {
		Query query = getEntityManager()
				.createQuery(String.format("from %s ", clazz.getSimpleName()));
		Registry.impl(JPAImplementation.class).cache(query);
		List results = query.getResultList();
		return new LinkedHashSet<A>(results);
	}

	@SuppressWarnings("unchecked")
	public <A> Set<A> getAllForCreationUser(Class<A> clazz) {
		Query query = getEntityManager()
				.createQuery(String.format("from %s where creationUser=?1 ",
						clazz.getSimpleName()))
				.setParameter(1, PermissionsManager.get().getUser());
		// seems to be throwing transactional cache errors
		// Registry.impl(JPAImplementation.class).cache(query);
		List results = query.getResultList();
		return new LinkedHashSet<A>(results);
	}

	@SuppressWarnings("unchecked")
	public <A> Set<A> getAllForUser(Class<A> clazz) {
		Query query = getEntityManager()
				.createQuery(String.format("from %s where user=?1 ",
						clazz.getSimpleName()))
				.setParameter(1, PermissionsManager.get().getUser());
		// seems to be throwing transactional cache errors
		// Registry.impl(JPAImplementation.class).cache(query);
		List results = query.getResultList();
		return new LinkedHashSet<A>(results);
	}

	public abstract Collection<G> getAllGroups();

	public List<Long> getAllIds(Class clazz) {
		Query query = getEntityManager().createQuery(String.format(
				"select id from %s order by id", clazz.getSimpleName()));
		Registry.impl(JPAImplementation.class).cache(query);
		return query.getResultList();
	}

	@SuppressWarnings("unchecked")
	public Collection<G> getVisibleGroups() {
		Set<G> grps = new HashSet<G>(
				(Collection<? extends G>) PermissionsManager.get()
						.getUserGroups().values());
		String filterEql = createGroupFilter(true, FilterCombinator.OR);
		filterEql = filterEql.isEmpty() ? "" : " OR " + filterEql;
		// get metas
		List<G> visgrps = getEntityManager()
				.createQuery("select distinct g from "
						+ getCommonPersistence()
								.getImplementationSimpleClassName(IGroup.class)
						+ " g left join fetch g.memberUsers "
						+ "where g.id = -1  " + filterEql)
				.getResultList();
		// hydrate children, just in case - no formalised
		for (G group : visgrps) {
			grps.add(group);
			group.getMemberGroups().size();
			group.getMemberOfGroups().size();
			group.getMemberUsers().size();
		}
		return new ArrayList<G>(grps);
	}

	public void
			init(ServletClassMetadataCacheProvider classMetadataCacheProvider)
					throws Exception {
		this.classMetadataCacheProvider = classMetadataCacheProvider;
		initNonDb();
		runDbUpdaters(true);
		scanClassRefs();
		initDb();
	}

	public void runDbUpdaters(boolean preCacheWarmup) throws Exception {
		try {
			// NOTE - the order (cache, dbupdate, ensure objects) may need to be
			// manually
			// changed
			// depends on whether the db update depends on some code which
			// uses the cache...
			// warmupCaches();
			new DbUpdateRunner().run(getEntityManager(), preCacheWarmup);
		} catch (Exception e) {
			Logger.getLogger(AlcinaServerConfig.get().getMainLoggerName())
					.warn("", e);
			e.printStackTrace();
			throw e;
		}
	}

	private ClassMetadataCache getClassInfo(Logger mainLogger)
			throws Exception {
		return classMetadataCacheProvider.getClassInfo(mainLogger, true);
	}

	protected void addCriteria(StringBuffer sb, String string,
			FilterCombinator combinator) {
		if (SEUtilities.isNullOrEmpty(string)) {
			return;
		}
		if (sb.length() != 0) {
			sb.append(" ");
			sb.append(combinator);
		}
		sb.append(" ");
		sb.append(string);
		sb.append(" ");
	}

	protected G createBlankGroup() {
		return (G) getCommonPersistence()
				.getNewImplementationInstance(IGroup.class);
	}

	protected void createSystemGroupsAndUsers() {
		// normally, override
	}

	@SuppressWarnings("unchecked")
	protected List<G> getAllGroupEntities() {
		List<G> resultList = new ArrayList(getEntityManager()
				.createQuery("select distinct g from "
						+ getCommonPersistence()
								.getImplementationSimpleClassName(IGroup.class)
						+ " g " + " left join fetch g.memberGroups mgs "
						+ " left join fetch g.memberOfGroups mogs "
						+ " left join fetch g.memberUsers u"
						+ " left join fetch u.primaryGroup "
						+ " left join fetch u.secondaryGroups")
				.getResultList());
		Set<U> usersInGroups = new LinkedHashSet<U>();
		for (G jg : resultList) {
			usersInGroups.addAll((Collection<? extends U>) jg.getMemberUsers());
		}
		List<U> users = getEntityManager()
				.createQuery("from " + getCommonPersistence()
						.getImplementationSimpleClassName(IUser.class))
				.getResultList();
		G blankGroup = createBlankGroup();
		if (blankGroup != null && blankGroup.getName() != null) {
			Set<U> usersNotInGroups = new LinkedHashSet<U>();
			blankGroup.setMemberUsers(usersNotInGroups);
			for (U ju : users) {
				if (!usersInGroups.contains(ju)) {
					usersNotInGroups.add(ju);
				}
			}
			resultList.add(blankGroup);
		}
		return resultList;
	}

	protected abstract CommonPersistenceLocal getCommonPersistence();

	protected abstract EntityManager getEntityManager();

	protected abstract EntityManagerFactory getEntityManagerFactory();

	protected void initDb() throws Exception {
		createSystemGroupsAndUsers();
		populateEntities();
	}

	protected void initLoggers() {
		// Logger logger = Logger
		// .getLogger(AlcinaServerConfig.get().getMainLoggerName());
		// Layout l = new PatternLayout("%-5p [%c{1}] %m%n");
		// Appender a = new SafeConsoleAppender(l);
		// String mainLoggerAppenderName =
		// AlcinaServerConfig.MAIN_LOGGER_APPENDER;
		// a.setName(mainLoggerAppenderName);
		// if (logger.getAppender(mainLoggerAppenderName) == null) {
		// logger.addAppender(a);
		// }
		// logger.setAdditivity(true);
		//
		// String databaseEventLoggerName = AlcinaServerConfig.get()
		// .getDatabaseEventLoggerName();
		// if (EntityLayerObjects.get().getPersistentLogger() == null) {
		// Logger dbLogger = Logger.getLogger(databaseEventLoggerName);
		// dbLogger.removeAllAppenders();
		// dbLogger.setLevel(Level.INFO);
		// l = new PatternLayout("%-5p [%c{1}] %m%n");
		// a = new DbAppender(l);
		// a.setName(databaseEventLoggerName);
		// dbLogger.addAppender(a);
		// EntityLayerObjects.get().setPersistentLogger(dbLogger);
		// }
	}

	protected void initNonDb() throws Exception {
		initLoggers();
		initServiceImpl();
		scanRegistry();
	}

	protected abstract void initServiceImpl();

	protected void populateEntities() throws Exception {
		// override to populate initial entities -
		// normally do a JVM property check to ensure only once per JVM creation
	}

	protected void scanClassRefs() {
		Logger mainLogger = Logger
				.getLogger(AlcinaServerConfig.get().getMainLoggerName());
		try {
			Registry.impl(JPAImplementation.class).muteClassloaderLogging(true);
			new ClassrefScanner().scan(getClassInfo(mainLogger));
		} catch (Exception e) {
			mainLogger.warn("", e);
		} finally {
			Registry.impl(JPAImplementation.class)
					.muteClassloaderLogging(false);
		}
	}

	protected void scanRegistry() {
		Logger mainLogger = Logger
				.getLogger(AlcinaServerConfig.get().getMainLoggerName());
		try {
			Registry.impl(JPAImplementation.class).muteClassloaderLogging(true);
			new RegistryScanner().scan(getClassInfo(mainLogger),
					new ArrayList<String>(), Registry.get(), "entity-layer");
			Registry.get()
					.registerBootstrapServices(ObjectPersistenceHelper.get());
		} catch (Exception e) {
			mainLogger.warn("", e);
		} finally {
			Registry.impl(JPAImplementation.class)
					.muteClassloaderLogging(false);
		}
	}

	public static class ServletClassMetadataCacheProvider {
		public ClassMetadataCache getClassInfo(Logger mainLogger,
				boolean entityLayer) throws Exception {
			return new ServletClasspathScanner("*", true, false, mainLogger,
					Registry.MARKER_RESOURCE,
					entityLayer ? Arrays.asList(new String[] {})
							: Arrays.asList(new String[] { "WEB-INF/classes",
									"WEB-INF/lib" })).getClasses();
		}
	}
}
