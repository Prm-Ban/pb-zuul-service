package com.sunwell.zuul.filters;


import com.netflix.zuul.ZuulFilter;

import com.netflix.zuul.context.RequestContext;
import com.sunwell.zuul.model.UserInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AuthenticationFilter extends ZuulFilter {
    private static final int FILTER_ORDER =  2;
    private static final boolean  SHOULD_FILTER=true;
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);

    @Autowired
    FilterUtils filterUtils;

    @Autowired
    RestTemplate restTemplate;

    @Override
    public String filterType() {
        return filterUtils.PRE_FILTER_TYPE;
    }

    @Override
    public int filterOrder() {
        return FILTER_ORDER;
    }

    @Override
    public boolean shouldFilter() {
        return SHOULD_FILTER;
    }

    private boolean isAuthTokenPresent() {
        if (filterUtils.getAuthToken() !=null){
            return true;
        }

        return false;
    }

    private Object isAuthTokenValid(){
    	logger.debug("isAuthTokenValid() called");
        ResponseEntity restExchange = null;
        try {
        	logger.debug("isAuthTokenValid() is calling rest");
            restExchange =
                    restTemplate.exchange(
                            "http://authentication-service/authentication/resources/userinfo",
                            HttpMethod.GET,
                            null, Object.class, filterUtils.getAuthToken());
        }
        catch(HttpClientErrorException ex){
        	logger.debug("Exception() occured");
        	ex.printStackTrace();
            if (ex.getStatusCode()==HttpStatus.UNAUTHORIZED) {
                return null;
            }

            throw ex;
        }
        catch(Exception ex) {
        	logger.debug("Exception() occured");
        	ex.printStackTrace();
        	throw ex;
        }
        logger.debug("isAuthTokenValid() is exiting");
        return restExchange.getBody();
    }

    @Override
    public Object run() {
        RequestContext ctx = RequestContext.getCurrentContext();
    	System.out.println("RUN CALLED");
        //If we are dealing with a call to the authentication service, let the call go through without authenticating
        if ( ctx.getRequest().getRequestURI().equals("http://localhost:8090/authentication/resources/userinfo")){
            return null;
        }

        if (isAuthTokenPresent()){
           logger.debug("Authentication token is present here.");
        }
        else{
            logger.debug("Authentication token is not present.");

            ctx.setResponseStatusCode(HttpStatus.UNAUTHORIZED.value());
            ctx.setSendZuulResponse(false);
        }

//        Object userInfo = isAuthTokenValid();
//        logger.debug("User info: " + userInfo.getOrganizationId());
//        if (userInfo!=null){
//            logger.debug("Authentication token is valid.");
//            filterUtils.setUserId(userInfo.getUserId());
//            filterUtils.setOrgId(userInfo.getOrganizationId());
//            return null;
//        }
//        else {
//        	logger.debug("Auth token is not valid.");
//        }
////
//        logger.debug("Authentication token is not valid.");
//        ctx.setResponseStatusCode(HttpStatus.UNAUTHORIZED.value());
//        ctx.setSendZuulResponse(false);

        return null;

    }
}
