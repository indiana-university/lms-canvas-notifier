package edu.iu.uits.lms.canvasnotifier.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationEventPublisher;
import org.springframework.security.authorization.event.AuthorizationEvent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
@Slf4j
public class LmsAuthorizationEventPublisher implements AuthorizationEventPublisher {
    private ApplicationEventPublisher applicationEventPublisher;

    public LmsAuthorizationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public <T> void publishAuthorizationEvent(Supplier<Authentication> authentication,
                                              T object, AuthorizationDecision decision) {
        applicationEventPublisher.publishEvent(new AuthorizationEvent(authentication, object, decision));
    }
}