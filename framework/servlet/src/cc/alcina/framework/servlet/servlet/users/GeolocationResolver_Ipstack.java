package cc.alcina.framework.servlet.servlet.users;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CachingMap;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SimpleHttp;

@Registration.Singleton(GeolocationResolver.class)
public class GeolocationResolver_Ipstack implements GeolocationResolver {
	// Timeout for requests to IpStack
	private static int QUERY_TIMEOUT = 15 * 1000;

	private CachingMap<String, String> ipToLocation = new CachingMap<>(
			s -> getLocation0(s));

	@Override
	public synchronized String getLocation(String ipAddress) {
		if (Configuration.is(GeolocationResolver_Ipstack.class,
				"dummyResolver")) {
			return "(dummy ip adress)";
		}
		return ipToLocation.get(ipAddress);
	}

	private String getLocation0(String ipAddress) {
		if (Ax.isBlank(ipAddress)) {
			return "(no ip adress)";
		}
		Pattern p = Pattern.compile("([0-9.]+), .+");
		Matcher m = p.matcher(ipAddress);
		// handle multiple ip address (apache networking/headers...?)
		if (m.matches()) {
			ipAddress = m.group(1);
		}
		try {
			String apiKey = ResourceUtilities
					.get(GeolocationResolver_Ipstack.class, "apiKey");
			// Generate query
			String url = Ax.format("http://api.ipstack.com/%s", ipAddress);
			StringMap params = StringMap.properties("access_key", apiKey,
					"output", "json");
			SimpleHttp query = new SimpleHttp(url)
					.withQueryStringParameters(params)
					.withTimeout(QUERY_TIMEOUT);
			String result = query.asString();
			ObjectNode node = (ObjectNode) new ObjectMapper().readTree(result);
			if (node.has("success") && !node.get("success").asBoolean()) {
				return "(unable to resolve ip adress)(probably usage limit)";
			}
			if (node.get("country_name").isNull()) {
				return "(Local address)";
			}
			return Ax.format("%s/%s",
					Ax.blankTo(node.get("city").asText(), "(No city)"),
					node.get("country_name").asText());
		} catch (Exception e) {
			e.printStackTrace();
			return "(unable to resolve ip adress)";
		}
	}
}
