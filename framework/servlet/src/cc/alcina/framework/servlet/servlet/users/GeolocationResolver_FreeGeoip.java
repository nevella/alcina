package cc.alcina.framework.servlet.servlet.users;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CachingMap;
import cc.alcina.framework.entity.ResourceUtilities;

//Gone, see freegeoip.com
public class GeolocationResolver_FreeGeoip implements GeolocationResolver {
	private CachingMap<String, String> ipToLocation = new CachingMap<>(
			s -> getLocation0(s));

	@Override
	public synchronized String getLocation(String ipAddress) {
		if (ResourceUtilities.is(GeolocationResolver_FreeGeoip.class,
				"dummyResolver")) {
			return "(dummy ip adress)";
		}
		return ipToLocation.get(ipAddress);
	}

	private String getLocation0(String ipAddress) {
		if (Ax.isBlank(ipAddress)) {
			return "(no ip adress)";
		}
		try {
			String url = Ax.format("http://freegeoip.net/json/%s", ipAddress);
			String result = ResourceUtilities.readUrlAsString(url);
			ObjectNode node = (ObjectNode) new ObjectMapper().readTree(result);
			if (Ax.isBlank(node.get("country_name").asText())) {
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
