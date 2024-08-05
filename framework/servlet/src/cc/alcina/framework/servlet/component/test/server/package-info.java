@Build("Build by running ant in the component directory (parent of this directory)")
@Deploy("The component is accessible at /alcina.gwt.test (any devconsole)")
@Feature.Ref(Feature_AlcinaGwtTest.class)
package cc.alcina.framework.servlet.component.test.server;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.servlet.annotation.Build;
import cc.alcina.framework.servlet.annotation.Deploy;
import cc.alcina.framework.servlet.component.test.Feature_AlcinaGwtTest;
