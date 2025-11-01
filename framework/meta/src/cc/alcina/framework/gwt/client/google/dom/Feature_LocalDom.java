package cc.alcina.framework.gwt.client.google.dom;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.meta.Feature_Ui_support;

/**
 * See the <code>com.google.gwt.dom.client</code> package javadoc (in
 * alcina/jsdom)
 */
@Feature.Status.Ref(Feature.Status.Open.class)
@Feature.Parent(Feature_Ui_support.class)
public interface Feature_LocalDom extends Feature {
	/**
	 * <p>
	 * To handle (say) an injected react component writing multiple consecutive
	 * text nodes, browser markup is structure-normalised (blank text nodes
	 * removed, text nodes collated) *except* in script/style tags (which would
	 * cause reflows) before to-local sync
	 * 
	 * <p>
	 * This is imperfect - better would be to use the IdProtocol from browser to
	 * local - but that's significantly more work
	 */
	public interface _ExactFromBrowser extends Feature {
	}
}
