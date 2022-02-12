package cc.alcina.framework.entity.impl.jboss;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Junction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.sql.JoinType;

import cc.alcina.framework.common.client.logic.FilterCombinator;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.search.CriteriaGroup;
import cc.alcina.framework.common.client.search.OrderCriterion;
import cc.alcina.framework.common.client.search.OrderGroup;
import cc.alcina.framework.common.client.search.SearchCriterion;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.search.VTCriterion;

@SuppressWarnings({ "unchecked" })
public abstract class HibernateEJBSearcherBase {
	protected Map<Class<? extends SearchCriterion>, SearchCriterionHandler> handlerMap;

	protected LinkedHashSet<Class> linkedEntities = new LinkedHashSet<Class>();

	protected Map<Class, Criteria> classCriteriaMap = new HashMap<Class, Criteria>();

	protected Map<Class, DetachedCriteria> detachedCriteriaMap = new HashMap<Class, DetachedCriteria>();

	protected Map<Class, VTHandler> vtHandlerMap = new HashMap<Class, VTHandler>();

	protected SearchDefinition def;

	protected boolean allowHandler(SearchCriterionHandler handler) {
		return true;
	}

	protected void conditionalCreate(Class srcClass, Class tgtClass,
			String propertyName, String alias) {
		conditionalCreate(srcClass, tgtClass, propertyName, alias,
				JoinType.INNER_JOIN);
	}

	protected void conditionalCreate(Class srcClass, Class tgtClass,
			String propertyName, String alias, JoinType joinType) {
		if (classCriteriaMap.containsKey(srcClass)
				&& !classCriteriaMap.containsKey(tgtClass)
				&& !detachedCriteriaMap.containsKey(tgtClass)) {
			Criteria criteria = classCriteriaMap.get(srcClass);
			if (VTCriterion.class.isAssignableFrom(tgtClass)) {
				VTHandler vtHandler = Registry.query(VTHandler.class)
						.addKeys(tgtClass).impl();
				DetachedCriteria detachedCriteria = vtHandler
						.createDetachedCriteria(this);
				detachedCriteriaMap.put(tgtClass, detachedCriteria);
				vtHandlerMap.put(tgtClass, vtHandler);
				vtHandler.prepareLink(criteria, detachedCriteria);
			} else {
				Criteria criteria2 = criteria.createCriteria(propertyName,
						alias, joinType);
				classCriteriaMap.put(tgtClass, criteria2);
			}
		}
	}

	protected SearchCriterionHandler getCriterionHandler(SearchCriterion sc) {
		return handlerMap.get(sc.getClass());
	}

	protected Class getEntityClass(CriteriaGroup cg) {
		return cg.entityClass();
	}

	protected boolean hasCriterion(Set<CriteriaGroup> criteriaGroups,
			Class<? extends SearchCriterion> clazz) {
		for (CriteriaGroup group : criteriaGroups) {
			Set<SearchCriterion> criteria = group.getCriteria();
			for (SearchCriterion searchCriterion : criteria) {
				if (searchCriterion.getClass() == clazz) {
					return true;
				}
			}
		}
		return false;
	}

	protected boolean hasOrderCriterion(Set<OrderGroup> orderGroups,
			Class<? extends OrderCriterion> clazz) {
		for (OrderGroup orderGroup : orderGroups) {
			if (orderGroup.getSoleCriterion() != null
					&& orderGroup.getSoleCriterion().getClass() == clazz) {
				return true;
			}
		}
		return false;
	}

	protected void processCriteriaAndOrder(SearchDefinition def) {
		Set<CriteriaGroup> criteriaGroups = def.getCriteriaGroups();
		Set<OrderGroup> orderGroups = def.getOrderGroups();
		for (CriteriaGroup cg : criteriaGroups) {
			if (!PermissionsManager.get().isPermitted(cg)) {
				continue;
			}
			if (!cg.provideIsEmpty() && getEntityClass(cg) != null) {
				linkedEntities.add(getEntityClass(cg));
			}
		}
		for (OrderGroup og : orderGroups) {
			if (!og.provideIsEmpty() && getEntityClass(og) != null) {
				linkedEntities.add(getEntityClass(og));
			}
		}
	}

	protected void processHandlers(SearchDefinition def) {
		Set<CriteriaGroup> criteriaGroups = def.getCriteriaGroups();
		for (CriteriaGroup cg : criteriaGroups) {
			if (!PermissionsManager.get().isPermitted(cg)) {
				continue;
			}
			if (!cg.provideIsEmpty()) {
				boolean countingVt = detachedCriteriaMap
						.containsKey(getEntityClass(cg));
				Junction junction = cg.getCombinator() == FilterCombinator.OR
						|| countingVt ? Restrictions.disjunction()
								: Restrictions.conjunction();
				boolean added = false;
				for (SearchCriterion sc : (Set<SearchCriterion>) cg
						.getCriteria()) {
					SearchCriterionHandler handler = getCriterionHandler(sc);
					if (!allowHandler(handler)) {
						continue;
					}
					if (handler == null) {
						System.err.println(
								"No handler for class " + sc.getClass());
					}
					Criterion criterion = handler.handle(sc);
					if (criterion != null) {
						junction.add(handler.handle(sc));
						added = true;
					}
				}
				if (!detachedCriteriaMap.containsKey(getEntityClass(cg))) {
					if (added) {
						classCriteriaMap.get(getEntityClass(cg)).add(junction);
					}
				} else {
					detachedCriteriaMap.get(getEntityClass(cg)).add(junction);
					vtHandlerMap.get(getEntityClass(cg)).link(cg);
				}
			}
		}
	}

	protected void register(Class<? extends SearchCriterion> clazz,
			SearchCriterionHandler criterionHandler) {
		handlerMap.put(clazz, criterionHandler);
	}
}
