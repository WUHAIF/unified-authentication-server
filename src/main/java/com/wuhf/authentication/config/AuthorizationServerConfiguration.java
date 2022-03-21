package com.wuhf.authentication.config;

import com.wuhf.authentication.config.handler.OauthUserApprovalHandler;
import com.wuhf.authentication.config.handler.OauthWebResponseExceptionTranslator;
import com.wuhf.authentication.config.serializer.FastJsonRedisTokenStoreSerializationStrategy;
import com.wuhf.authentication.service.OauthClientDetailsService;
import com.wuhf.authentication.service.OauthCodeService;
import com.wuhf.authentication.service.OauthUserDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.OAuth2RequestFactory;
import org.springframework.security.oauth2.provider.approval.UserApprovalHandler;
import org.springframework.security.oauth2.provider.request.DefaultOAuth2RequestFactory;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.redis.RedisTokenStore;

/**
 * @Description: oauth2.0授权服务器配置
 * @author: wuhaifeng
 * @date: 2022年03月21日 23:30
 */
@Configuration
@EnableAuthorizationServer
public class AuthorizationServerConfiguration extends AuthorizationServerConfigurerAdapter {


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


    /**
     * ClientDetailsServiceConfigurer：
     * 用来配置客户端详情服务（ClientDetailsService），
     * 客户端详情信息在 这里进行初始化，
     * 你能够把客户端详情信息写死在这里或者是通过数据库来存储调取详情信息。
     * @param clients
     * @throws Exception
     */
    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
        clients.withClientDetails(oauthClientDetailsService);
    }

    /**
     * token 存储处理类，使用redis
     * @param connectionFactory
     * @return
     */
    @Bean
    public TokenStore tokenStore(RedisConnectionFactory connectionFactory) {
        final RedisTokenStore redisTokenStore = new RedisTokenStore(connectionFactory);
        // 前缀
        redisTokenStore.setPrefix("TOKEN:");
        // 序列化策略，使用fastjson
        redisTokenStore.setSerializationStrategy(new FastJsonRedisTokenStoreSerializationStrategy());
        return redisTokenStore;
    }

    /**
     * 认证端点配置
     * AuthorizationServerEndpointsConfigurer：用来配置令牌（token）的访问端点和令牌服务(token services)。
     * @param endpoints
     * @throws Exception
     */
    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
        endpoints.tokenStore(tokenStore)
                .allowedTokenEndpointRequestMethods(HttpMethod.GET, HttpMethod.POST)
                // 自定义认证异常处理
                .exceptionTranslator(oauthWebResponseExceptionTranslator)
                // 自定义的授权码模式的code（授权码）处理，使用redis存储
                .authorizationCodeServices(authorizationCodeServices)
                // 用户信息service
                .userDetailsService(userDetailService)
                // 用户授权确认处理器
                .userApprovalHandler(userApprovalHandler())
                // 注入authenticationManager来支持password模式
                .authenticationManager(authenticationManager)
                // 自定义授权确认页面
                .pathMapping("/oauth/confirm_access", "/approval");
    }

    /**
     * 用来配置令牌端点的安全约束
     * AuthorizationServer的端点（/oauth/**）安全配置（访问规则、过滤器、返回结果处理等）
     * @param oauthServer
     * @throws Exception
     */
    @Override
    public void configure(AuthorizationServerSecurityConfigurer oauthServer) throws Exception {
        // 允许 /oauth/token的端点表单认证
        oauthServer.allowFormAuthenticationForClients()
                .tokenKeyAccess("permitAll()")
                // 允许 /oauth/token_check端点的访问
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