package cc.alcina.framework.gwt.client.lux;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.gen.SimpleCssResource;

public class LuxModule {
	private static LuxModule instance;

	public static LuxModule get() {
		if (instance == null) {
			instance = new LuxModule();
		}
		return instance;
	}

	Map<String, String> variableDefs = new LinkedHashMap<>();

	public LuxResources resources = GWT.create(LuxResources.class);

	private LuxModule() {
	}

	public String interpolate(String incoming) {
		Set<Entry<String, String>> entrySet = variableDefs.entrySet();
		String css = incoming + "";
		for (Entry<String, String> entry : entrySet) {
			{
				String varDef = Ax.format("var(- %s)", entry.getKey());
				boolean found = css.indexOf(varDef) != -1;
				if (found) {
					css = replace(css, varDef, entry.getValue());
				}
			}
			{
				String varDef = Ax.format("var(-%s)", entry.getKey());
				boolean found = css.indexOf(varDef) != -1;
				if (found) {
					css = replace(css, varDef, entry.getValue());
				}
			}
		}
		return css;
	}

	public void interpolateAndInject(SimpleCssResource cssResource) {
		interpolateAndInject(cssResource.getText());
	}

	public void interpolateAndInject(String css) {
		css = interpolate(css);
		StyleInjector.injectAtEnd(css);
	}

	public void setVariables(String themeProperties) {
		RegExp re = RegExp.compile("\\s*(-.+):\\s*(.+);", "g");
		MatchResult result;
		while ((result = re.exec(themeProperties)) != null) {
			variableDefs.put(result.getGroup(1), result.getGroup(2) + " ");
		}
		boolean needsInterpolation = true;
		while (needsInterpolation) {
			needsInterpolation = false;
			for (Entry<String, String> e : variableDefs.entrySet()) {
				if (e.getValue().contains("var(")) {
					needsInterpolation = true;
					e.setValue(interpolate(e.getValue()));
				}
			}
		}
		interpolateAndInject(resources.luxStyles());
	}

	/*
	 * Edge 14 issue
	 */
	String replace(String source, String from, String to) {
		StringBuilder result = new StringBuilder();
		int idx = 0;
		while (true) {
			int idxTo = source.indexOf(from, idx);
			if (idxTo == -1) {
				result.append(source.substring(idx));
				break;
			}
			result.append(source.substring(idx, idxTo));
			result.append(to);
			idx = idxTo + from.length();
		}
		return result.toString();
	}
}
