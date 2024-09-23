package cc.alcina.framework.servlet.component.romcom.server;

/**
 * <p>
 * A {@link RemoteComponentResource} is a cacheable String, which can be
 * injected by the client
 */
/*
 * @formatter:off
 * 
 
 ## Implementation plan

 - client code can specify a context "ResourceMarker" - if there's a sole SetInnerMarkup/setText descendant 
   (within the context) then the mutation is labelled with the ResourceMarker
 - when sending mutations to the server, if the ResourceMarker exists on the client (it'll be visible via a cookie),
   don't send the contents
 
 * * @formatter:on
 */
public class RemoteComponentResource {
}
