package cc.alcina.framework.servlet.publication.delivery;

import cc.alcina.framework.entity.Configuration;

public class ForceDomainAddressFilter implements AddressFilter {
	@Override
	public String[] filterAddresses(String[] addresses) {
		String domain = Configuration.get(AddressFilter.class,
				"smtp.restrictToDomain");
		if (domain.length() == 0) {
			return addresses;
		}
		String[] result = new String[addresses.length];
		int i = 0;
		for (String address : addresses) {
			result[i++] = address.split("@")[0] + "@" + domain;
		}
		return result;
	}
}
