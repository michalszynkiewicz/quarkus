package io.quarkus.it.rest.client.multipart;

import static java.nio.charset.StandardCharsets.UTF_8;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.MultipartForm;
import org.jboss.resteasy.reactive.PartType;

import io.smallrye.common.annotation.Blocking;

@Path("")
public class MultipartResource {

    private static final Logger log = Logger.getLogger(MultipartResource.class);

    public static final String HELLO_WORLD = "HELLO WORLD";
    public static final String GREETING_TXT = "greeting.txt";
    @RestClient
    MultipartClient client;

    @GET
    @Path("/client")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @Blocking
    public String sendFile() throws Exception {
        io.quarkus.it.rest.client.multipart.MultipartBody data = new io.quarkus.it.rest.client.multipart.MultipartBody();
        data.file = HELLO_WORLD.getBytes(UTF_8);
        data.fileName = GREETING_TXT;
        return client.sendMultipartData(data);
        // mstodo
        //        MultipartBody body = new MultipartBody();
        //        body.fileName = GREETING_TXT;
        //        body.file = HELLO_WORLD.getBytes(UTF_8);
        //        return client.sendMultipartData(body);
        //        return "";
    }

    @POST
    @Path("/echo")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public String consumeMultipart(@MultipartForm MultipartBody body) {
        return String.format("fileOk:%s,nameOk:%s", isEqualTo(body.file, HELLO_WORLD), GREETING_TXT.equals(body.fileName));
    }

    private Object isEqualTo(byte[] bytes, String content) {
        return content.equals(new String(bytes, UTF_8));
    }

    public static class MultipartBody {

        @FormParam("file")
        @PartType(MediaType.APPLICATION_OCTET_STREAM)
        public byte[] file;

        @FormParam("fileName")
        @PartType(MediaType.TEXT_PLAIN)
        public String fileName;
    }

}
