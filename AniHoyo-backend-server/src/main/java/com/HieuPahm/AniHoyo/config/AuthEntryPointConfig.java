package com.HieuPahm.AniHoyo.config;

import java.io.IOException;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.HieuPahm.AniHoyo.domain.RestResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AuthEntryPointConfig implements AuthenticationEntryPoint {
     private final AuthenticationEntryPoint delegate = new BearerTokenAuthenticationEntryPoint();
    // mapper dùng để convert sang kiểu object
    private final ObjectMapper mapper;
    
    public AuthEntryPointConfig(ObjectMapper mapper){
        this.mapper = mapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {
        this.delegate.commence(request, response, authException);
        response.setContentType("application/json;charset=UTF-8"); //support vietnamese language

        RestResponse<Object> res = new RestResponse<>();
        res.setStatusCode(HttpStatus.UNAUTHORIZED.value());
        // handle error NULL value when not send anything
        String errorMess = Optional.ofNullable(authException.getCause())
                    .map(Throwable::getMessage)
                    .orElse(authException.getMessage());

        res.setError(errorMess);
        res.setMessage("Token not valid !! (có thể do sai định dạng hoặc hết hạn)");
        mapper.writeValue(response.getWriter(), res);
    }
}
