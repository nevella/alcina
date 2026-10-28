package cc.alcina.framework.servlet.transform.fragment.style;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.google.gwt.dom.client.DomStyleConstants;

import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CachingMap;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.MultikeyMap;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.common.client.util.TextUtils;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.SEUtilities;

/*
 * This began as a quick-n-dirty, and should really be replaced by a full css
 * parser. But it's fast, and somewhat customised...
 */
public class QuickMsoStyleParser {
	public static final float PIXELS_PER_POINT = 148F / 106.35F;

	static Map<String, Float> unitPixelMap = new HashMap<String, Float>();

	static Map<String, Float> unitMultiplierMap = new HashMap<String, Float>();

	public static final float NBSP_WIDTH = 4.0F;

	static Set<String> additiveRules = new LinkedHashSet<String>();

	static Set<String> cornerRules = new LinkedHashSet<String>();

	static String[] corners = { "top", "right", "bottom", "left" };
	static {
		unitPixelMap.put("px", 1F);
		unitPixelMap.put("pt", PIXELS_PER_POINT);
		unitPixelMap.put("em", 10F / 0.625F);
		unitPixelMap.put("cm", 39.46F);
		unitPixelMap.put("in", 98.67F);
		unitPixelMap.put("%", 10F / 62.5F);
		unitMultiplierMap.put("%", 0.01f);
		unitMultiplierMap.put("em", 1.0f);
		additiveRules.add("margin-left");
		additiveRules.add("margin-right");
		additiveRules.add("margin-top");
		additiveRules.add("margin-bottom");
		additiveRules.add("padding-left");
		additiveRules.add("padding-right");
		additiveRules.add("padding-top");
		additiveRules.add("padding-bottom");
		// additiveRules.add("text-indent");
		cornerRules.add("margin");
		cornerRules.add("padding");
	}

	static Pattern stylePattern = Pattern.compile("(.+?)\\{(.+?)\\}",
			Pattern.DOTALL);

	static Pattern designatorPattern = Pattern.compile("(.+?)(?:,|\\z)",
			Pattern.DOTALL);

	static Pattern rulePattern = Pattern.compile("(.+?):(.+?)(?:;|\\z)",
			Pattern.DOTALL);

	static Pattern cleanDesignatorPattern = Pattern.compile(
			"(&amp;amp;#60;!--)|(&#60;!--)|(--&#62;)(&.+?;)|(/\\*.+?\\*/)",
			Pattern.DOTALL);

	public static float pointsToPixels(float points) {
		return points * unitPixelMap.get("pt");
	}

	Map<String, Float> unitsPixelMap = new HashMap<String, Float>();

	/*
	 * additive checks are super-simplistic - only use in very specific case
	 */
	private boolean respectAdditiveRules = false;

	Map<String, StringMap> styleStringToRule = new HashMap<String, StringMap>();

	Multimap<String, List<StyleElement>> stylze = new Multimap<String, List<StyleElement>>();

	private String[] automatchSelectorSplits = new String[0];

	StringMap lcMap = new StringMap();

	StringMap normalisedWsMap = new StringMap();

	private boolean elementOnly;

	public QuickMsoStyleParser() {
	}

	/*
	 * Returns lower (earlier) for more dots...
	 */
	static class ClassSpecificityOrdering implements Comparator<String> {
		CachingMap<String, Integer> specificity = new CachingMap<>(
				s -> s.split("\\.").length);

		@Override
		public int compare(String o1, String o2) {
			return -(specificity.get(o1) - specificity.get(o2));
		}
	}

	CachingMap<String, Set<String>> cnCombinationLookup = new CachingMap<>(
			cnAttr -> {
				Set<String> cnCombinations = new LinkedHashSet<String>();
				if (cnAttr.contains(" ")) {
					String[] cns = cnAttr.split(" ");
					if (cns.length > 6) {
						Ax.out("dev::style-parser-hack %s cns -\n\t%s",
								cns.length, cnAttr);
						return cnCombinations;
					}
					Permute itr = new Permute(cns);
					// hack, ordering ain't exact
					for (String s : cns) {
						cnCombinations.add(s);
					}
					while (itr.hasNext()) {
						String[] next = (String[]) itr.next();
						List<String> asList = Arrays.asList(next);
						for (int i = 1; i <= asList.size(); i++) {
							cnCombinations.add(CommonUtils
									.join(asList.subList(0, i), "."));
						}
					}
					cnCombinations = cnCombinations.stream()
							.sorted(new ClassSpecificityOrdering())
							.collect(AlcinaCollectors.toLinkedHashSet());
				} else {
					cnCombinations.add(cnAttr);
				}
				return cnCombinations;
			});

	Pattern msoListTrue = Pattern.compile("(?:^|\\s+)l\\d+(\\s+|\\z)");

	Pattern italic = Pattern.compile("italic", Pattern.CASE_INSENSITIVE);

	Pattern smallCaps = Pattern.compile("small-caps", Pattern.CASE_INSENSITIVE);

	Pattern normal = Pattern.compile("normal", Pattern.CASE_INSENSITIVE);

	Pattern bold = Pattern.compile("bold", Pattern.CASE_INSENSITIVE);

	Pattern uppercase = Pattern.compile("uppercase", Pattern.CASE_INSENSITIVE);

	Pattern underline = Pattern.compile("underline", Pattern.CASE_INSENSITIVE);

	Pattern strikethrough = Pattern.compile("line-through",
			Pattern.CASE_INSENSITIVE);

	Pattern _super = Pattern.compile("super", Pattern.CASE_INSENSITIVE);

	Pattern sub = Pattern.compile("sub", Pattern.CASE_INSENSITIVE);

	public boolean debug;

	Set<String> runStyles = new HashSet<String>(Arrays
			.asList(new String[] { "font-size", "font-style", "font-weight" }));

	public String automatchSelectors;

	private DomDocument doc;

	private MultikeyMap<String> nodeRuleName = new UnsortedMultikeyMap<>(2);

	public void addStyles(String css, Object source) {
		css = css.replaceAll("(?s)/\\*.+?\\*/", "");
		Matcher styleMatcher = stylePattern.matcher(css);
		while (styleMatcher.find()) {
			String designators = TextUtils
					.normalizeWhitespaceAndTrim(styleMatcher.group(1));
			String styles = styleMatcher.group(2);
			StringMap rules = getStyles(styles);
			Matcher dm = designatorPattern.matcher(designators);
			while (dm.find()) {
				StyleElement e = new StyleElement(normaliseSelector(dm), rules,
						source, styleMatcher, css);
				// normalise footnote indent/outdent
				if (e.key.contains("msofootnotetext")) {
					Set<Entry<String, String>> entrySet = e.rules.entrySet();
					for (Entry<String, String> entry : entrySet) {
						entry.setValue(
								entry.getValue().replace("22.7pt", "36.0pt"));
					}
				}
				e.addToStylze(stylze);
			}
		}
	}

	public boolean areEquivalentRunStyles(String s1, String s2) {
		StringMap m1 = getStyles(s1);
		StringMap m2 = getStyles(s2);
		for (Entry<String, String> entry : m1.entrySet()) {
			String key = entry.getKey();
			if (entry.getValue().equals(m2.get(key))) {
			} else {
				if (nonRunStyle(key)) {
					return false;
				}
			}
		}
		for (Entry<String, String> entry : m2.entrySet()) {
			String key = entry.getKey();
			if (entry.getValue().equals(m1.get(key))) {
			} else {
				if (nonRunStyle(key)) {
					return false;
				}
			}
		}
		return true;
	}

	public boolean areEquivalentStyles(String s1, String s2) {
		StringMap m1 = getStyles(s1);
		StringMap m2 = getStyles(s2);
		for (Entry<String, String> entry : m1.entrySet()) {
			String key = entry.getKey();
			if (entry.getValue().equals(m2.get(key))) {
			} else {
				if (visuallySignificant(key)) {
					return false;
				}
			}
		}
		for (Entry<String, String> entry : m2.entrySet()) {
			String key = entry.getKey();
			if (entry.getValue().equals(m1.get(key))) {
			} else {
				if (visuallySignificant(key)) {
					return false;
				}
			}
		}
		return true;
	}

	public void classesToStyles(DomDocument xd) {
		xd.descendants().filter(DomNode::isElement).map(DomNode::w3cElement)
				.forEach(this::classToStyleWithStyle);
	}

	public void classToStyle(Element elt) {
		classToStyle(elt, false);
	}

	public void classToStyle(Element elt, boolean withStyle) {
		if (!elt.getAttribute("class").isEmpty()
				&& (withStyle || elt.getAttribute("style").isEmpty())) {
			String styleString = getStyleString(elt);
			elt.setAttribute("style", styleString);
		}
	}

	public void classToStyleWithStyle(Element elt) {
		classToStyle(elt, true);
	}

	public String getDefaultBrowserStyles() {
		return Io.read().resource("defaultBrowserStyles.css").asString();
	}

	public String getDefaultStyles() {
		return Io.read().resource("defaultStyles.css").asString();
	}

	public String getDisplay(Node node) {
		return getStringRule(node, DomStyleConstants.STYLE_DISPLAY);
	}

	public float getFirstLineMarginLeft(Node n) {
		return getMarginLeft(n) + getTextIndent(n);
	}

	public float getFontSize(Node n) {
		return getNumericRule(n, "font-size", "100%", true);
	}

	public float getFontSizeNoParent(Node n) {
		return getNumericRule(n, "font-size", "100%", false);
	}

	public float getMarginLeft(Node n) {
		return getNumericRule(n, new String[] { "margin", "margin-left" });
	}

	public float getCumulativeMarginLeft(Node n) {
		Element documentElement = n.getOwnerDocument().getDocumentElement();
		float cumulativeMarginLeft = 0.0F;
		setElementOnly(true);
		while (n != documentElement
				&& !n.getNodeName().equalsIgnoreCase("body")) {
			cumulativeMarginLeft += getNumericRule(n,
					new String[] { "margin", "margin-left" });
			n = n.getParentNode();
		}
		setElementOnly(false);
		return cumulativeMarginLeft;
	}

	public float getMarginTop(Node n) {
		return getNumericRule(n, new String[] { "margin", "margin-top" });
	}

	public float getRelativeFontSize(Node n) {
		return getNumericRule(n, "font-size", "100px", true);
	}

	public String getStyleString(Element elt) {
		List<Map<String, String>> styles = getStyles(elt);
		List<String> rules = new ArrayList<String>();
		for (Map<String, String> map : styles) {
			for (String rule : map.keySet()) {
				rules.add(rule + ": " + map.get(rule));
			}
		}
		Collections.reverse(rules);
		String styleString = CommonUtils.join(rules, "; ");
		return styleString;
	}

	public String getTextAlign(Node node) {
		return getStringRule(node, "text-align");
	}

	public float getTextIndent(Node n) {
		return getNumericRule(n, "text-indent");
	}

	public String getTextTransform(Node node) {
		return getStringRule(node, "text-transform");
	}

	public boolean hasFontWeight(Node node) {
		return getStringRule(node, "font-weight") != null;
	}

	public boolean hasSignificantParaMargins(Element elt) {
		float marginBottom = getMarginBottom(elt);
		float marginTop = getMarginTop(elt);
		boolean marginsDefined = (isRuleDefined(elt, "margin-bottom")
				&& isRuleDefined(elt, "margin-top"))
				|| isRuleDefined(elt, "margin");
		return (marginBottom >= 2 || marginTop >= 2)
				|| (elt.getTagName().equals("P") && !marginsDefined);
	}

	public boolean isBold(Node node) {
		String rule = getStringRule(node, "font-weight");
		return rule != null && bold.matcher(rule).find();
	}

	public boolean isElementOnly() {
		return this.elementOnly;
	}

	public boolean isItalic(Node node) {
		String rule = getStringRule(node, "font-style");
		return rule != null && italic.matcher(rule).find();
	}

	public boolean isMsoListItem(Node node) {
		String rule = getStringRule(node, "mso-list");
		return rule != null && msoListTrue.matcher(rule).find();
	}

	public boolean isNormal(Node node) {
		String rule = getStringRule(node, "font-style");
		return rule != null && normal.matcher(rule).find();
	}

	public boolean isRespectAdditiveRules() {
		return this.respectAdditiveRules;
	}

	public boolean isSmallCaps(Node node) {
		String rule = getStringRule(node, "font-variant");
		return rule != null && smallCaps.matcher(rule).find();
	}

	public boolean isStrikethrough(Node node) {
		String rule = getStringRule(node, "text-decoration");
		return rule != null && strikethrough.matcher(rule).find();
	}

	public boolean isUnderline(Node node) {
		String rule = getStringRule(node, "text-decoration");
		return rule != null && underline.matcher(rule).find();
	}

	public boolean isUppercase(Node node) {
		String rule = getStringRule(node, "text-transform");
		return rule != null && uppercase.matcher(rule).find();
	}

	public boolean isVerticalAlignSuper(Node node) {
		String rule = getStringRule(node, "vertical-align");
		return rule != null && _super.matcher(rule).find();
	}

	public boolean isVerticalAlignSub(Node node) {
		String rule = getStringRule(node, "vertical-align");
		return rule != null && sub.matcher(rule).find();
	}

	public QuickMsoStyleParser parse(Document doc) {
		return parse(doc, null);
	}

	public QuickMsoStyleParser parse(Document doc, String extraStyles) {
		return parse(DomDocument.from(doc), extraStyles);
	}

	public void parse(DomDocument xmlDoc) {
		parse(xmlDoc, null);
	}

	public QuickMsoStyleParser parse(DomDocument xmlDoc, String extraStyles) {
		return parse(xmlDoc, extraStyles, false);
	}

	public QuickMsoStyleParser parse(DomDocument xmlDoc, String extraStyles,
			boolean withDefaultBrowserStyles) {
		this.doc = xmlDoc;
		List<DomNode> styleElements = Stream
				.concat(doc.xpath("//style").stream(),
						doc.xpath("//STYLE").stream())
				.filter(DomNode::isElement).collect(Collectors.toList());
		if (automatchSelectors != null) {
			automatchSelectorSplits = automatchSelectors.split(",\\s*");
		}
		if (extraStyles != null) {
			addStyles(extraStyles, "extraStyles");
		} else {
			addStyles(getDefaultStyles(), "defaultStyles");
		}
		if (withDefaultBrowserStyles) {
			addStyles(getDefaultBrowserStyles(), "defaultBrowserStyles");
		}
		for (DomNode style : styleElements) {
			String c = cleanStyles(style);
			addStyles(c, style);
		}
		return this;
	}

	public void setElementOnly(boolean elementOnly) {
		this.elementOnly = elementOnly;
	}

	public void setRespectAdditiveRules(boolean respectAdditiveRules) {
		this.respectAdditiveRules = respectAdditiveRules;
	}

	private String cleanStyles(DomNode style) {
		String tc = style.textContent();
		return cleanDesignatorPattern.matcher(tc).replaceAll("");
	}

	private void debugResolveStyles(List<StyleElement> list) {
		int debug = 4;
	}

	private String getStringRule0(Node n, String ruleName) {
		float result = 0;
		boolean inherited = isInherited(ruleName);
		while (n != null) {
			if (n.getNodeType() == Node.ELEMENT_NODE) {
				List<Map<String, String>> styles = getStyles((Element) n);
				for (Map<String, String> map : styles) {
					if (map.containsKey(ruleName)) {
						return map.get(ruleName).trim();
					}
				}
			} else {
				break;
			}
			if (!inherited) {
				break;
			}
			n = n.getParentNode();
		}
		return null;
	}

	private boolean isAdditiveRule(String ruleName) {
		return isRespectAdditiveRules() && additiveRules.contains(ruleName);
	}

	private boolean isMultiplicativeUnit(String value) {
		for (String unit : unitMultiplierMap.keySet()) {
			if (value.endsWith(unit)) {
				return true;
			}
		}
		return false;
	}

	private boolean isUnit(String s) {
		for (String unit : unitPixelMap.keySet()) {
			if (s.endsWith(unit)) {
				return true;
			}
		}
		return false;
	}

	private boolean nonRunStyle(String key) {
		if (key.startsWith("mso-")) {
			return false;
		}
		if (key.startsWith("margin")) {
			return false;
		}
		if (key.startsWith("tab-stops")) {
			return false;
		}
		return !runStyles.contains(key);
	}

	private boolean visuallySignificant(String ruleName) {
		if (ruleName.equals("tab-stops")) {
			return false;
		}
		return true;
	}

	String fastLc(String toLc) {
		if (!lcMap.containsKey(toLc)) {
			lcMap.put(toLc, toLc.toLowerCase());
		}
		return lcMap.get(toLc);
	}

	String fastNormaliseWs(String toNormalise) {
		if (!normalisedWsMap.containsKey(toNormalise)) {
			normalisedWsMap.put(toNormalise,
					SEUtilities.normalizeWhitespace(toNormalise).trim());
		}
		return normalisedWsMap.get(toNormalise);
	}

	float getMarginBottom(Node n) {
		return getNumericRule(n, new String[] { "margin", "margin-bottom" });
	}

	int getNumericRule(Node n, String ruleName) {
		return getNumericRule(n, ruleName, "0", false);
	}

	int getNumericRule(Node n, String ruleName, String defaultRuleValue,
			boolean multiply) {
		return getNumericRule(n, ruleName, defaultRuleValue, multiply, false);
	}

	Integer getNumericRule(Node n, String ruleName, String defaultRuleValue,
			boolean multiply, boolean nullIfUndefined) {
		if (debug) {
			System.out.println("--style:" + ruleName);
		}
		float result = multiply ? parseNumericRule(defaultRuleValue) : 0;
		boolean defined = !nullIfUndefined;
		absoluteRule: while (n != null) {
			if (n.getNodeType() == Node.ELEMENT_NODE) {
				Element e = (Element) n;
				List<Map<String, String>> styles = getStyles(e);
				findRule: for (Map<String, String> map : styles) {
					if (map.containsKey(ruleName)) {
						defined = true;
						String value = map.get(ruleName);
						if (isAdditiveRule(ruleName)) {
							result += parseNumericRule(value);
						} else {
							if (multiply && isMultiplicativeUnit(value)) {
								result *= parseMultiplicativeRule(value);
							} else {
								result = parseNumericRule(value);
								break absoluteRule;
							}
						}
						if (debug) {
							System.out.println("--eval: " + value + " : "
									+ parseNumericRule(value));
						}
						break findRule;
					}
				}
			}
			if (elementOnly) {
				break;
			}
			n = n.getParentNode();
		}
		return defined ? (int) result : null;
	}

	int getNumericRule(Node n, String[] ruleNames) {
		for (int i = ruleNames.length - 1; i >= 0; i--) {
			Integer result = getNumericRule(n, ruleNames[i], "0", false, true);
			if (result != null) {
				return result;
			}
		}
		return getNumericRule(n, ruleNames[0], "0", false);
	}

	float getPaddingLeft(Node n) {
		return getNumericRule(n, new String[] { "padding", "padding-left" });
	}

	String getStringRule(Node n, String ruleName) {
		return nodeRuleName.ensure(() -> getStringRule0(n, ruleName), n,
				ruleName);
	}

	List<Map<String, String>> getStyles(Element elt) {
		List<Map<String, String>> result = new ArrayList<Map<String, String>>();
		String tn = fastLc(elt.getTagName());
		String cnAttr = fastLc(elt.getAttribute(
				cc.alcina.framework.common.client.util.HtmlConstants.ATTR_CLASS)
				.trim());
		String sr = elt.getAttribute(
				cc.alcina.framework.common.client.util.HtmlConstants.ATTR_STYLE);
		if (sr.length() != 0) {
			StringMap styles = getStyles(sr);
			if (styles.size() > 0) {
				if (debug) {
					Ax.out("--resolve: %s :: %s :: \n\t%s", elt, sr, styles);
				}
			}
			result.add(styles);
		}
		DomNode node = doc.nodeFor(elt);
		Set<String> cnCombinations = cnCombinationLookup.get(cnAttr);
		for (String cn : cnCombinations) {
			if (cn.length() != 0) {
				List<StyleElement> list = stylze.get(tn + "." + cn);
				if (list != null) {
					if (debug) {
						debugResolveStyles(list);
						Ax.out("--resolve: %s :: %s :: \n\t%s", elt,
								tn + "." + cn, list);
					}
					for (int i = list.size() - 1; i >= 0; i--) {
						result.add(list.get(i).rules);
					}
				}
				list = stylze.get("." + cn);
				if (list != null) {
					if (debug) {
						debugResolveStyles(list);
						Ax.out("--resolve: %s :: %s :: \n\t%s", elt, "." + cn,
								list);
					}
					for (int i = list.size() - 1; i >= 0; i--) {
						result.add(list.get(i).rules);
					}
				}
			}
		}
		List<StyleElement> list = stylze.get(tn);
		if (list != null) {
			if (debug) {
				debugResolveStyles(list);
				Ax.out("--resolve: %s :: %s :: \n\t%s", elt, tn, list);
			}
			for (int i = list.size() - 1; i >= 0; i--) {
				result.add(list.get(i).rules);
			}
		}
		return result;
	}

	public StringMap getStyles(String styles) {
		if (!styleStringToRule.containsKey(styles)) {
			StringMap rules = new StringMap();
			Matcher rm = rulePattern
					.matcher(SEUtilities.normalizeWhitespace(styles).trim());
			while (rm.find()) {
				String ruleKey = normaliseSelector(rm);
				String ruleValue = fastLc(rm.group(2));
				rules.put(ruleKey, ruleValue);
				if (ruleKey.equals("font")) {
					String[] splitz = ruleValue.split(" ");
					for (String s : splitz) {
						if (isUnit(s)) {
							rules.put("font-size", s);
						}
					}
				}
			}
			styleStringToRule.put(styles, rules);
		}
		return styleStringToRule.get(styles);
	}

	boolean isInherited(String ruleName) {
		switch (ruleName) {
		case DomStyleConstants.STYLE_DISPLAY:
			return false;
		default:
			return true;
		}
	}

	boolean isRuleDefined(Node n, String ruleName) {
		if (debug) {
			System.out.println("--style:" + ruleName);
		}
		while (n != null) {
			if (n.getNodeType() == Node.ELEMENT_NODE) {
				List<Map<String, String>> styles = getStyles((Element) n);
				for (Map<String, String> map : styles) {
					if (map.containsKey(ruleName)) {
						return true;
					}
				}
			}
			n = n.getParentNode();
		}
		return false;
	}

	boolean isRuleDefinedNoParent(Node n, String ruleName) {
		if (debug) {
			System.out.println("--style:" + ruleName);
		}
		if (n.getNodeType() == Node.ELEMENT_NODE) {
			List<Map<String, String>> styles = getStyles((Element) n);
			for (Map<String, String> map : styles) {
				if (map.containsKey(ruleName)) {
					return true;
				}
			}
		}
		return false;
	}

	String normaliseSelector(Matcher dm) {
		String selector = fastLc(dm.group(1).trim());
		for (String s : automatchSelectorSplits) {
			if (selector.startsWith(s)) {
				selector = selector.substring(s.length()).trim();
			}
		}
		return selector;
	}

	float parseMultiplicativeRule(String value) {
		float multiplier = 1.0F;
		float fvalue = Float.MIN_VALUE;
		for (String unit : unitMultiplierMap.keySet()) {
			if (value.endsWith(unit)) {
				fvalue = Float.parseFloat(value.replace(unit, ""));
				multiplier = unitMultiplierMap.get(unit);
			}
		}
		return fvalue == Float.MIN_VALUE ? 1.0f : fvalue * multiplier;
	}

	float parseNumericRule(String value) {
		if (unitsPixelMap.containsKey(value)) {
			return unitsPixelMap.get(value);
		}
		value = value.replace(" !msorm", "");
		value = value.replace(" !Important", "");
		value = value.replace(" !important", "");
		float multiplier = 1.0F;
		float fvalue = Float.MIN_VALUE;
		try {
			for (String unit : unitPixelMap.keySet()) {
				if (value.endsWith(unit)) {
					fvalue = Float.parseFloat(value.replace(unit, ""));
					multiplier = unitPixelMap.get(unit);
				}
			}
			if (fvalue == Float.MIN_VALUE) {
				fvalue = Float.parseFloat(value);
			}
		} catch (NumberFormatException e) {
			return parseNumericRule("100%");
		}
		float result = multiplier * fvalue;
		unitsPixelMap.put(value, result);
		return result;
	}

	static class StyleElement {
		Object source;

		String key;

		StringMap rules;

		private String css;

		private int start;

		private int end;

		public StyleElement(String key, StringMap rules, Object source,
				Matcher styleMatcher, String css) {
			this.rules = rules;
			this.key = key;
			this.source = source;
			this.css = css;
			this.start = styleMatcher.start();
			this.end = styleMatcher.end();
		}

		public void addToStylze(Multimap<String, List<StyleElement>> stylze) {
			checkRules();
			stylze.add(key, this);
		}

		public String dumpSource() {
			if (source instanceof Element) {
				Element element = (Element) source;
				// return Ax.format("attr:\n%s\n\ntext:\n%s",
				// element.getAttribute("src"), element.getTextContent());
				return DomNode.from(element).toPrettyMarkup();
			} else {
				return source.toString();
			}
		}

		@Override
		public String toString() {
			return Ax.format("%s : %s [source: %s]\n\n %s", key, rules, source,
					css.substring(start, end));
		}

		private void checkRules() {
			StringMap adds = new StringMap();
			for (Iterator<Entry<String, String>> itr = rules.entrySet()
					.iterator(); itr.hasNext();) {
				Entry<String, String> e = itr.next();
				String rk = e.getKey();
				if (cornerRules.contains(rk)) {
					String[] vals = SEUtilities
							.normalizeWhitespaceAndTrim(e.getValue())
							.split("\\s+");
					int j = 0;
					for (String corner : corners) {
						String newKey = String.format("%s-%s", rk, corner);
						adds.put(newKey, vals[j++]);
						j = j % vals.length;
					}
					itr.remove();
				}
			}
			// make sure we don't overwrite rules with adds
			adds.putAll(rules);
			rules.putAll(adds);
		}
	}

	public String getColor(Node node) {
		return getStringRule(node, "color");
	}

	public String getBackgroundColor(Node node) {
		return getStringRule(node, "background-color");
	}

	public String getBackgroundColorOrColorBackground(Node node) {
		String backgroundColorExplicit = getStringRule(node,
				"background-color");
		if (Ax.notBlank(backgroundColorExplicit)) {
			return backgroundColorExplicit;
		} else {
			String background = getStringRule(node, "background");
			if (background != null && background.startsWith("#")) {
				return background;
			} else {
				return null;
			}
		}
	}
}
