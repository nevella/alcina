package cc.alcina.framework.gwt.client.dirndl.activity;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.gwt.client.place.CategoryNamePlace;

@Reflected
@Registration(DirectedCategoryActivity.class)
public class DirectedCategoryActivity<CNP extends CategoryNamePlace>
		extends DirectedActivity<CNP> {
}
