package cc.alcina.extras.webdriver;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.extras.webdriver.api.TestResultType;
import cc.alcina.extras.webdriver.api.UnitTest;
import cc.alcina.extras.webdriver.api.WDWriter;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.ResourceUtilities;

public class WDManager {
	private static Map<String, WDToken> resultCache = new HashMap<String, WDToken>();

	public static final String CONTEXT_TOKEN = WDManager.class.getName()
			+ ".CONTEXT_TOKEN";

	final static Logger logger = LoggerFactory
			.getLogger(MethodHandles.lookup().lookupClass());

	public static final String CONTEXT_REQUEST = WDManager.class.getName()
			+ ".CONTEXT_REQUEST";

	// private WDToken checkCache(WDConfigurationItem config) {
	// WDToken token = resultCache.get(config.topLevelClassName);
	// if (token != null
	// && (System.currentTimeMillis() - config.usedCacheIfFresherThan) < token
	// .getRootResult().getStartTime()) {
	// return token;
	// }
	// return null;
	// }
	public WDToken getLastResult(WDConfigurationItem config,
			HttpServletResponse resp) {
		if (config == null) {
			System.out.println("null config - no result);");
			return null;
		}
		return resultCache.get(config.topLevelClassName);
	}

	public synchronized WDToken runTest(WDConfigurationItem config,
			HttpServletResponse response, boolean persist, boolean statsOnly)
			throws Exception {
		WDToken token = new WDToken();
		// token = checkCache(config);
		// if (token != null) {
		// return token;
		// }
		for (int i = 0; i < config.times; i++) {
			token = new WDToken();
			WDWriter writer = new WDWriter();
			writer.setResp(response);
			writer.setStatsOnly(statsOnly);
			token.setWriter(writer);
			token.setConfiguration(config);
			token.setDriverHandler(config.driverHandler());
			String cn = config.topLevelClassName;
			if (config.times != 1) {
				writer.write(Ax.format("Pass <%s>", i + 1), 0);
			}
			UnitTest test = (UnitTest) Class.forName(cn).newInstance();
			try {
				LooseContext.push();
				LooseContext.set(CONTEXT_TOKEN, token);
				int timeout = ResourceUtilities.getInteger(WDManager.class,
						"overrideTestTimeout");
				if (timeout != 0) {
					LooseContext.set(WDUtils.CONTEXT_OVERRIDE_TIMEOUT, timeout);
				}
				test.process(token, 0, null);
			} finally {
				LooseContext.pop();
			}
			if (token.getRootResult().getResultType() != TestResultType.ERROR
					|| config.closeOnError) {
				if (ResourceUtilities.is("allowCloseBrowser")) {
					Ax.err("...closeAndCleanup");
					try {
						token.getDriverHandler().closeAndCleanup();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			cacheToken(token);
		}
		logger.info(token.getRootResult().toString());
		return token;
	}

	private void cacheToken(WDToken token) {
		WDToken tCopy = new WDToken();
		tCopy.setConfiguration(token.getConfiguration());
		tCopy.setRootResult(token.getRootResult());
		resultCache.put(tCopy.getConfiguration().topLevelClassName, tCopy);
	}
}
