package cc.alcina.framework.common.client.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class HtmlConstants {
	private static final String HTML_BLOCKS = "ARTICLE,ADDRESS,ASIDE,AUDIO,BLOCKQUOTE,"
			+ "BR,CANVAS,CENTER,DD,DIV,DT,DL,FIELDSET,FIGCAPTION,FIGURE,FOOTER,FORM,"
			+ "H1,H2,H3,H4,H5,H6,HEADER,HGROUP,HR,IFRAME,ILAYER,LAYER,LI,NOSCRIPT,"
			+ "OL,OUTPUT,P,PRE,SECTION,TABLE,TD,TFOOT,TR,UL,VIDEO,BODY";

	private static Set<String> blockTags = null;

	public static final String HTML_TOPS = ",HEAD,HTML,BODY,";

	public static final String HTML_NO_CONTENT = ",BR,HR,INPUT,";

	public static final String HTML_INVISIBLE_CONTENT_ELEMENTS = ",HEAD,STYLE,TEXTAREA,SCRIPT,INPUT,SELECT,TITLE,";

	/**
	 * note - question is "is a child indented"...an element never indents
	 * itself
	 */
	public static final String HTML_INDENTS = ",BLOCKQUOTE,UL,OL,";

	public static final String HTML_SELF_OUTDENTS = ",LI,";

	public static final String DIV = "DIV";

	public static final String SCRIPT = "SCRIPT";

	public static final String META = "META";

	public static final String BODY = "BODY";

	public static final String IMG = "IMG";

	public static final String A = "A";

	public static final String TABLE = "TABLE";

	public static final String CSS_BACKGROUND_COLOR = "backgroundColor";

	public static final String CSS_BORDER_BOTTOM = "borderBottom";

	public static final String STYLE_BORDER_WIDTH = "borderWidth";

	public static final String STYLE_BORDER_STYLE = "borderStyle";

	public static final String STYLE_BORDER_COLOR = "borderColor";

	public static final String ATTR_TITLE = "title";

	public static final String ATTR_CLASS = "class";

	public static final String CSS_CURSOR = "cursor";

	public static final String CSS_COLOR = "color";

	public static final String ATTR_STYLE = "style";

	public static final String STYLE = "STYLE";

	public static final String CSS_UNDERLINE = "underline";

	public static final String CSS_TEXT_DECORATION = "textDecoration";

	public static final String BASE = "BASE";

	public static final String HEAD = "HEAD";

	public static final String LINK = "LINK";

	public static final String CONTENT_EDITABLE = "contentEditable";

	public static final String TARGET_BLANK = "_blank";

	public static synchronized boolean isHtmlBlock(String tag) {
		if (blockTags == null) {
			blockTags = new HashSet<>();
			Arrays.stream(HTML_BLOCKS.split(",")).forEach(blockTags::add);
			Arrays.stream(HTML_BLOCKS.toLowerCase().split(","))
					.forEach(blockTags::add);
		}
		return blockTags.contains(tag);
	}
}
