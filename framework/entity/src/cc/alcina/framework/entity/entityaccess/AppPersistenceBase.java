package cc.alcina.framework.entity.entityaccess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;

import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import cc.alcina.framework.common.client.entity.Iid;
import cc.alcina.framework.common.client.logic.FilterCombinator;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.permissions.IGroup;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.permissions.ReadOnlyException;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.domaintransform.ClassrefScanner;
import cc.alcina.framework.entity.logic.AlcinaServerConfig;
import cc.alcina.framework.entity.logic.EntityLayerLocator;
import cc.alcina.framework.entity.registry.RegistryScanner;
import cc.alcina.framework.entity.util.ClasspathScanner.ServletClasspathScanner;
import cc.alcina.framework.entity.util.JaxbUtils;

public abstract class AppPersistenceBase<CI extends ClientInstance, U extends IUser, G extends IGroup, IID extends Iid> {
	public static final String PERSISTENCE_TEST = AppPersistenceBase.class
			.getName() + ".PERSISTENCE_TEST";

	public static final String READ_ONLY = AppPersistenceBase.class.getName()
			+ ".READ_ONLY";

	public static boolean isTest() {
		return Boolean.getBoolean(PERSISTENCE_TEST);
	}

	public static void setTest() {
		System.setProperty(PERSISTENCE_TEST, String.valueOf(true));
	}

	public static boolean isReadOnly() {
		return Boolean.getBoolean(READ_ONLY);
	}

	public static void setReadOnly(boolean readonly) {
		System.setProperty(READ_ONLY, String.valueOf(readonly));
	}

	public static void checkNotReadOnly() throws ReadOnlyException {
		if (isReadOnly()) {
			throw new ReadOnlyException(System.getProperty(READ_ONLY));
		}
	}

	protected CommonPersistenceLocal commonPersistence;

	private Map<String, Date> classInfo;

	protected abstract CommonPersistenceLocal getCommonPersistence();

	protected AppPersistenceBase() {
	}

	protected void initNonDb() throws Exception {
		initLoggers();
		initServiceImpl();
		scanRegistry();
	}

	public void init() throws Exception {
		initNonDb();
		scanClassRefs();
		initDb();
	}

	protected void initDb() throws Exception {
		createSystemGroupsAndUsers();
		populateEntities();
	}

	protected void populateEntities() throws Exception {
		// override to populate initial entities -
		// normally do a JVM property check to ensure only once per JVM creation
	}

	protected void scanRegistry() {
		Logger mainLogger = Logger.getLogger(AlcinaServerConfig.get()
				.getMainLoggerName());
		try {
			EntityLayerLocator.get().jpaImplementation()
					.muteClassloaderLogging(true);
			new RegistryScanner().scan(ensureClassInfo(mainLogger),
					new ArrayList<String>(), Registry.get());
		} catch (Exception e) {
			mainLogger.warn("", e);
		} finally {
			EntityLayerLocator.get().jpaImplementation()
					.muteClassloaderLogging(false);
		}
	}

	private Map<String, Date> ensureClassInfo(Logger mainLogger)
			throws Exception {
		if (classInfo == null) {
			classInfo = new ServletClasspathScanner("*", true, false,
					mainLogger, Registry.MARKER_RESOURCE,
					Arrays.asList(new String[] { "WEB-INF/classes",
							"WEB-INF/lib" })).getClasses();
		}
		return classInfo;
	}

	protected void scanClassRefs() {
		Logger mainLogger = Logger.getLogger(AlcinaServerConfig.get()
				.getMainLoggerName());
		try {
			EntityLayerLocator.get().jpaImplementation()
					.muteClassloaderLogging(true);
			new ClassrefScanner().scan(ensureClassInfo(mainLogger));
		} catch (Exception e) {
			mainLogger.warn("", e);
		} finally {
			EntityLayerLocator.get().jpaImplementation()
					.muteClassloaderLogging(false);
		}
	}

	protected abstract void initServiceImpl();

	protected void createSystemGroupsAndUsers() {
		// normally, override
	}

	protected void initLoggers() {
		Logger logger = Logger.getLogger(AlcinaServerConfig.get()
				.getMainLoggerName());
		Layout l = new PatternLayout("%-5p [%c{1}] %m%n");
		Appender a = new ConsoleAppender(l);
		String mainLoggerAppenderName = AlcinaServerConfig.MAIN_LOGGER_APPENDER;
		a.setName(mainLoggerAppenderName);
		if (logger.getAppender(mainLoggerAppenderName) == null) {
			logger.addAppender(a);
		}
		logger.setAdditivity(true);
		String metricLoggerName = AlcinaServerConfig.get()
				.getMetricLoggerName();
		if (metricLoggerName != null) {
			Logger metricLogger = Logger.getLogger(metricLoggerName);
			metricLogger.removeAllAppenders();
			metricLogger.addAppender(new ConsoleAppender(
					MetricLogging.METRIC_LAYOUT));
			metricLogger.setLevel(Level.DEBUG);
			metricLogger.setAdditivity(false);
			MetricLogging.metricLogger = metricLogger;
			EntityLayerLocator.get().setMetricLogger(metricLogger);
		}
		String databaseEventLoggerName = AlcinaServerConfig.get()
				.getDatabaseEventLoggerName();
		if (EntityLayerLocator.get().getPersistentLogger() == null) {
			Logger dbLogger = Logger.getLogger(databaseEventLoggerName);
			dbLogger.removeAllAppenders();
			dbLogger.setLevel(Level.INFO);
			l = new PatternLayout("%-5p [%c{1}] %m%n");
			a = new DbAppender(l);
			a.setName(databaseEventLoggerName);
			dbLogger.addAppender(a);
			EntityLayerLocator.get().setPersistentLogger(dbLogger);
		}
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
	public Collection<G> getVisibleGroups() {
		Set<G> grps = new HashSet<G>(
				(Collection<? extends G>) PermissionsManager.get()
						.getUserGroups().values());
		String filterEql = createGroupFilter(true, FilterCombinator.OR);
		filterEql = filterEql.isEmpty() ? "" : " OR " + filterEql;
		// get metas
		List<G> visgrps = getEntityManager().createQuery(
				"select distinct g from "
						+ getCommonPersistence()
								.getImplementationSimpleClassName(IGroup.class)
						+ " g left join fetch g.memberUsers "
						+ "where g.id = -1  " + filterEql).getResultList();
		// hydrate children, just in case - no formalised
		for (G group : visgrps) {
			grps.add(group);
			group.getMemberGroups().size();
			group.getMemberOfGroups().size();
			group.getMemberUsers().size();
		}
		return new ArrayList<G>(grps);
	}

	@SuppressWarnings("unchecked")
	protected List<G> getAllGroupEntities() {
		List<G> resultList = new ArrayList(getEntityManager().createQuery(
				"select distinct g from "
						+ getCommonPersistence()
								.getImplementationSimpleClassName(IGroup.class)
						+ " g " + " left join fetch g.memberGroups mgs "
						+ " left join fetch g.memberOfGroups mogs "
						+ " left join fetch g.memberUsers u"
						+ " left join fetch u.primaryGroup "
						+ " left join fetch u.secondaryGroups").getResultList());
		Set<U> usersInGroups = new LinkedHashSet<U>();
		for (G jg : resultList) {
			usersInGroups.addAll((Collection<? extends U>) jg.getMemberUsers());
		}
		List<U> users = getEntityManager().createQuery(
				"from "
						+ getCommonPersistence()
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

	protected G createBlankGroup() {
		return (G) getCommonPersistence().getNewImplementationInstance(
				IGroup.class);
	}

	public abstract Collection<G> getAllGroups();

	@SuppressWarnings("unchecked")
	public <A> Set<A> getAll(Class<A> clazz) {
		Query query = getEntityManager().createQuery(
				String.format("from %s ", clazz.getSimpleName()));
		EntityLayerLocator.get().jpaImplementation().cache(query);
		List results = query.getResultList();
		return new LinkedHashSet<A>(results);
	}

	@SuppressWarnings("unchecked")
	public <A> Set<A> getAllForUser(Class<A> clazz) {
		Query query = getEntityManager().createQuery(
				String.format("from %s where user=? ", clazz.getSimpleName()))
				.setParameter(1, PermissionsManager.get().getUser());
		// seems to be throwing transactional cache errors
		// EntityLayerLocator.get().jpaImplementation().cache(query);
		List results = query.getResultList();
		return new LinkedHashSet<A>(results);
	}

	@SuppressWarnings("unchecked")
	public <A> Set<A> getAllForCreationUser(Class<A> clazz) {
		Query query = getEntityManager().createQuery(
				String.format("from %s where creationUser=? ",
						clazz.getSimpleName())).setParameter(1,
				PermissionsManager.get().getUser());
		// seems to be throwing transactional cache errors
		// EntityLayerLocator.get().jpaImplementation().cache(query);
		List results = query.getResultList();
		return new LinkedHashSet<A>(results);
	}

	public List<Long> getAllIds(Class clazz) {
		Query query = getEntityManager().createQuery(
				String.format("select id from %s order by id",
						clazz.getSimpleName()));
		EntityLayerLocator.get().jpaImplementation().cache(query);
		return query.getResultList();
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

	protected abstract EntityManager getEntityManager();

	protected abstract EntityManagerFactory getEntityManagerFactory();

	public void destroy() {
		try {
			Registry.get().appShutdown();
			EntityLayerLocator.get().appShutdown();
			JaxbUtils.appShutdown();
		} catch (Exception e) {
			// squelch, JBoss being frenzied
		}
	}
}
