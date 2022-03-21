package com.wuhf.authentication.config.handler;

import com.wuhf.authentication.common.enums.ResultStatusCode;
import com.wuhf.authentication.common.utils.ResultUtil;
import com.wuhf.authentication.common.vo.Result;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author alex
 * @date 2020/07/23
 */
@Component
public class OauthAccessDeniedHandler implements AccessDeniedHandler {
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) {
        accessDeniedException.printStackTrace();
        response.setStatus(HttpStatus.OK.value());
        ResultUtil.writeJavaScript(response, Result.error(ResultStatusCode.PERMISSION_DENIED));
    }
}
