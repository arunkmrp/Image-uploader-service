package com.villagetalkies;

import org.glassfish.jersey.media.multipart.MultiPartFeature;

import com.villagetalkies.resources.ImageResource;

import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import java.util.EnumSet;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterRegistration;

public class ImageUploadServiceApplication extends Application<ImageUploadServiceConfiguration> {

    public static void main(final String[] args) throws Exception {
        new ImageUploadServiceApplication().run(args);
    }

    @Override
    public String getName() {
        return "ImageUploadService";
    }

    @Override
    public void initialize(final Bootstrap<ImageUploadServiceConfiguration> bootstrap) {
        // TODO: application initialization
    }

    @Override
    public void run(final ImageUploadServiceConfiguration configuration,
            final Environment environment) {
        // TODO: implement application
        // Register multipart support
        environment.jersey().register(MultiPartFeature.class);

        // Register the ImageResource here
        environment.jersey().register(new ImageResource());
        final FilterRegistration.Dynamic cors = environment.servlets().addFilter("CORS", CrossOriginFilter.class);

        // Configure CORS parameters
        cors.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*"); // Allow all origins
        cors.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM,
                "X-Requested-With,Content-Type,Accept,Origin");
        cors.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM,
                "OPTIONS,GET,PUT,POST,DELETE,HEAD");
        cors.setInitParameter(CrossOriginFilter.ALLOW_CREDENTIALS_PARAM, "true");

        // Add URL mapping
        cors.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true,
                "/*");

    }

}
