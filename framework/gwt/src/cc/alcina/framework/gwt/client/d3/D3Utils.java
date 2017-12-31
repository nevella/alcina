package cc.alcina.framework.gwt.client.d3;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.ElementRemote;

public class D3Utils {
	private static int svgIdCounter;

	public static native D3Visualisation createVisualisation(
			String visualisationName, JavaScriptObject data)/*-{
															try {
															var visualisation = $wnd.jdm.graphs[visualisationName](data);
															return visualisation;
															} catch (e) {
															debugger;
															throw e;
															}
															
															}-*/;

	public static String nextContainerId() {
		return "d3-svg-container-" + svgIdCounter++;
	}

	public final static class D3Visualisation extends JavaScriptObject {
		protected D3Visualisation() {
		}

		public native void renderGraph(ElementRemote element, int ctrWidth,
				int ctrHeight)/*-{
								try {
								this.render(element, ctrWidth, ctrHeight);
								} catch (e) {
								debugger;
								throw e;
								}
								}-*/;
	}
}
