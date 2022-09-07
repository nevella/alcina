package cc.alcina.framework.entity.impl.jboss;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import org.hibernate.Criteria;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CriteriaImpl;
import org.hibernate.loader.OuterJoinLoader;
import org.hibernate.loader.criteria.CriteriaLoader;
import org.hibernate.loader.criteria.CriteriaQueryTranslator;
import org.hibernate.persister.entity.OuterJoinLoadable;

import cc.alcina.framework.common.client.logic.domain.Entity;

public class HibernateCriteriaToSql {
	public static String toSql(Criteria criteria) {
		String sql = "";
		Object[] parameters = null;
		try {
			CriteriaImpl criteriaImpl = (CriteriaImpl) criteria;
			Method getSessionMethod = Arrays
					.stream(criteriaImpl.getClass().getMethods())
					.filter(m -> m.getName().equals("getSession")).findFirst()
					.get();
			Object sessionImpl = getSessionMethod.invoke(criteriaImpl);
			Method getSessionFactoryMethod = Arrays
					.stream(sessionImpl.getClass().getMethods())
					.filter(m -> m.getName().equals("getSessionFactory"))
					.findFirst().get();
			SessionFactoryImplementor factory = (SessionFactoryImplementor) getSessionFactoryMethod
					.invoke(sessionImpl);
			String[] implementors = factory
					.getImplementors(criteriaImpl.getEntityOrClassName());
			OuterJoinLoadable persister = (OuterJoinLoadable) factory
					.getEntityPersister(implementors[0]);
			LoadQueryInfluencers loadQueryInfluencers = new LoadQueryInfluencers();
			CriteriaLoader loader = new CriteriaLoader(persister, factory,
					criteriaImpl, implementors[0].toString(),
					loadQueryInfluencers);
			Field f = OuterJoinLoader.class.getDeclaredField("sql");
			f.setAccessible(true);
			sql = (String) f.get(loader);
			Field fp = CriteriaLoader.class.getDeclaredField("translator");
			fp.setAccessible(true);
			CriteriaQueryTranslator translator = (CriteriaQueryTranslator) fp
					.get(loader);
			parameters = translator.getQueryParameters()
					.getPositionalParameterValues();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		if (sql != null) {
			int fromPosition = sql.indexOf(" from ");
			sql = "\nSELECT * " + sql.substring(fromPosition);
			if (parameters != null && parameters.length > 0) {
				for (Object val : parameters) {
					String value = "%";
					if (val instanceof Boolean) {
						value = ((Boolean) val) ? "1" : "0";
					} else if (val instanceof String) {
						value = "'" + val + "'";
					} else if (val instanceof Number) {
						value = val.toString();
					} else if (val instanceof Class) {
						value = "'" + ((Class) val).getCanonicalName() + "'";
					} else if (val instanceof Date) {
						SimpleDateFormat sdf = new SimpleDateFormat(
								"yyyy-MM-dd HH:mm:ss.SSS");
						value = "'" + sdf.format((Date) val) + "'";
					} else if (val instanceof Enum) {
						value = "" + ((Enum) val).ordinal();
					} else if (val instanceof Entity) {
						value = "" + ((Entity) val).getId();
					} else {
						value = val.toString();
					}
					sql = sql.replaceFirst("\\?", value);
				}
			}
		}
		return sql.replaceAll("left outer join", "\nleft outer join")
				.replaceAll(" and ", "\nand ").replaceAll(" on ", "\non ")
				.replaceAll("<>", "!=").replaceAll("<", " < ")
				.replaceAll(">", " > ");
	}
}
