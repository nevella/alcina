package cc.alcina.template.cs.actions.search;

import java.io.Serializable;

import cc.alcina.framework.common.client.actions.RemoteActionWithParameters;
import cc.alcina.template.cs.misc.search.DomainTransformSearchDefinition;


public class DomainTransformRecordSearchAction extends
		RemoteActionWithParameters<DomainTransformSearchDefinition> implements
		Serializable {
	public DomainTransformRecordSearchAction() {
		setParameters(new DomainTransformSearchDefinition());
	}

	@Override
	public String getDescription() {
		return "";
	}

	@Override
	public String getDisplayName() {
		return "Search domain transforms";
	}
}
