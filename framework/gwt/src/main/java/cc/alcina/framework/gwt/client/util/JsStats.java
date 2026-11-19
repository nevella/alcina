package cc.alcina.framework.gwt.client.util;

public final class JsStats {
	public static native boolean isStatsAvailable() /*-{
    return !!$stats;
	}-*/;

	public static native boolean logStat(String evtGroup, String type) /*-{
    return !!$stats && $stats({
      moduleName : $moduleName,
      subSystem : "---",
      evtGroup : evtGroup,
      millis : (new Date()).getTime(),
      type : type,
      className : "---",
    });
	}-*/;
}