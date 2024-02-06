package cc.alcina.framework.entity.logic;

import java.util.Timer;
import java.util.regex.Pattern;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.persistence.AppPersistenceBase;

public class EntityLayerUtils {
	private static Pattern botExtraUa;

	private static Pattern botUa;

	public static final Timer timer = new Timer("alcina-jvm-timer", true);

	public static String getApplicationHostName() {
		return Configuration.get("applicationHostName");
	}

	public static String getLocalHostName() {
		try {
			String defined = Configuration.get("localHostName");
			if (Ax.isBlank(defined)) {
				return java.net.InetAddress.getLocalHost().getHostName();
			} else {
				return defined;
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static Boolean isBotExtraUserAgent(String userAgent) {
		return EntityLayerUtils.botExtraUa != null
				&& EntityLayerUtils.botExtraUa.matcher(userAgent).find();
	}

	public static Boolean isBotUserAgent(String userAgent) {
		if (EntityLayerUtils.botUa == null) {
			EntityLayerUtils.botUa = Pattern.compile(
					"(AdsBot-Google|AhrefsBot|bingbot|googlebot"
							+ "|ArchiveTeam|curl|facebookexternalhit|HggH"
							+ "|LoadImpactPageAnalyzer|LoadImpactRload|servlet"
							+ "|WebCache|WebQL|WeCrawlForThePeace|Wget"
							+ "|python-requests|FlipboardProxy"
							+ "|BingPreview|Baiduspider|YandexBot|Java|rogerbot|Slackbot)",
					Pattern.CASE_INSENSITIVE);
			String botExtraRegex = Configuration.get("botUserAgentExtra");
			EntityLayerUtils.botExtraUa = botExtraRegex.isEmpty() ? null
					: Pattern.compile(botExtraRegex);
		}
		return CommonUtils.isNullOrEmpty(userAgent)
				|| EntityLayerUtils.botUa.matcher(userAgent).find()
				|| EntityLayerUtils.isBotExtraUserAgent(userAgent);
	}

	public static boolean isProduction() {
		return !isTestOrTestServer();
	}

	public static boolean isTest() {
		return AppPersistenceBase.isTest();
	}

	public static boolean isTestOrTestServer() {
		return Ax.isTest() || AppPersistenceBase.isTestServer();
	}

	public static boolean isTestServer() {
		return AppPersistenceBase.isTestServer();
	}

	public static void setTestServer(boolean testServer) {
		AppPersistenceBase.setTestServer(testServer);
	}
}
