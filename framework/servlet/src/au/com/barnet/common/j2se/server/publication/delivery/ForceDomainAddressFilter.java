package au.com.barnet.common.j2se.server.publication.delivery;

import cc.alcina.framework.entity.ResourceUtilities;

public class ForceDomainAddressFilter implements AddressFilter {
	public String[] filterAddresses(String[] addresses) {
		String domain = ResourceUtilities.getBundledString(
				AddressFilter.class, "smtp.restrictToDomain");
		if (domain.length() == 0) {
			return addresses;
		}
		String[] result = new String[addresses.length];
		int i = 0;
		for (String address : addresses) {
			result[i++]=address.split("@")[0]+"@"+domain;
		}
		return result;
	}
}
