package cc.alcina.framework.entity.entityaccess;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.domaintransform.ClassrefScanner;
import cc.alcina.framework.entity.logic.AlcinaServerConfig;
import cc.alcina.framework.entity.logic.EntityLayerLocator;
import cc.alcina.framework.entity.registry.RegistryScanner;
import cc.alcina.framework.entity.util.ClasspathScanner.ServletClasspathScanner;

public abstract class AppPersistenceBase<CI extends ClientInstance, U extends IUser, G extends IGroup, IID extends Iid> {
	public static final String PERSISTENCE_TEST = AppPersistenceBase.class
			.getName() + ".PERSISTENCE_TEST";

	public static boolean isTest() {
		return Boolean.getBoolean(PERSISTENCE_TEST);
	}

	public static void setTest() {
		System.setProperty(PERSISTENCE_TEST, String.valueOf(true));
	}

	protected CommonPersistenceLocal commonPersistence;

	protected abstract CommonPersistenceLocal getCommonPersistence();

	protected AppPersistenceBase() {
	}

	protected void loadCustomProperties() {
		try {
			FileInputStream fis = new FileInputStream(AlcinaServerConfig.get()
					.getCustomPropertiesFilePath());
			ResourceUtilities.registerCustomProperties(fis);
		} catch (FileNotFoundException fnfe) {
			// no custom properties
		}
	}
	
	public void init() throws Exception {
		loadCustomProperties();
		initLoggers();
		createSystemGroupsAndUsers();
		initServiceImpl();
		scanRegistryAndClassRefs();
		populateEntities();
	}

	protected void populateEntities() throws Exception {
		// override to populate initial entities -
		// normally do a JVM property check to ensure only once per JVM creation
	}

	protected void scanRegistryAndClassRefs() {
		Logger mainLogger = Logger.getLogger(AlcinaServerConfig.get()
				.getMainLoggerName());
		try {
			EntityLayerLocator.get().jpaImplementation().muteClassloaderLogging(true);
			Map<String, Date> classes = new ServletClasspathScanner("*", true,
					false, mainLogger, Registry.MARKER_RESOURCE,
					Arrays.asList(new String[] { "WEB-INF/classes",
							"WEB-INF/lib" })).getClasses();
			new RegistryScanner().scan(classes, new ArrayList<String>(),
					Registry.get());
			new ClassrefScanner().scan(classes);
		} catch (Exception e) {
			mainLogger.warn("", e);
		}finally{
			EntityLayerLocator.get().jpaImplementation().muteClassloaderLogging(false);
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
		G blankGroup = (G) getCommonPersistence().getNewImplementationInstance(
				IGroup.class);
		Set<U> usersNotInGroups = new LinkedHashSet<U>();
		blankGroup.setMemberUsers(usersNotInGroups);
		for (U ju : users) {
			if (!usersInGroups.contains(ju)) {
				usersNotInGroups.add(ju);
			}
		}
		return resultList;
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
}
