package cc.alcina.framework.servlet.publication.delivery;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.publication.ContentDeliveryType;
import cc.alcina.framework.common.client.publication.ContentDeliveryType.ContentDeliveryType_MULTIPLE;
import cc.alcina.framework.common.client.publication.DeliveryModel;
import cc.alcina.framework.common.client.publication.DeliveryModel.MultipleDeliveryEntry;
import cc.alcina.framework.common.client.publication.request.ContentRequestBase;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.servlet.publication.FormatConverter;
import cc.alcina.framework.servlet.publication.PublicationContext;

/**
 * @author nick@alcina.cc
 */
@Registration({ ContentDeliveryType.class, ContentDeliveryType_MULTIPLE.class })
public class ContentDeliveryMultiple implements ContentDelivery {
	Logger logger = LoggerFactory.getLogger(getClass());

	@SuppressWarnings("resource")
	@Override
	public String deliver(PublicationContext ctx, InputStream convertedContent,
			DeliveryModel deliveryModel, FormatConverter fc) throws Exception {
		byte[] bytes = ResourceUtilities
				.readStreamToByteArray(convertedContent);
		String token = null;
		for (DeliveryModel.MultipleDeliveryEntry entry : deliveryModel
				.getMultipleDeliveryEntries()) {
			ContentDelivery deliverer = Registry.query(ContentDelivery.class)
					.setKeys(ContentDeliveryType.class,
							entry.provideContentDeliveryType().getClass())
					.impl();
			ContentRequestBase mutableDeliveryModel = (ContentRequestBase) deliveryModel;
			InputStream stream = new ByteArrayInputStream(bytes);
			if (entry.getEmailAddresses() != null) {
				mutableDeliveryModel.setEmailAddress(entry.getEmailAddresses());
			}
			if (entry.getEmailSubject() != null) {
				mutableDeliveryModel.setEmailSubject(entry.getEmailSubject());
			}
			if (entry.getFileName() != null) {
				mutableDeliveryModel.setSuggestedFileName(entry.getFileName());
			}
			mutableDeliveryModel.setMimeType(entry.getMimeType());
			if (entry.getTransformerClassName() != null) {
				MultipleDeliveryEntry.Transformer transformer = Reflections
						.newInstance(Reflections
								.forName(entry.getTransformerClassName()));
				stream = transformer.apply(stream,
						entry.provideTransformerProperties());
			}
			logger.info("Delivering type {}",
					entry.provideContentDeliveryType().serializedForm());
			token = deliverer.deliver(ctx, stream, deliveryModel, fc);
		}
		return token;
	}
}
