package com.HieuPahm.AniHoyo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InterceptorConfiguration implements WebMvcConfigurer {
    @Bean
    AuthorityIntercepter getAuthorityInterceptor() {
        return new AuthorityIntercepter();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Paths that the filter chain already serves without a token (see
        // SecurityConfiguration / PublicPaths) must not be re-checked against the
        // `permissions` table, otherwise a logged-in user is refused access to a
        // public endpoint. `/api/v1/files` and `/api/v1/ratings/**` are deliberate
        // ACL exceptions of this project and are kept as they were.
        String[] publicPaths = PublicPaths.all();
        String[] whiteList = new String[publicPaths.length + 2];
        System.arraycopy(publicPaths, 0, whiteList, 0, publicPaths.length);
        whiteList[publicPaths.length] = "/api/v1/files";
        whiteList[publicPaths.length + 1] = "/api/v1/ratings/**";
        registry.addInterceptor(getAuthorityInterceptor()).excludePathPatterns(whiteList);
    }
}
