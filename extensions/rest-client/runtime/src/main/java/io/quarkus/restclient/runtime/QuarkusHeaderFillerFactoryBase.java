package io.quarkus.restclient.runtime;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.logging.Logger;
import org.jboss.resteasy.microprofile.client.header.HeaderFiller;
import org.jboss.resteasy.microprofile.client.header.HeaderFillerFactory;

public abstract class QuarkusHeaderFillerFactoryBase implements HeaderFillerFactory {
    final Logger log = Logger.getLogger(QuarkusHeaderFillerFactoryBase.class);

    // filled in a generated constructor of the subclass
    protected final Map<Identifier, QuarkusHeaderFiller> map = new ConcurrentHashMap<>();

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public HeaderFiller createFiller(String value, String headerName, boolean required, Class<?> interfaceClass,
            Object ignored) {
        Identifier id = new Identifier(interfaceClass.getName(), value, headerName, required);
        QuarkusHeaderFiller quarkusHeaderFiller = map.get(id);
        if (quarkusHeaderFiller == null) {
            throw new IllegalStateException("No header filler value generator found for " + interfaceClass + ", "
                    + headerName);
        }
        quarkusHeaderFiller.initialize();
        return quarkusHeaderFiller;
    }

    public static abstract class QuarkusHeaderFiller implements HeaderFiller {
        @SuppressWarnings("unused") // used in the generated code
        protected static final Logger log = Logger.getLogger(QuarkusHeaderFiller.class);

        void initialize() {
            failIfInvalid();
        }

        @Override
        public abstract List<String> generateValues();

        public void failIfInvalid() {
        }
    }

    public static final class Identifier {
        final String className;
        final String value;
        final String headerName;
        final boolean required;

        public Identifier(String className, String value, String headerName, Boolean required) {
            this.className = className;
            this.value = value;
            this.headerName = headerName;
            this.required = required;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Identifier that = (Identifier) o;
            return required == that.required &&
                    Objects.equals(className, that.className) &&
                    Objects.equals(value, that.value) &&
                    Objects.equals(headerName, that.headerName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(className, value, headerName, required);
        }
    }
}
