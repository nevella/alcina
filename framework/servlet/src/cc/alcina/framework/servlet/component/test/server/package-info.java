@ComponentMeta.Build("Build by running ant in the component directory (parent of this directory)")
@ComponentMeta.Deploy("The component is accessible at /alcina.gwt.test (any devconsole). ")
@ComponentMeta.Test("The component is itself a test - to execute, start, launch AlcinaGwtTestClient.gwt.xml in dev mode (see vscode.devmode.snippet.json) and go to http://127.0.0.1:31009/alcina.gwt.test?gwt.l in a browser. Test output is in the devtools console")
@Feature.Ref(Feature_AlcinaGwtTest.class)
package cc.alcina.framework.servlet.component.test.server;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.servlet.annotation.ComponentMeta;
import cc.alcina.framework.servlet.component.test.Feature_AlcinaGwtTest;
