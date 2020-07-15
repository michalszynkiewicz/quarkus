package io.quarkus.restclient.deployment;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;

import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParams;
import org.jboss.jandex.DotName;

public class DotNames {
    public static final DotName CLIENT_HEADER_PARAM = DotName.createSimple(ClientHeaderParam.class.getName());
    public static final DotName CLIENT_HEADER_PARAMS = DotName.createSimple(ClientHeaderParams.class.getName());
    public static final Set<DotName> JAXRS_ANNOTATIONS;
    public static final DotName STRING = DotName.createSimple(String.class.getName());
    public static final DotName LIST = DotName.createSimple(List.class.getName());

    static {
        Set<DotName> jaxrsAnnos = new HashSet<>();
        for (Class<?> methodAnnoClass : Arrays.asList(GET.class, POST.class, PUT.class, DELETE.class, HEAD.class, OPTIONS.class,
                PATCH.class)) {
            jaxrsAnnos.add(DotName.createSimple(methodAnnoClass.getName()));
        }
        JAXRS_ANNOTATIONS = Collections.unmodifiableSet(jaxrsAnnos);
    }
}
