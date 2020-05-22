package cc.alcina.framework.servlet.servlet.users;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CachingMap;
import cc.alcina.framework.entity.ResourceUtilities;

@RegistryLocation(registryPoint = GeolocationResolver.class, implementationType = ImplementationType.SINGLETON)
public class GeolocationResolver_Ipstack implements GeolocationResolver {
	private CachingMap<String, String> ipToLocation = new CachingMap<>(
			s -> getLocation0(s));

	@Override
	public synchronized String getLocation(String ipAddress) {
		if (ResourceUtilities.is(GeolocationResolver_Ipstack.class,
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
			String url = Ax.format(
					"http://api.ipstack.com/%s?access_key=%s&output=json",
					ipAddress, apiKey);
			String result = ResourceUtilities.readUrlAsString(url);
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
