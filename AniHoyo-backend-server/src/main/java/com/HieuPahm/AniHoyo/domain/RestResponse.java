package com.HieuPahm.AniHoyo.domain;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RestResponse<T> {
    private int statusCode;
    private String error;
    // message maybe string or an object
    private Object message;
    private T data;

    /**
     * Request URI the response belongs to. Only filled in for error responses so
     * the success envelope the clients already rely on stays untouched.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String path;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Instant timestamp;

}
