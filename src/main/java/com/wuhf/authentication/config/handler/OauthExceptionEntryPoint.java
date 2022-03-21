package com.wuhf.authentication.config.handler;

import com.wuhf.authentication.common.enums.ResultStatusCode;
import com.wuhf.authentication.common.utils.ResultUtil;
import com.wuhf.authentication.common.vo.Result;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author alex
 * @date 2020/07/23
 */
@Component
public class OauthExceptionEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException){
        authException.printStackTrace();
        Throwable cause = authException.getCause();

        response.setStatus(HttpStatus.OK.value());
        if(cause instanceof InvalidTokenException) {
            ResultUtil.writeJavaScript(response, Result.error(ResultStatusCode.INVALID_TOKEN));

        }else{
            ResultUtil.writeJavaScript(response, Result.error(ResultStatusCode.TOKEN_MISS));
        }
    }
}
