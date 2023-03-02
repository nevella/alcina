package cc.alcina.framework.servlet.publication.delivery;

import java.util.ArrayList;

import cc.alcina.framework.entity.Configuration;

public class FilterDomainAddressFilter implements AddressFilter {
	@Override
	public String[] filterAddresses(String[] addresses) {
		String domain = Configuration.get(AddressFilter.class,
				"smtp.restrictToDomain");
		if (domain.length() == 0) {
			return addresses;
		}
		ArrayList<String> result = new ArrayList<String>();
		for (String address : addresses) {
			if (address.contains(domain)) {
				result.add(address);
			}
		}
		return (String[]) result.toArray(new String[result.size()]);
	}
}
