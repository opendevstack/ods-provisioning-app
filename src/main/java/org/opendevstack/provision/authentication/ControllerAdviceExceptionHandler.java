package org.opendevstack.provision.authentication;

import org.opendevstack.provision.adapter.IODSAuthnzAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class ControllerAdviceExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ControllerAdviceExceptionHandler.class);

    @Autowired
    private IODSAuthnzAdapter manager;

    @ExceptionHandler(MissingCredentialsInfoException.class)
    public ResponseEntity handleException(MissingCredentialsInfoException ex, WebRequest request) {

        String username = request.getRemoteUser();

        // Clean up spring security context
        SecurityContextHolder.clearContext();
        logger.info("Spring security context cleared!!");

        try {
            // Invalidate indentity
            manager.invalidateIdentity();
            logger.info("Identity invalidated!");
        } catch (Exception e) {
            logger.warn("Error while trying to invalidate identity!");
        }

        logger.error("Removed user session for user '" + username + "'!", ex);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

}
