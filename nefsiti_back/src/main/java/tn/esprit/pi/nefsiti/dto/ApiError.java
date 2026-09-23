package tn.esprit.pi.nefsiti.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(int status, String message, Map<String, String> erreurs) {

    public ApiError(int status, String message) {
        this(status, message, null);
    }
}
