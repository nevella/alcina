package cc.alcina.framework.gwt.client.data.place;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.gwt.client.place.BasePlace;
import cc.alcina.framework.gwt.client.place.BasePlaceTokenizer;

public class PublicationPlace extends BasePlace {
    public long id;

    public PublicationPlace() {
    }

    public PublicationPlace(long id) {
        this.id = id;
    }

    @Override
    public String toHrefString() {
        return Registry.impl(PublicationPlaceAbsoluteHrefSupplier.class)
                .getHref(this);
    }

    @RegistryLocation(registryPoint = PublicationPlaceAbsoluteHrefSupplier.class, implementationType = ImplementationType.SINGLETON)
    public static class PublicationPlaceAbsoluteHrefSupplier {
        public String getHref(PublicationPlace basePlace) {
            return null;
        }
    }

    public static class PublicationPlaceTokenizer
            extends BasePlaceTokenizer<PublicationPlace> {
        @Override
        public Class<PublicationPlace> getTokenizedClass() {
            return PublicationPlace.class;
        }

        @Override
        protected PublicationPlace getPlace0(String token) {
            PublicationPlace place = new PublicationPlace();
            if (parts.length == 1) {
                return place;
            }
            place.id = Long.parseLong(parts[1]);
            return place;
        }

        @Override
        protected void getToken0(PublicationPlace place) {
            addTokenPart(place.id);
        }
    }
}
