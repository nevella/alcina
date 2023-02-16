package cc.alcina.framework.gwt.client.util;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.HeadElement;
import com.google.gwt.dom.client.LocalDom;
import com.google.gwt.dom.client.ScriptElement;

public class JavascriptInjector {
	private static HeadElement head;

	/*
	 * Because script creation injection can cause dom mutations (the script
	 * runs immediately), run it *outside* localdom
	 */
	public static void inject(String javascript) {
		LocalDom.invokeExternal(() -> inject0(javascript));
	}

	public static void injectExternal(String sourceUrl) {
		HeadElement head = getHead();
		ScriptElement element = createScriptElement();
		element.setSrc(sourceUrl);
		head.appendChild(element);
	}

	public static ScriptElement injectJsonLd(String json) {
		HeadElement head = getHead();
		ScriptElement element = createScriptElement();
		element.setType("application/ld+json");
		element.setText(json);
		head.appendChild(element);
		return element;
	}

	public static void removeScriptElement(ScriptElement element) {
		HeadElement head = getHead();
		head.removeChild(element);
	}

	private static ScriptElement createScriptElement() {
		ScriptElement script = Document.get().createScriptElement();
		script.setAttribute("language", "javascript");
		return script;
	}

	private static HeadElement getHead() {
		if (head == null) {
			Element element = Document.get().getElementsByTagName("head")
					.getItem(0);
			assert element != null : "HTML Head element required";
			HeadElement head = HeadElement.as(element);
			JavascriptInjector.head = head;
		}
		return JavascriptInjector.head;
	}

	private static native void inject0(String javascript) /*-{
    var head = $doc.head;
    var script = $doc.createElement("script");
    script.language = "javascript";
    script.innerText = javascript;
    head.appendChild(script);
	}-*/;
}