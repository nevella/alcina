package cc.alcina.framework.gwt.client.util;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.util.IntPair;

import com.google.gwt.core.client.JavaScriptObject;

public class TextUtilsImpl {
    public static native String normalizeWhitespace(String text)/*-{
    if (text == null) {
      return text;
    }
    if ($wnd.global_ws_re == null) {
      $wnd.global_ws_re = /[\u0009\n\u000B\u000C\r\u0020\u00A0]+/g;
    }
    return text.replace($wnd.global_ws_re, " ");
    }-*/;

    /**
     * Declares that regular expressions should be matched across line borders.
     */
    public final static int MULTILINE = 1;

    /**
     * Declares that characters are matched reglardless of case.
     */
    public final static int CASE_INSENSITIVE = 2;

    private static native JavaScriptObject _createExpression(String pattern,
            String flags)
    /*-{
    return new RegExp(pattern, flags);
    }-*/;

    private static JavaScriptObject createExpression(String pattern,
            int flags) {
        String sFlags = "g";
        if ((flags & MULTILINE) != 0)
            sFlags += "m";
        if ((flags & CASE_INSENSITIVE) != 0)
            sFlags += "i";
        return _createExpression(pattern, sFlags);
    }

    public static List<IntPair> match(String text, String regex) {
        List<IntPair> matches = new ArrayList<IntPair>();
        JavaScriptObject regExp = createExpression(regex, CASE_INSENSITIVE);
        _match(regExp, text, matches);
        return matches;
    }

    private static native void _match(JavaScriptObject regExp, String text,
            List matches)/*-{
    var result = text.match(regExp);
    if (result == null) {
      return;
    }
    var x = 0;
    for (var i = 0; i < result.length; i++) {
      var str = result[i];
      var start = text.indexOf(str, x);
      x = start + str.length;
      var pair = @cc.alcina.framework.common.client.util.IntPair::new(II)(start,x);
      matches.@java.util.ArrayList::add(Ljava/lang/Object;)(pair);
    }
    }-*/;
}
