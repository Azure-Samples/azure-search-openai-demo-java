package com.microsoft.openai.samples.rag.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class LoggedUserService {

    public LoggedUser getLoggedUser(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        //this is always true in the PoC code
        if(authentication == null) {
           return getDefaultUser();
        }
        //this code is never executed in the PoC. It's a hook for future improvements requiring integration with authentication providers.
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {

            String currentUserName = jwtAuthentication.getName();
            String displayName = jwtAuthentication.getToken().getClaimAsString("preferred_username");
            String oid = jwtAuthentication.getToken().getClaimAsString("oid");

            return new LoggedUser(currentUserName, displayName, "",displayName, oid);
        }
        return getDefaultUser();
    }

    private LoggedUser getDefaultUser() {
        return new LoggedUser("bob.user@contoso.com", "bob.user@contoso.com", "generic", "Bob The User", "default");
    }
}
