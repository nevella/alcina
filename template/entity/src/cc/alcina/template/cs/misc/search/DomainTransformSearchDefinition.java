package cc.alcina.template.cs.misc.search;

import java.util.Date;

import cc.alcina.framework.common.client.logic.permissions.PermissibleChildClasses;
import cc.alcina.framework.common.client.search.CriterionPropertyNameMapping;
import cc.alcina.framework.common.client.search.CriterionPropertyNameMappings;
import cc.alcina.framework.common.client.search.DateCriterion;
import cc.alcina.framework.common.client.search.DateGroup;
import cc.alcina.framework.common.client.search.LongCriteriaGroup;
import cc.alcina.framework.common.client.search.LongCriterion;
import cc.alcina.framework.common.client.search.PersistentObjectCriteriaGroup;
import cc.alcina.framework.common.client.search.PersistentObjectCriterion;
import cc.alcina.framework.common.client.search.SingleTableSearchDefinition;
import cc.alcina.framework.common.client.search.TxtCriteriaGroup;
import cc.alcina.framework.common.client.search.TxtCriterion;
import cc.alcina.framework.common.client.search.SearchCriterion.Direction;
import cc.alcina.framework.common.client.search.TxtCriteriaGroup.TxtCriteriaGroup2;
import cc.alcina.framework.common.client.search.TxtCriterion.TxtCriterionType;

@CriterionPropertyNameMappings( {
		@CriterionPropertyNameMapping(criteriaGroupClass = DateGroup.class, criterionClass = DateCriterion.class, propertyName = "utcDate"),
		@CriterionPropertyNameMapping(criteriaGroupClass = UserCriteriaGroup.class, criterionClass = UserCriterion.class, propertyName = "t.user"),
		@CriterionPropertyNameMapping(criteriaGroupClass = TxtCriteriaGroup.class, criterionClass = TxtCriterion.class, propertyName = "propertyName"),
		@CriterionPropertyNameMapping(criteriaGroupClass = TxtCriteriaGroup2.class, criterionClass = TxtCriterion.class, propertyName = "newStringValue"),
		@CriterionPropertyNameMapping(criteriaGroupClass = LongCriteriaGroup.class, criterionClass = LongCriterion.class, propertyName = "objectId"),
		@CriterionPropertyNameMapping(criteriaGroupClass = PersistentObjectCriteriaGroup.class, criterionClass = PersistentObjectCriterion.class, propertyName = "objectClassRef") })
@PermissibleChildClasses( { DateGroup.class, UserCriteriaGroup.class,
		TxtCriteriaGroup.class, LongCriteriaGroup.class,
		TxtCriteriaGroup2.class, PersistentObjectCriteriaGroup.class })
public class DomainTransformSearchDefinition extends
		SingleTableSearchDefinition<DomainTransformEventInfo> {
	@Override
	protected void init() {
		setResultClass(DomainTransformEventInfo.class);
		Date now = new Date();
		Date aMonthAgo = new Date(now.getTime() - ((long) 30 * 86400 * 1000));
		getCriteriaGroups().add(new DateGroup(aMonthAgo, now));
		UserCriteriaGroup jucg = new UserCriteriaGroup();
		jucg.addCriterion(new UserCriterion("User"));
		getCriteriaGroups().add(jucg);
		TxtCriteriaGroup tcg = new TxtCriteriaGroup("Property name");
		((TxtCriterion) tcg.soleCriterion())
				.setTxtCriterionType(TxtCriterionType.EQUALS);
		getCriteriaGroups().add(tcg);
		LongCriteriaGroup lcg = new LongCriteriaGroup("Object id");
		getCriteriaGroups().add(lcg);
		tcg = new TxtCriteriaGroup2("New value");
		getCriteriaGroups().add(tcg);
		PersistentObjectCriteriaGroup pocg = new PersistentObjectCriteriaGroup();
		pocg.addCriterion(new PersistentObjectCriterion("Object type"));
		getCriteriaGroups().add(pocg);
		setResultsPerPage(50);
		setOrderPropertyName("id");
		setOrderDirection(Direction.DESCENDING);
	}

	@Override
	public String resultEqlPrefix() {
		return "from DomainTransformEventPersistentImpl t left join fetch  t.user t1 ";
	}

	@Override
	public String idEqlPrefix() {
		return "select count (id) from DomainTransformEventPersistentImpl t ";
	}
}
