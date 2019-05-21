package com.frame.common.auth.shiro;

import com.frame.common.auth.jwt.JwtFilter;
import com.frame.common.auth.jwt.JwtRealm;
import com.frame.common.base.config.FrameProperties;
import com.frame.common.base.constant.CacheConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.authc.pam.FirstSuccessfulStrategy;
import org.apache.shiro.authc.pam.ModularRealmAuthenticator;
import org.apache.shiro.cache.CacheManager;
import org.apache.shiro.mgt.DefaultSessionStorageEvaluator;
import org.apache.shiro.mgt.DefaultSubjectDAO;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.MethodInvokingFactoryBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 权限检查类
 *
 * @author ly
 * @version 2016年5月20日 下午3:44:45
 */
@Slf4j
@Configuration
public class ShiroConfig {

    /**
     * 系统参数设置
     */
    @Autowired
    private FrameProperties frameProperties;


    /**
     * 缓存管理器 根据插件自动注入
     */
    @Autowired
    @Qualifier("shiroCache")
    private CacheManager shiroCache;

    /**
     * 定义多个realm的认证策略配置,使用FirstSuccessfulStrategy
     * @return
     */
    @Bean
    public ModularRealmAuthenticator modularRealmAuthenticator() {
        ModularRealmAuthenticator modularRealmAuthenticator = new ModularRealmAuthenticator();
        modularRealmAuthenticator.setAuthenticationStrategy(new FirstSuccessfulStrategy());
        return modularRealmAuthenticator;
    }

    /**
     * 安全管理器
     */
    @Bean
    public DefaultWebSecurityManager securityManager() {
        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();

        securityManager.setAuthenticator(modularRealmAuthenticator());
        Collection<Realm> realms = new ArrayList<>();
        realms.add(jwtRealm());
        realms.add(shiroRealm());
        securityManager.setCacheManager(shiroCache);

        securityManager.setRealms(realms);
         /*
         * 关闭shiro自带的session，详情见文档
         * http://shiro.apache.org/session-management.html#SessionManagement-StatelessApplications%28Sessionless%29
         */
        ((DefaultSessionStorageEvaluator) ((DefaultSubjectDAO) securityManager.getSubjectDAO())
                .getSessionStorageEvaluator()).setSessionStorageEnabled(false);
        securityManager.setSubjectFactory(new AgileSubjectFactory());

        return securityManager;
    }


    /**
     * 項目自定义的Realm -
     *
     * @return
     */
    @Bean(initMethod = "onInit")
    public JwtRealm jwtRealm() {
        JwtRealm jwtRealm = new JwtRealm();

        //Authorization
        jwtRealm.setAuthorizationCachingEnabled(true);
        //缓存AuthenticationInfo信息的缓存名称
        jwtRealm.setAuthorizationCacheName(CacheConstant.SHIRO_JWT_REALM_AUTHORIZATION);
        //启用身份验证缓存，即缓存AuthenticationInfo信息，默认false
        jwtRealm.setAuthenticationCachingEnabled(false);
        jwtRealm.setCacheManager(shiroCache);


        return jwtRealm;
    }

    /**
     * shiro密码加密配置
     *
     * @return
     */
    @Bean
    public PasswordHash passwordHash() {
        //密码加密 1次md5,增强密码可修改此处
        PasswordHash passwordHash = new PasswordHash();
        passwordHash.setAlgorithmName("MD5");
        passwordHash.setHashIterations(1);
        return passwordHash;
    }

    /**
     * 項目自定义的Realm -
     *
     * @return
     */
    @Bean
    public ShiroRealm shiroRealm() {
        ShiroRealm shiroRealm = new ShiroRealm();
        shiroRealm.setCredentialsMatcher(hashedCredentialsMatcher(passwordHash()));
        shiroRealm.setAuthorizationCachingEnabled(false);
        //启用身份验证缓存，即缓存AuthenticationInfo信息，默认false
        shiroRealm.setAuthenticationCachingEnabled(false);

        return shiroRealm;
    }

    @Bean
    public KickOutFilter kickOutFilter() {
        KickOutFilter kickOutFilter = new KickOutFilter();
        //使用cacheManager获取相应的cache来缓存用户登录的会话；用于保存用户—会话之间的关系的；
        //也可以重新另写一个，重新配置缓存时间之类的自定义缓存属性
        kickOutFilter.setCacheManager(shiroCache);
        //是否踢出后来登录的，默认是false；即后者登录的用户踢出前者登录的用户；踢出顺序。
        kickOutFilter.setKickOutAfter(this.frameProperties.getAuth().isKickOutAfter());
        //同一个用户最大的会话数，默认1；比如2的意思是同一个用户允许最多同时两个人登录；
        kickOutFilter.setMaxSession(this.frameProperties.getAuth().getKickOutMaxSession());
        return kickOutFilter;
    }

    @Bean
    public FilterRegistrationBean shiroKickOutFilter(KickOutFilter filter) {
        FilterRegistrationBean registration = new FilterRegistrationBean(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public JwtFilter jwtFilter() {
        JwtFilter jwtFilter = new JwtFilter();
        //使用cacheManager获取相应的cache来缓存用户登录的会话；用于保存用户—会话之间的关系的；
        //也可以重新另写一个，重新配置缓存时间之类的自定义缓存属性
        jwtFilter.setCacheManager(shiroCache);
        return jwtFilter;
    }

    @Bean
    public FilterRegistrationBean shiroJwtFilter(JwtFilter filter) {
        FilterRegistrationBean registration = new FilterRegistrationBean(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public HeaderFilter headerFilter() {
        HeaderFilter headerFilter = new HeaderFilter();
        return headerFilter;
    }

    @Bean
    public FilterRegistrationBean shiroHeaderFilter(HeaderFilter filter) {
        FilterRegistrationBean registration = new FilterRegistrationBean(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean("shiroFilter")
    public ShiroFilterFactoryBean shiroFilter(DefaultWebSecurityManager securityManager) {
        ShiroFilterFactoryBean shiroFilter = new ShiroFilterFactoryBean();
        //配置securityManager
        shiroFilter.setSecurityManager(securityManager);

        // 添加自己的过滤器并且取名为jwt
        Map<String, javax.servlet.Filter> filterMap = new LinkedHashMap<>();
        filterMap.put("header", headerFilter());
        filterMap.put("user", jwtFilter());
        if (this.frameProperties.getAuth().isKickOutValid()) {
            filterMap.put("kickOut", kickOutFilter());
        }
        shiroFilter.setFilters(filterMap);

        shiroFilter.setLoginUrl("/api/login");

        // 自定义url规则
        Map<String, String> filterChainDefinitionMap = new LinkedHashMap<>();

        String[] ignoreUrl = this.frameProperties.getAuth().getIgnoreUrl();
        if (!CollectionUtils.sizeIsEmpty(ignoreUrl)) {
            for (int i = 0; i < ignoreUrl.length; i++) {
                filterChainDefinitionMap.put(ignoreUrl[i], "header,anon");
            }
        }
        //开放的静态资源 网站图标
        filterChainDefinitionMap.put("/favicon.ico", "anon");
        filterChainDefinitionMap.put("/css/**", "anon");
        filterChainDefinitionMap.put("/js/**", "anon");
        filterChainDefinitionMap.put("/img/**", "anon");
        filterChainDefinitionMap.put("/fonts/**", "anon");
        filterChainDefinitionMap.put("/swagger/**", "anon");

        filterChainDefinitionMap.put("/**/*.css", "anon");
        filterChainDefinitionMap.put("/**/*.js", "anon");
        filterChainDefinitionMap.put("/**/*.html", "anon");
        // 所有请求通过我们自己的JWT Filter

        filterChainDefinitionMap.put("/api/login", "header,anon");
        if (this.frameProperties.getAuth().isKickOutValid()) {
            filterChainDefinitionMap.put("/api/**", "header,user,kickOut");
        } else {
            filterChainDefinitionMap.put("/api/**", "header,user");
        }
        filterChainDefinitionMap.put("/test/**", "anon");

        shiroFilter.setFilterChainDefinitionMap(filterChainDefinitionMap);
        return shiroFilter;
    }

    @Bean
    public HashedCredentialsMatcher hashedCredentialsMatcher(PasswordHash passwordHash) {
        HashedCredentialsMatcher hashedCredentialsMatcher = new RetryLimitCredentialsMatcher(shiroCache, this.frameProperties.getAuth().getPasswordRetryLimit());
        //散列算法
        hashedCredentialsMatcher.setHashAlgorithmName(passwordHash.getAlgorithmName());
        //散列次数
        hashedCredentialsMatcher.setHashIterations(passwordHash.getHashIterations());

        return hashedCredentialsMatcher;
    }


    /**
     * 开启shiro aop注解支持
     */
    @Bean
    public AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor(SecurityManager securityManager) {
        AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor = new AuthorizationAttributeSourceAdvisor();
        authorizationAttributeSourceAdvisor.setSecurityManager(securityManager);
        return authorizationAttributeSourceAdvisor;
    }

    /**
     * DefaultAdvisorAutoProxyCreator，Spring的一个bean，由Advisor决定对哪些类的方法进行AOP代理。
     */
    @Bean
    public DefaultAdvisorAutoProxyCreator defaultAdvisorAutoProxyCreator() {
        DefaultAdvisorAutoProxyCreator defaultAAP = new DefaultAdvisorAutoProxyCreator();
        defaultAAP.setProxyTargetClass(true);
        return defaultAAP;
    }

    @Bean
    public MethodInvokingFactoryBean methodInvokingFactoryBean(SecurityManager securityManager) {
        MethodInvokingFactoryBean bean = new MethodInvokingFactoryBean();
        bean.setStaticMethod("org.apache.shiro.SecurityUtils.setSecurityManager");
        bean.setArguments(securityManager);
        return bean;
    }
}
