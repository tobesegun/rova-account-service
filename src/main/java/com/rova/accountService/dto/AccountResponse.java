package com.rova.accountService.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountResponse<T> {
    private String respCode;
    private String respDescription;
    private T respBody;
    @JsonIgnore
    private HttpStatus httpStatus;

    public AccountResponse(String description, String code, HttpStatus status){
        respCode = code;
        httpStatus = status;
        respDescription = description;
    }

    @Override
    public String toString() {
        return "Response{" +
                "respCode='" + respCode + '\'' +
                ", respDescription='" + respDescription + '\'' +
                ", respBody=" + respBody +
                '}';
    }
}
