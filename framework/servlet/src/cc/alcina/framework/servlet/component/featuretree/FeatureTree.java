package cc.alcina.framework.servlet.component.featuretree;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Click;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.module.support.login.pub.LoginPlace;
import cc.alcina.framework.servlet.dom.Environment;
import cc.alcina.framework.servlet.dom.PathrefDom;
import cc.alcina.framework.servlet.dom.PathrefDom.Credentials;
import cc.alcina.framework.servlet.dom.RemoteUi;

/**
 * A remote component that models the jvm-visible feature tree
 *
 * @author nick@alcina.cc
 *
 */
@Feature.Ref(Feature_FeatureTree.class)
public class FeatureTree {
	Logger logger = LoggerFactory.getLogger(getClass());

	public void start() {
		Credentials credentials = new PathrefDom.Credentials("tree", "tree");
		Environment environment = PathrefDom.get().register(new Ui(),
				credentials);
		logger.info("http://127.0.0.1:31002/remote/c/{}/{}", credentials.id,
				credentials.auth);
	}

	@Directed(receives = DomEvents.Click.class)
	@Bean(PropertySource.FIELDS)
	class Mock extends Model implements DomEvents.Click.Handler {
		@Directed(tag = "p")
		// @Directed(tag = "div")
		String m1 = "baa";

		@Directed(
			tag = "a",
			bindings = @Binding(
				type = Binding.Type.PROPERTY,
				to = "href",
				literal = "#mm"))
		String m2 = "maa";

		@Override
		public void onClick(Click event) {
			set("m1", "lamb");
			new LoginPlace().go();
		}
	}

	class Ui implements RemoteUi {
		@Override
		public void init() {
		}

		@Override
		public void render() {
			new DirectedLayout().render(new Mock()).appendToRoot();
		}
	}
}
