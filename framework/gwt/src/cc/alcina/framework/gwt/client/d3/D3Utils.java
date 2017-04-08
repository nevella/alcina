package cc.alcina.framework.gwt.client.d3;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element_Jso;

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

	public final static class D3Visualisation extends JavaScriptObject {
		protected D3Visualisation() {
		}
		public  void renderGraph(Element_Jso element, int ctrWidth,
				int ctrHeight){
			renderGraph(element, ctrWidth, ctrHeight,null);
		}
		public native void renderGraph(Element_Jso element, int ctrWidth,
				int ctrHeight, JavaScriptObject data)/*-{
            try {
                this.render(element, ctrWidth, ctrHeight, data);
            } catch (e) {
                debugger;
                throw e;
            }
		}-*/;
	}

	public static String nextContainerId() {
		return "d3-svg-container-" + svgIdCounter++;
	}
}
