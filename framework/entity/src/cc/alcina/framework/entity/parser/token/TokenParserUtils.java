package cc.alcina.framework.entity.parser.token;

public class TokenParserUtils {
	public static String quickNormalisePunctuation(String s) {
		if (s == null) {
			return null;
		}
		return s.replace('\u00A0', ' ').replace('\u2013', '-')
				.replace('\u2011', '-').replace('\u2019', '\'');
	}
}
