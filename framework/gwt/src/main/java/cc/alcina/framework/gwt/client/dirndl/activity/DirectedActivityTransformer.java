package cc.alcina.framework.gwt.client.dirndl.activity;

import cc.alcina.framework.common.client.csobjects.Bindable;

public interface DirectedActivityTransformer<DA extends DirectedActivity, B extends Bindable> {
	B transform(DA activity);
}
