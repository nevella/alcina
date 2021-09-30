package cc.alcina.framework.servlet.publication.delivery;

import cc.alcina.framework.entity.ResourceUtilities;

// Like ForceDomainAddressFilter, but forces a particular recipient
public class ForceRecipientAddressFilter implements AddressFilter {
	public String[] filterAddresses(String[] addresses) {
		String domain = ResourceUtilities.getBundledString(AddressFilter.class,
				"smtp.restrictToAddress");
		if (domain.length() == 0) {
			return addresses;
		}
		String[] result = { domain };
		return result;
	}
}
