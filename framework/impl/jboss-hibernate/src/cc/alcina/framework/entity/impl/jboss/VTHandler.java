package cc.alcina.framework.entity.impl.jboss;

import org.hibernate.Criteria;
import org.hibernate.criterion.DetachedCriteria;

import cc.alcina.framework.common.client.search.CriteriaGroup;

public interface VTHandler {
	public DetachedCriteria createDetachedCriteria();
	public void prepareLink(Criteria toCriteria, DetachedCriteria detachedCriteria);
	public void link(CriteriaGroup cg);
}
