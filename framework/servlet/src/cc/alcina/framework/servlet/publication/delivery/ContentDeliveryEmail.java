package cc.alcina.framework.servlet.publication.delivery;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import com.sun.mail.smtp.SMTPMessage;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.publication.ContentDeliveryType;
import cc.alcina.framework.common.client.publication.ContentDeliveryType.ContentDeliveryType_EMAIL;
import cc.alcina.framework.common.client.publication.DeliveryModel;
import cc.alcina.framework.common.client.publication.DeliveryModel.MailAttachment;
import cc.alcina.framework.common.client.publication.DeliveryModel.MailInlineImage;
import cc.alcina.framework.common.client.publication.FormatConversionTarget.FormatConversionTarget_PDF;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.servlet.publication.EntityCleaner;
import cc.alcina.framework.servlet.publication.FormatConverter;
import cc.alcina.framework.servlet.publication.PublicationContext;

/**
 * could extend xxxMimeType - but we'd need to expand the registry, with a
 * "no-inherit"..TODO??
 * 
 * @author nick@alcina.cc
 * 
 */
@RegistryLocation(registryPoint = ContentDeliveryType.class, targetClass = ContentDeliveryType_EMAIL.class)
public class ContentDeliveryEmail implements ContentDelivery {
	public static final String PUBLICATION_REASON_MESSAGE = "<!--PUBLICATION_REASON_MESSAGE-->";

	public static final String CONTEXT_SMTP_FROM_NAME = ContentDeliveryEmail.class
			.getName() + ".CONTEXT_SMTP_FROM_NAME";

	public static final String CONTEXT_OVERRIDE_TO_ADDRESS = ContentDeliveryEmail.class
			.getName() + ".CONTEXT_OVERRIDE_TO_ADDRESS";

	public static final String CONTEXT_ALSO_SEND_TO_ADDRESS = ContentDeliveryEmail.class
			.getName() + ".CONTEXT_ALSO_SEND_TO_ADDRESS";

	public static final String CONTEXT_SMTP_FROM_EMAIL = ContentDeliveryEmail.class
			.getName() + ".CONTEXT_SMTP_FROM_EMAIL";

	public static final transient String PROP_ATTACH_EMAIL_BODY_AS_PDF = ContentDeliveryEmail.class
			.getName() + ".PROP_ATTACH_EMAIL_BODY_AS_PDF";

	public static final transient String PROP_ATTACH_EMAIL_BODY_AS_PDF_FILENAME = ContentDeliveryEmail.class
			.getName() + ".PROP_ATTACH_EMAIL_BODY_AS_PDF_FILENAME";

	public String deliver(final InputStream convertedContent,
			final DeliveryModel deliveryModel, final FormatConverter hfc,
			boolean requestorPass) throws Exception {
		byte[] msgBytes = ResourceUtilities
				.readStreamToByteArray(convertedContent);
		send(new ByteArrayInputStream(msgBytes), deliveryModel, hfc,
				requestorPass, deliveryModel.getEmailAddress());
		if (LooseContext.has(CONTEXT_ALSO_SEND_TO_ADDRESS)) {
			send(new ByteArrayInputStream(msgBytes), deliveryModel, hfc,
					requestorPass,
					LooseContext.get(CONTEXT_ALSO_SEND_TO_ADDRESS));
		}
		return "OK";
	}

	@Override
	public String deliver(PublicationContext ctx,
			final InputStream convertedContent,
			final DeliveryModel deliveryModel, final FormatConverter hfc)
			throws Exception {
		byte[] msgBytes = ResourceUtilities
				.readStreamToByteArray(convertedContent);
		deliver(new ByteArrayInputStream(msgBytes), deliveryModel, hfc, false);
		deliver(new ByteArrayInputStream(msgBytes), deliveryModel, hfc, true);
		return "OK";
	}

	protected boolean isUseVerp() {
		return false;
	}

	protected String send(final InputStream convertedContent,
			final DeliveryModel deliveryModel, final FormatConverter hfc,
			boolean requestorPass, String emailAddress)
			throws MessagingException, UnsupportedEncodingException,
			InstantiationException, IllegalAccessException,
			ClassNotFoundException, AddressException, Exception, IOException,
			FileNotFoundException, NoSuchProviderException {
		boolean debug = false;
		Properties props = new Properties();
		Class c = ContentDeliveryEmail.class;
		String host = ResourceUtilities.getBundledString(c, "smtp.host.name");
		Integer port = Integer.valueOf(
				ResourceUtilities.getBundledString(c, "smtp.host.port"));
		Boolean authenticate = Boolean.valueOf(
				ResourceUtilities.getBundledString(c, "smtp.authenticate"));
		String userName = ResourceUtilities.getBundledString(c,
				"smtp.username");
		String password = ResourceUtilities.getBundledString(c,
				"smtp.password");
		String fromAddress = ResourceUtilities.getBundledString(c,
				"smtp.from.address");
		String fromName = ResourceUtilities.getBundledString(c,
				"smtp.from.name");
		if (LooseContext.has(CONTEXT_SMTP_FROM_EMAIL)) {
			fromAddress = LooseContext.get(CONTEXT_SMTP_FROM_EMAIL);
		}
		if (LooseContext.has(CONTEXT_SMTP_FROM_NAME)) {
			fromName = LooseContext.get(CONTEXT_SMTP_FROM_NAME);
		}
		props.put("mail.smtp.host", host);
		props.put("mail.smtp.auth", authenticate.toString());
		if (ResourceUtilities.is(c, "smtp.ttls")) {
			props.setProperty("mail.smtp.starttls.enable", "true");
		}
		Session session = Session.getInstance(props, null);
		session.setDebug(debug);
		SMTPMessage msg = new SMTPMessage(session);
		msg.setSentDate(new Date());
		msg.setFrom(new InternetAddress(fromAddress, fromName));
		List<InternetAddress> addresses = new ArrayList<InternetAddress>();
		String[] emailAddresses = emailAddress.split("(;|,| )+");
		String filterClassName = ResourceUtilities
				.getBundledString(AddressFilter.class, "smtp.filter.className");
		String systemEmailAddressOfRequestor = deliveryModel
				.getSystemEmailAddressOfRequestor();
		if (LooseContext.has(CONTEXT_OVERRIDE_TO_ADDRESS)) {
			addresses.clear();
			emailAddresses = new String[] {
					LooseContext.get(CONTEXT_OVERRIDE_TO_ADDRESS) };
		}
		if (!SEUtilities.isNullOrEmpty(filterClassName)) {
			AddressFilter filter = (AddressFilter) Class
					.forName(filterClassName).newInstance();
			emailAddresses = filter.filterAddresses(emailAddresses);
			if (systemEmailAddressOfRequestor != null) {
				String[] tmpSystemEmailBuffer = filter.filterAddresses(
						new String[] { systemEmailAddressOfRequestor });
				systemEmailAddressOfRequestor = tmpSystemEmailBuffer.length == 1
						? tmpSystemEmailBuffer[0]
						: "(null)";
			}
		}
		for (String email : emailAddresses) {
			String emTrim = email.trim();
			if (emTrim.length() == 0) {
				continue;
			}
			if (requestorPass
					&& !emTrim.equals(systemEmailAddressOfRequestor)) {
				continue;
			}
			if (!requestorPass
					&& emTrim.equals(systemEmailAddressOfRequestor)) {
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
		msg.setSubject(
				requestorPass ? deliveryModel.getEmailSubjectForRequestor()
						: deliveryModel.getEmailSubject());
		MailAttachment pdfAttachment = null;
		if (deliveryModel.isEmailInline()) {
			if (deliveryModel.hasProperty(PROP_ATTACH_EMAIL_BODY_AS_PDF)) {
				FormatConverter converter = Registry.impl(FormatConverter.class,
						FormatConversionTarget_PDF.class);
				InputStream stream = converter.convert(PublicationContext.get(),
						PublicationContext.get().formatConversionModel);
				String uuid = UUID.randomUUID().toString();
				pdfAttachment = new MailAttachment();
				pdfAttachment.uid = uuid;
				pdfAttachment.contentType = "application/pdf";
				pdfAttachment.requestBytes = ResourceUtilities
						.readStreamToByteArray(stream);
				pdfAttachment.dataSourceMimeType = "application/pdf";
				pdfAttachment.suggestedFileName = deliveryModel
						.providePropertyValue(
								PROP_ATTACH_EMAIL_BODY_AS_PDF_FILENAME);
				deliveryModel.addAttachment(pdfAttachment);
			}
			String message = ResourceUtilities
					.readStreamToString(convertedContent);
			message = message.replace(PUBLICATION_REASON_MESSAGE,
					requestorPass
							? deliveryModel.getAttachmentMessageForRequestor()
							: deliveryModel.getAttachmentMessage());
			message = EntityCleaner.get().nonAsciiToUnicodeEntities(message);
			MimeMultipart multipart = new MimeMultipart("related");
			// first part (the html)
			BodyPart messageBodyPart = new MimeBodyPart();
			messageBodyPart.setContent(message, "text/html");
			multipart.addBodyPart(messageBodyPart);
			for (MailInlineImage image : deliveryModel.provideImages()) {
				MimeBodyPart imageBodyPart = new MimeBodyPart();
				imageBodyPart.setHeader("Content-Type", image.contentType);
				imageBodyPart.setHeader("Content-Transfer-Encoding", "base64");
				imageBodyPart.setDisposition(MimeBodyPart.INLINE);
				imageBodyPart.setContentID(Ax.format("<%s>", image.uid));
				DataSource ds = new ByteArrayDataSource(image.requestBytes,
						"image/jpeg");
				imageBodyPart.setDataHandler(new DataHandler(ds));
				multipart.addBodyPart(imageBodyPart);
			}
			for (MailAttachment attachment : deliveryModel
					.provideAttachments()) {
				MimeBodyPart attachmentBodyPart = new MimeBodyPart();
				attachmentBodyPart.setHeader("Content-Type",
						attachment.contentType);
				attachmentBodyPart.setHeader("Content-Transfer-Encoding",
						"base64");
				attachmentBodyPart.setDisposition(MimeBodyPart.ATTACHMENT);
				attachmentBodyPart
						.setContentID(Ax.format("<%s>", attachment.uid));
				DataSource ds = new ByteArrayDataSource(attachment.requestBytes,
						attachment.contentType);
				attachmentBodyPart.setDataHandler(new DataHandler(ds));
				attachmentBodyPart.setFileName(attachment.suggestedFileName);
				multipart.addBodyPart(attachmentBodyPart);
			}
			msg.setContent(multipart);
		} else {
			MimeBodyPart messageBodyPart = new MimeBodyPart();
			messageBodyPart
					.setContent(
							requestorPass
									? deliveryModel
											.getAttachmentMessageForRequestor()
									: deliveryModel.getAttachmentMessage(),
							"text/html");
			Multipart multipart = new MimeMultipart();
			final String fileName = deliveryModel.getSuggestedFileName() + "."
					+ hfc.getFileExtension();
			multipart.addBodyPart(messageBodyPart);
			messageBodyPart = new MimeBodyPart();
			File file = File.createTempFile(
					deliveryModel.getSuggestedFileName(),
					"." + hfc.getFileExtension());
			file.deleteOnExit();
			ResourceUtilities.writeStreamToStream(convertedContent,
					new FileOutputStream(file));
			messageBodyPart
					.setDataHandler(new DataHandler(new FileDataSource(file)));
			messageBodyPart.setFileName(fileName);
			multipart.addBodyPart(messageBodyPart);
			msg.setContent(multipart);
		}
		if (isUseVerp() && PublicationContext.get() != null) {
			String publicationUid = PublicationContext
					.get().publicationResult.publicationUid;
			// will be null if non-persistent
			if (publicationUid != null) {
				String replyTo = fromAddress.replaceFirst("(.+?)@(.+)",
						String.format("$1+.r.%s@$2", publicationUid));
				String bounceTo = fromAddress.replaceFirst("(.+?)@(.+)",
						String.format("$1+.b.%s@$2", publicationUid));
				msg.setEnvelopeFrom(bounceTo);
				msg.setHeader("Reply-to", replyTo);
			}
		}
		Transport transport = session.getTransport("smtp");
		transport.connect(host, port, userName, password);
		transport.sendMessage(msg, msg.getAllRecipients());
		if (PublicationContext.get() != null) {
			PublicationContext.get().mimeMessageId = msg.getMessageID();
		}
		transport.close();
		if (pdfAttachment != null) {
			deliveryModel.removeAttachment(pdfAttachment);
		}
		return "OK";
	}
}
