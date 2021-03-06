package com.wuhf.authentication.config;


import com.wuhf.authentication.config.handler.*;
import com.wuhf.authentication.config.serializer.FastJsonRedisTokenStoreSerializationStrategy;
import com.wuhf.authentication.service.OauthClientDetailsService;
import com.wuhf.authentication.service.OauthCodeService;
import com.wuhf.authentication.service.OauthUserDetailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.OAuth2RequestFactory;
import org.springframework.security.oauth2.provider.approval.UserApprovalHandler;
import org.springframework.security.oauth2.provider.request.DefaultOAuth2RequestFactory;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.redis.RedisTokenStore;

/**
 * Oauth????????????????????????Oauth?????????????????????
 * @author alex
 * @date 2020/07/21
 */
//@Slf4j
//@Configuration
public class OAuth2ServerConfiguration {

    public static final String RESOURCE_ID = "USER-RESOURCE";

    /**
     * ?????????????????????
     */
    @Configuration
    @EnableResourceServer
    protected class ApiResourceServerConfiguration extends ResourceServerConfigurerAdapter {

        @Autowired
        private OauthTokenExtractor oauthTokenExtractor;
        @Autowired
        private OauthExceptionEntryPoint oauthExceptionEntryPoint;
        @Autowired
        private OauthAccessDeniedHandler oauthAccessDeniedHandler;

        @Override
        public void configure(ResourceServerSecurityConfigurer resources) {
            resources.resourceId(RESOURCE_ID).stateless(false);
            // token?????????
            resources.tokenExtractor(oauthTokenExtractor)
                    // token???????????????
                    .authenticationEntryPoint(oauthExceptionEntryPoint)
                    // ????????????????????????
                    .accessDeniedHandler(oauthAccessDeniedHandler);
        }

        @Override
        public void configure(HttpSecurity http) throws Exception {
            http
                // STATELESS?????????????????????access_token???????????????????????????session??????
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
                .requestMatchers().antMatchers("/get/**")
            .and()
                .authorizeRequests()
                .antMatchers("/get/**").access("#oauth2.hasScope('read')");
        }
    }

    /**
     * ?????????????????????
     */
    @Configuration
    @EnableResourceServer
    protected class SyncResourceServerConfiguration extends ResourceServerConfigurerAdapter {

        @Autowired
        private OauthTokenExtractor oauthTokenExtractor;
        @Autowired
        private OauthExceptionEntryPoint oauthExceptionEntryPoint;
        @Autowired
        private OauthAccessDeniedHandler oauthAccessDeniedHandler;

        @Override
        public void configure(ResourceServerSecurityConfigurer resources) {
            resources.resourceId(RESOURCE_ID).stateless(false);
            // token?????????
            resources.tokenExtractor(oauthTokenExtractor)
                    // token???????????????
                    .authenticationEntryPoint(oauthExceptionEntryPoint)
                    // ????????????????????????
                    .accessDeniedHandler(oauthAccessDeniedHandler);
        }

        @Override
        public void configure(HttpSecurity http) throws Exception {
            http
                // STATELESS?????????????????????access_token???????????????????????????session??????
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
                .requestMatchers().antMatchers("/update/**")
            .and()
                .authorizeRequests()
                .antMatchers("/update/**").access("#oauth2.hasScope('write') && hasRole('admin')");
        }
    }

    /**
     * oauth2.0?????????????????????
     */
    @Configuration
    @EnableAuthorizationServer
    protected class AuthorizationServerConfiguration extends AuthorizationServerConfigurerAdapter {
        @Autowired
        private TokenStore tokenStore;

        @Autowired
        private OauthClientDetailsService oauthClientDetailsService;

        @Autowired
        private OauthCodeService authorizationCodeServices;

        @Autowired
        private OauthUserDetailService userDetailService;

        @Autowired
        @Qualifier("authenticationManagerBean")
        private AuthenticationManager authenticationManager;
        @Autowired
        private OauthWebResponseExceptionTranslator oauthWebResponseExceptionTranslator;

        @Override
        public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
            clients.withClientDetails(oauthClientDetailsService);
        }

        /**
         * token ????????????????????????redis
         * @param connectionFactory
         * @return
         */
        @Bean
        public TokenStore tokenStore(RedisConnectionFactory connectionFactory) {
            final RedisTokenStore redisTokenStore = new RedisTokenStore(connectionFactory);
            // ??????
            redisTokenStore.setPrefix("TOKEN:");
            // ????????????????????????fastjson
            redisTokenStore.setSerializationStrategy(new FastJsonRedisTokenStoreSerializationStrategy());
            return redisTokenStore;
        }

        /**
         * ??????????????????
         * @param endpoints
         * @throws Exception
         */
        @Override
        public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
            endpoints.tokenStore(tokenStore)
                    .allowedTokenEndpointRequestMethods(HttpMethod.GET, HttpMethod.POST)
                    // ???????????????????????????
                    .exceptionTranslator(oauthWebResponseExceptionTranslator)
                    // ??????????????????????????????code??????????????????????????????redis??????
                    .authorizationCodeServices(authorizationCodeServices)
                    // ????????????service
                    .userDetailsService(userDetailService)
                    // ???????????????????????????
                    .userApprovalHandler(userApprovalHandler())
                    // ??????authenticationManager?????????password??????
                    .authenticationManager(authenticationManager)
                    // ???????????????????????????
                    .pathMapping("/oauth/confirm_access", "/approval");
        }

        /**
         * AuthorizationServer????????????/oauth/**?????????????????????????????????????????????????????????????????????
         * @param oauthServer
         * @throws Exception
         */
        @Override
        public void configure(AuthorizationServerSecurityConfigurer oauthServer) throws Exception {
            // ?????? /oauth/token?????????????????????
            oauthServer.allowFormAuthenticationForClients()
                    .tokenKeyAccess("permitAll()")
                    // ?????? /oauth/token_check???????????????
                    .checkTokenAccess("permitAll()");
        }

        @Bean
        public OAuth2RequestFactory oAuth2RequestFactory() {
            return new DefaultOAuth2RequestFactory(oauthClientDetailsService);
        }

        @Bean
        public UserApprovalHandler userApprovalHandler() {
            OauthUserApprovalHandler userApprovalHandler = new OauthUserApprovalHandler();
            userApprovalHandler.setTokenStore(tokenStore);
            userApprovalHandler.setClientDetailsService(oauthClientDetailsService);
            userApprovalHandler.setRequestFactory(oAuth2RequestFactory());
            return userApprovalHandler;
        }

    }
}
