package cc.alcina.framework.servlet.environment.style;

import java.util.Arrays;
import java.util.List;

import com.helger.css.ECSSVersion;
import com.helger.css.decl.CSSMediaRule;
import com.helger.css.decl.CSSSelectorSimpleMember;
import com.helger.css.decl.CSSStyleRule;
import com.helger.css.decl.CascadingStyleSheet;
import com.helger.css.decl.ICSSSelectorMember;
import com.helger.css.decl.visit.CSSVisitor;
import com.helger.css.decl.visit.DefaultCSSVisitor;
import com.helger.css.decl.visit.ICSSVisitor;
import com.helger.css.reader.CSSReader;
import com.helger.css.reader.CSSReaderSettings;
import com.helger.css.writer.CSSWriter;
import com.helger.css.writer.CSSWriterSettings;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.Registration.Priority;
import cc.alcina.framework.common.client.util.Ax;

/**
 * Scopes a style sheet to a given prefix (by prefixing most selectors, and
 * rewriting body, html and *)
 */
@Registration(StyleScoper.class)
public interface StyleScoper {
	String scope(String baseCss, String styleScope);

	public static class Naive implements StyleScoper {
		@Override
		public String scope(String baseCss, String styleScope) {
			if (Ax.isBlank(baseCss)) {
				return baseCss;
			} else {
				return Ax.format("%s %s", styleScope, baseCss);
			}
		}
	}

	@Registration(
		value = StyleScoper.class,
		priority = Priority.PREFERRED_LIBRARY)
	public static class PhCss implements StyleScoper {
		@Override
		public String scope(String cssText, String styleScope) {
			if (Ax.isBlank(cssText)) {
				return cssText;
			}
			StringBuilder out = new StringBuilder();
			List<String> rules = Arrays.asList(cssText.split("\\}"));
			for (String rule : rules) {
				if (rule.contains(":has") || rule.contains("*[dca-column]")
						|| rule.contains(
								".decorated-area > glosses > content-annotations")
						|| rule.contains("background-image")) {
				} else {
					out.append(rule);
					out.append("}\n");
				}
			}
			cssText = out.toString();
			CSSReaderSettings rSettings = new CSSReaderSettings();
			CascadingStyleSheet css = CSSReader.readFromString(cssText,
					ECSSVersion.CSS30);
			ICSSVisitor visitor = new DefaultCSSVisitor() {
				@Override
				public void onBeginMediaRule(CSSMediaRule aMediaRule) {
					css.removeRule(aMediaRule);
				}

				@Override
				public void onBeginStyleRule(CSSStyleRule rule) {
					rule.getAllSelectors().forEach(sel -> {
						ICSSSelectorMember aMember = new CSSSelectorSimpleMember(
								styleScope);
						sel.addMember(0, aMember);
					});
				}
			};
			CSSVisitor.visitCSS(css, visitor);
			CSSWriterSettings wSettings = new CSSWriterSettings(
					ECSSVersion.CSS30, false);
			CSSWriter aWriter = new CSSWriter(wSettings);
			String sCSSCode = aWriter.getCSSAsString(css);
			return sCSSCode;
		}
	}
}
