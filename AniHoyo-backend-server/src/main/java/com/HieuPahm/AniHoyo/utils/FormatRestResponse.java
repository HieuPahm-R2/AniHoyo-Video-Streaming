package com.HieuPahm.AniHoyo.utils;

import java.util.Map;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import com.HieuPahm.AniHoyo.domain.RestResponse;
import com.HieuPahm.AniHoyo.utils.anotation.MessageApi;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.io.Resource;

@ControllerAdvice
public class FormatRestResponse implements ResponseBodyAdvice<Object> {
	@Override
	public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
		return true;
	}

	@Override
	@Nullable
	public Object beforeBodyWrite(@Nullable Object arg0,
			MethodParameter arg1,
			MediaType arg2,
			Class<? extends HttpMessageConverter<?>> arg3,
			ServerHttpRequest arg4,
			ServerHttpResponse arg5) {
		// Only a data-binding converter (Jackson here) can serialize the envelope in
		// place of the body the controller declared. Every other converter casts the
		// value to the type it was chosen for, so swapping the body in blows up:
		//   * ResponseEntity<Resource>      -> ResourceHttpMessageConverter
		//     "RestResponse cannot be cast to Resource"
		//   * a Range request on a resource -> List<ResourceRegion> served by
		//     ResourceRegionHttpMessageConverter (generic, but its writeInternal
		//     casts to Collection)
		//     "RestResponse cannot be cast to java.util.Collection"
		//   * String / byte[] bodies        -> same story
		// Returning the value untouched keeps Spring's own body (null for redirects,
		// 204/304, empty 404s and HEAD requests) and its status/headers intact.
		if (!canWriteEnvelope(arg3)) {
			return arg0;
		}

		HttpServletResponse servletResponse = ((ServletServerHttpResponse) arg5).getServletResponse();
		int status = servletResponse.getStatus();
		RestResponse<Object> res = new RestResponse<>();
		res.setStatusCode(status);

		if (arg0 instanceof String || arg0 instanceof Resource) {
			return arg0;
		}
		String path = arg4.getURI().getPath();
		if (path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui")) {
			return arg0;
		}
		// ========= error case=========
		if (status >= 400) {
			// Already built by GlobalException (or by AuthEntryPointConfig) — keep it.
			if (arg0 instanceof RestResponse) {
				return arg0;
			}
			// Otherwise this is Spring's own error body (BasicErrorController,
			// filter-chain errors, container errors...). Re-shape it so clients always
			// receive the same envelope. `data` stays null on purpose: call sites treat
			// a truthy `data` as success, and an error must never look like one.
			return wrapErrorBody(arg0, status, path);
		} else {
			// ====== success case =======
			res.setData(arg0);
			MessageApi message = arg1.getMethodAnnotation(MessageApi.class);
			res.setMessage(message != null ? message.value() : "API HAS BEEN SUCCESSFULLY CALLED");
		}
		return res;
	}

	/**
	 * Can the converter that Spring already picked serialize a {@link RestResponse}
	 * instead of the body type it was selected for?
	 *
	 * <p>Only Jackson-style data binders can (JSON/XML/Smile/CBOR — all of them
	 * extend {@link AbstractJackson2HttpMessageConverter}); the resource, region,
	 * string and byte-array converters cast their argument and would throw a
	 * {@code ClassCastException} instead of writing anything.
	 */
	private static boolean canWriteEnvelope(Class<? extends HttpMessageConverter<?>> converterType) {
		return AbstractJackson2HttpMessageConverter.class.isAssignableFrom(converterType);
	}

	private RestResponse<Object> wrapErrorBody(Object body, int status, String path) {
		RestResponse<Object> res = new RestResponse<>();
		res.setStatusCode(status);

		String error = null;
		Object message = null;
		if (body instanceof Map<?, ?> map) {
			Object rawError = map.get("error");
			Object rawMessage = map.get("message");
			error = rawError != null ? String.valueOf(rawError) : null;
			message = rawMessage;
			if (message == null && map.get("detail") != null) {
				message = map.get("detail");
			}
		} else if (body != null && !(body instanceof RestResponse)) {
			error = String.valueOf(body);
		}

		String messageText = message != null ? String.valueOf(message) : "Yêu cầu thất bại (HTTP " + status + ")";
		res.setMessage(messageText);
		res.setError(error != null ? error : messageText);
		res.setPath(path);
		return res;
	}

}
