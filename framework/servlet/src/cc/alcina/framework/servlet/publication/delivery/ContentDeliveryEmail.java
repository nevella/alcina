package cc.alcina.framework.servlet.publication.delivery;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.publication.ContentDeliveryType;
import cc.alcina.framework.common.client.publication.ContentDeliveryType.ContentDeliveryType_EMAIL;
import cc.alcina.framework.common.client.publication.DeliveryModel;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.servlet.publication.EntityCleaner;
import cc.alcina.framework.servlet.publication.FormatConverter;
import cc.alcina.framework.servlet.publication.PublicationContext;

/**
 * could extend xxxMimeType - but we'd need to expand the registry, with a
 * "no-inherit"..TODO??
 * 
 * @author nreddel@barnet.com.au
 * 
 */
@RegistryLocation(registryPoint = ContentDeliveryType.class, targetClass = ContentDeliveryType_EMAIL.class)
public class ContentDeliveryEmail implements ContentDelivery {
	public static final String PUBLICATION_REASON_MESSAGE = "<!--PUBLICATION_REASON_MESSAGE-->";

	public String deliver(PublicationContext ctx,final InputStream convertedContent,
			final DeliveryModel deliveryModel, final FormatConverter hfc)
			throws Exception {
		byte[] msgBytes = ResourceUtilities
				.readStreamToByteArray(convertedContent);
		deliver(new ByteArrayInputStream(msgBytes), deliveryModel, hfc, false);
		deliver(new ByteArrayInputStream(msgBytes), deliveryModel, hfc, true);
		return "OK";
	}

	public String deliver(final InputStream convertedContent,
			final DeliveryModel deliveryModel, final FormatConverter hfc,
			boolean requestorPass) throws Exception {
		boolean debug = false;
		Properties props = new Properties();
		Class c = ContentDeliveryEmail.class;
		String host = ResourceUtilities.getBundledString(c,
				"smtp.host.name");
		Integer port = Integer.valueOf(ResourceUtilities
				.getBundledString(c, "smtp.host.port"));
		Boolean authenticate = Boolean.valueOf(ResourceUtilities
				.getBundledString(c, "smtp.authenticate"));
		String userName = ResourceUtilities.getBundledString(c,
				"smtp.username");
		String password = ResourceUtilities.getBundledString(c,
				"smtp.password");
		String fromAddress = ResourceUtilities.getBundledString(c,
				"smtp.from.address");
		String fromName = ResourceUtilities.getBundledString(c,
				"smtp.from.name");
		props.put("mail.smtp.host", host);
		props.put("mail.smtp.auth", authenticate.toString());
		Session session = Session.getDefaultInstance(props, null);
		session.setDebug(debug);
		MimeMessage msg = new MimeMessage(session);
		msg.setSentDate(new Date());
		msg.setFrom(new InternetAddress(fromAddress, fromName));
		List<InternetAddress> addresses = new ArrayList<InternetAddress>();
		String[] emailAddresses = deliveryModel.getEmailAddress().split("(;|,| )+");
		String filterClassName = ResourceUtilities.getBundledString(AddressFilter.class,
		"smtp.filter.className");
		
		if (!SEUtilities.isNullOrEmpty(filterClassName)) {
			AddressFilter filter = (AddressFilter) Class.forName(filterClassName).newInstance();
			emailAddresses = filter.filterAddresses(emailAddresses);
		}
		for (String email : emailAddresses) {
			String emTrim = email.trim();
			if (emTrim.length() == 0) {
				continue;
			}
			if (requestorPass
					&& !emTrim.equals(deliveryModel
							.getSystemEmailAddressOfRequestor())) {
				continue;
			}
			if (!requestorPass
					&& emTrim.equals(deliveryModel
							.getSystemEmailAddressOfRequestor())) {
				continue;
			}
			addresses.add(new InternetAddress(emTrim));
		}
		if (addresses.size() == 0) {
			return null;
		}
		InternetAddress[] addressTo = (InternetAddress[]) addresses
				.toArray(new InternetAddress[addresses.size()]);
		msg.setRecipients(Message.RecipientType.TO, addressTo);
		msg.setSubject(requestorPass ? deliveryModel
				.getEmailSubjectForRequestor() : deliveryModel
				.getEmailSubject());
		if (deliveryModel.isEmailInline()) {
			String message = ResourceUtilities
					.readStreamToString(convertedContent);
			message = message.replace(PUBLICATION_REASON_MESSAGE,
					requestorPass ? deliveryModel
							.getAttachmentMessageForRequestor() : deliveryModel
							.getAttachmentMessage());
			message = EntityCleaner.get().nonAsciiToUnicodeEntities(message);
			msg.setContent(message, "text/html");
		} else {
			MimeBodyPart messageBodyPart = new MimeBodyPart();
			messageBodyPart.setContent(requestorPass ? deliveryModel
					.getAttachmentMessageForRequestor() : deliveryModel
					.getAttachmentMessage(), "text/html");
			Multipart multipart = new MimeMultipart();
			final String fileName = deliveryModel.getSuggestedFileName() + "."
					+ hfc.getFileExtension();
			multipart.addBodyPart(messageBodyPart);
			messageBodyPart = new MimeBodyPart();
			File file = File.createTempFile(deliveryModel
					.getSuggestedFileName(), "." + hfc.getFileExtension());
			file.deleteOnExit();
			ResourceUtilities.writeStreamToStream(convertedContent,
					new FileOutputStream(file));
			messageBodyPart.setDataHandler(new DataHandler(new FileDataSource(
					file)));
			messageBodyPart.setFileName(fileName);
			multipart.addBodyPart(messageBodyPart);
			msg.setContent(multipart);
		}
		Transport transport = session.getTransport("smtp");
		transport.connect(host, port, userName, password);
		transport.sendMessage(msg, msg.getAllRecipients());
		PublicationContext.get().mimeMessageId=msg.getMessageID();
		transport.close();
		return "OK";
	}
}
