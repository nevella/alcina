package cc.alcina.framework.entity.logic;

import java.util.regex.Pattern;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.ResourceUtilities;

public class EntityLayerUtils {
	public static String getLocalHostName() {
		try {
			String defined = ResourceUtilities.get(EntityLayerUtils.class,
					"localHostName");
			if (Ax.isBlank(defined)) {
				return java.net.InetAddress.getLocalHost().getHostName();
			} else {
				return defined;
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static String getApplicationHostName() {
		return ResourceUtilities.get("applicationHostName");
	}

	public static Boolean isBotUserAgent(String userAgent) {
		if (EntityLayerUtils.botUa == null) {
			EntityLayerUtils.botUa = Pattern.compile(
					"(AdsBot-Google|AhrefsBot|bingbot|googlebot"
							+ "|ArchiveTeam|curl|facebookexternalhit|HggH"
							+ "|LoadImpactPageAnalyzer|LoadImpactRload|servlet"
							+ "|WebCache|WebQL|WeCrawlForThePeace|Wget"
							+ "|python-requests|FlipboardProxy|"
							+ "BingPreview|Baiduspider|YandexBot|Java|rogerbot|Slackbot)",
					Pattern.CASE_INSENSITIVE);
			String botExtraRegex = ResourceUtilities.get(EntityLayerUtils.class,
					"botUserAgentExtra");
			EntityLayerUtils.botExtraUa = botExtraRegex.isEmpty() ? null
					: Pattern.compile(botExtraRegex);
		}
		return CommonUtils.isNullOrEmpty(userAgent)
				|| EntityLayerUtils.botUa.matcher(userAgent).find()
				|| EntityLayerUtils.isBotExtraUserAgent(userAgent);
	}

	public static Boolean isBotExtraUserAgent(String userAgent) {
		return EntityLayerUtils.botExtraUa != null
				&& EntityLayerUtils.botExtraUa.matcher(userAgent).find();
	}

	private static Pattern botExtraUa;

	private static Pattern botUa;
}
