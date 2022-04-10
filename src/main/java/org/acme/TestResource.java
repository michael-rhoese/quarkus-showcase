package org.acme;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicReference;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/call")
public class TestResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestResource.class);

    @Inject
    ManagedExecutor executor;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response hello() throws InterruptedException {

        final var uploadDO = new UploadDO();

        executor.supplyAsync(() -> uploadDO)
                .thenApplyAsync(this::action)
                .thenApplyAsync(this::action)
                .thenApplyAsync(this::action)
                .thenApplyAsync(this::action)
                .thenApplyAsync(this::action)
                .exceptionallyAsync(this::doSomethingOnError);

        return Response.accepted().build();
    }

    private UploadDO action(UploadDO value) {

        if (StringUtils.countMatches(value.test.get(), "Stage") == 3) {
            LOGGER.info("Throws the excemption");
            throw new VirusScannerTaskException(value);
        }
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) { // NOSONAR
            throw new UploadTaskException(value, ex);
        }
        value.test.set(value.test.get() + " Stage");
        LOGGER.info(value.test.get());
        return value;
    }

    private UploadDO doSomethingOnError(final Throwable exception) {
        final UploadTaskDataProvider casted = (UploadTaskDataProvider) exception.getCause();
        // Clean DB entry
        LOGGER.info(
                "Exception handled for {}. ChronicleID={} | Version={}",
                exception.getMessage(),
                casted.getUploadDO().chronicleId,
                casted.getUploadDO().version
        );
        return null;
    }

    /**
     * Container to pass all required data to the async chain. That allowes a cleanup if any error occurres while:
     * <ul>
     * <li>The virus scanning process OR</li>
     * <li>Uploading to hdco dctm.</li>
     * </ul>
     */
    public static class UploadDO implements Serializable {

        private static final long serialVersionUID = 6077862470946954237L;

        // Variable for storing testing
        final AtomicReference<String> test = new AtomicReference<>("");

        // Reference to the file
        String path = "path";
        // Data to change state or delete corresponding db entity
        String chronicleId = "1234567891";
        String version = "0.1";

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
        }
    }

    public static interface UploadTaskDataProvider {

        UploadDO getUploadDO();
    }

    public static class VirusScannerTaskException extends RuntimeException implements UploadTaskDataProvider {

        private static final long serialVersionUID = -5217859170393982971L;

        private final UploadDO uploadDO;

        public VirusScannerTaskException(UploadDO uploadDO) {
            this.uploadDO = uploadDO;
        }

        @Override
        public UploadDO getUploadDO() {
            return uploadDO;
        }
    }

    public static class UploadTaskException extends RuntimeException implements UploadTaskDataProvider {

        private static final long serialVersionUID = -5217859170393982971L;

        private final UploadDO uploadDO;

        public UploadTaskException(UploadDO uploadDO) {
            this.uploadDO = uploadDO;
        }

        public UploadTaskException(UploadDO uploadDO, Throwable cause) {
            super(cause);
            this.uploadDO = uploadDO;
        }

        @Override
        public UploadDO getUploadDO() {
            return uploadDO;
        }
    }
}
