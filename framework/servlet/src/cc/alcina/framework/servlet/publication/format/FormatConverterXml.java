package cc.alcina.framework.servlet.publication.format;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.publication.FormatConversionTarget.FormatConversionTarget_XML;
import cc.alcina.framework.common.client.publication.PublicationContent;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.servlet.publication.FormatConverter;
import cc.alcina.framework.servlet.publication.PublicationContext;
import cc.alcina.framework.common.client.logic.reflection.Registration;

@RegistryLocation(registryPoint = FormatConverter.class, targetClass = FormatConversionTarget_XML.class)
@Registration({ FormatConverter.class, FormatConversionTarget_XML.class })
public class FormatConverterXml implements FormatConverter {

    // FIXME - registry.2 - remove
    public static interface TypedConverterXml<T extends PublicationContent> {

        public String toXml(T publicationContent);
    }

    @Override
    public InputStream convert(PublicationContext ctx, FormatConversionModel conversionModel) throws Exception {
        String s = conversionModel.html;
        if (s == null) {
            s = new String(conversionModel.bytes, StandardCharsets.UTF_8);
        }
        if (!s.startsWith("<?xml")) {
            s = CommonUtils.XML_PI + s;
        }
        TypedConverterXml typedConverter = Registry.implOrNull(TypedConverterXml.class, ctx.publicationContent.getClass());
        if (typedConverter != null) {
            s = typedConverter.toXml(ctx.publicationContent);
        }
        return new ByteArrayInputStream(s.getBytes("UTF-8"));
    }

    @Override
    public String getFileExtension() {
        return "xml";
    }

    @Override
    public String getMimeType() {
        return "text/xml";
    }
}
