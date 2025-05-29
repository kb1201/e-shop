package hr.fer.dipl.client;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class FeignClientInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        // Get the current token (from context, request, etc.)
        var token = ((UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication()).getDetails();

        template.header("Authorization", "Bearer " + token);
    }

}
