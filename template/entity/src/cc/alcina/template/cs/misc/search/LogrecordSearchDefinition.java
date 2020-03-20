package cc.alcina.template.cs.misc.search;

import java.util.Date;

import cc.alcina.framework.common.client.logic.permissions.PermissibleChildClasses;
import cc.alcina.framework.common.client.search.CriterionPropertyNameMapping;
import cc.alcina.framework.common.client.search.CriterionPropertyNameMappings;
import cc.alcina.framework.common.client.search.DateCriterion;
import cc.alcina.framework.common.client.search.DateGroup;
import cc.alcina.framework.common.client.search.EnumCriteriaGroup;
import cc.alcina.framework.common.client.search.SearchCriterion.Direction;
import cc.alcina.framework.common.client.search.SingleTableSearchDefinition;
import cc.alcina.framework.common.client.search.TxtCriteriaGroup;
import cc.alcina.framework.common.client.search.TxtCriterion;
import cc.alcina.template.cs.persistent.LogRecord;

@CriterionPropertyNameMappings( {
		@CriterionPropertyNameMapping(criteriaGroupClass = DateGroup.class, criterionClass = DateCriterion.class, propertyName = "createdOn"),
		@CriterionPropertyNameMapping(criteriaGroupClass = EnumCriteriaGroup.class, criterionClass = LogMessageTypeEnumCriterion.class, propertyName = "componentKey"),
		@CriterionPropertyNameMapping(criteriaGroupClass = TxtCriteriaGroup.class, criterionClass = TxtCriterion.class, propertyName = "text") })
@PermissibleChildClasses( { DateGroup.class, EnumCriteriaGroup.class,
		TxtCriteriaGroup.class })
public class LogrecordSearchDefinition extends
		SingleTableSearchDefinition<LogRecord> {
	@Override
	
	protected void init() {
		setResultClass(LogRecord.class);
		Date now = new Date();
		Date aMonthAgo = new Date(now.getTime() - ((long) 30 * 86400 * 1000));
		getCriteriaGroups().add(new DateGroup(aMonthAgo, now));
		EnumCriteriaGroup ecg = new EnumCriteriaGroup();
		ecg.addCriterion(new LogMessageTypeEnumCriterion("Key", true));
		getCriteriaGroups().add(ecg);
		getCriteriaGroups().add(new TxtCriteriaGroup("Text"));
		setResultsPerPage(50);
		setOrderPropertyName("createdOn");
		setOrderDirection(Direction.DESCENDING);
	}

	@Override
	public String resultEqlPrefix() {
		return "from Logging t ";
	}

	@Override
	public String idEqlPrefix() {
		return "select count (id) from Logging t ";
	}
}
