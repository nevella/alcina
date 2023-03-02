package cc.alcina.framework.servlet.publication.delivery;

import cc.alcina.framework.entity.Configuration;

// Like ForceDomainAddressFilter, but forces a particular recipient
public class ForceRecipientAddressFilter implements AddressFilter {
	@Override
	public String[] filterAddresses(String[] addresses) {
		String domain = Configuration.get(AddressFilter.class,
				"smtp.restrictToAddress");
		if (domain.length() == 0) {
			return addresses;
		}
		String[] result = { domain };
		return result;
	}
}
