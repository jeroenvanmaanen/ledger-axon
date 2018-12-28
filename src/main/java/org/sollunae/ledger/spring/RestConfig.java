package org.sollunae.ledger.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

import javax.servlet.MultipartConfigElement;
import java.lang.invoke.MethodHandles;

import static org.springframework.util.unit.DataSize.ofMegabytes;

@Configuration
public class RestConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final DataSize MAX_FILE_SIZE = ofMegabytes(100);

    @Bean(name = "multipartResolver")
    public StandardServletMultipartResolver multipartResolver() {
        LOGGER.info("Register standard servlet multipart resolver");
        return new StandardServletMultipartResolver();
    }

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize(MAX_FILE_SIZE);
        factory.setMaxRequestSize(MAX_FILE_SIZE);
        return factory.createMultipartConfig();
    }
}
