package cc.alcina.framework.gwt.client.util;

public class TextUtilsImpl {
	public static native String normalise(String text)/*-{
		if ($wnd.global_ws_re==null){
		$wnd.global_ws_re=/[\u0009\n\u000B\u000C\r\u0020\u00A0]+/g;
		}
		return  text.replace($wnd.global_ws_re," ");
	}-*/;
}
