package cc.alcina.framework.gwt.client.util;

public class TextUtilsImpl {
	public static native String normalise(String text)/*-{
		if ($wnd.global_ws_re==null){
		$wnd.global_ws_re=/\u0009\u000A\u000B\u000C\u000D\u0020\u00A0/g;
		}
		return  text.replace($wnd.global_ws_re," ");
	}-*/;
}
