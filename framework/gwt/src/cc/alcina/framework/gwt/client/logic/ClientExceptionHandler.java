package cc.alcina.framework.gwt.client.logic;

import cc.alcina.framework.common.client.csobjects.WebException;
import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.client.browsermod.BrowserMod;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;
import com.google.gwt.user.client.History;

public class ClientExceptionHandler implements UncaughtExceptionHandler {
	protected String getStandardErrorText() {
		return "Sorry for the inconvenience, and we'll fix this problem as soon as possible."
				+ ""
				+ " If the problem recurs, please try refreshing your browser";
	}

	protected Throwable possiblyWrapJavascriptException(Throwable e) {
		if (e instanceof JavaScriptException) {
			JavaScriptException je = (JavaScriptException) e;
			String errorText = je.getMessage();
			// if (BrowserMod.isChrome()){
			// errorText+="\n\nStacktrace: "+je.get
			// }
			errorText += extraInfoForExceptionText();
			e = new WebException("(Wrapped javascript exception) : "
					+ errorText);
		}
		return e;
	}
	/*called at points in code where the exception's not actually uncaught,
	 * but might as well give to centralised "arggh" handler
	 */
	
	public void handleException(Throwable e){
		onUncaughtException(e);
	}
	public void onUncaughtException(Throwable e) {
		// TODO - 3.02
		GWT.log("Uncaught exception escaped", e);
		if (GWT.isScript()) {
			ClientLayerLocator.get().notifications().showError(e);
		}
	}
	public  String extraInfoForExceptionText() {
		return "\n\nUser agent: " + BrowserMod.getUserAgent()
				+ "\n\nHistory token: " + History.getToken();
	}

}
