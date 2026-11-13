package cc.alcina.framework.gwtpeer.client.logic.domaintransform.lookup;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

public class JavascriptObjectUtils {
	public static <T extends JavaScriptObject> List<T>
			jsArrayToTypedArray(JsArray<T> typedArray) {
		List<T> result = new ArrayList<T>();
		for (int i = 0; i < typedArray.length(); i++) {
			result.add(typedArray.get(i));
		}
		return result;
	}
}
