package com.netease.nim.camellia.console.util;

import com.netease.nim.camellia.console.annotation.ActionSecurity;
import com.netease.nim.camellia.console.constant.ActionType;
import com.netease.nim.camellia.console.constant.AppCode;
import com.netease.nim.camellia.console.context.AppInfoContext;
import com.netease.nim.camellia.console.model.BaseUser;
import com.netease.nim.camellia.console.model.WebResult;
import com.netease.nim.camellia.console.service.UserAccessService;
import com.netease.nim.camellia.console.service.ao.IdentityDashboardBaseAO;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
@Component
@Aspect
public class AccessAOP {


    @Autowired
    UserAccessService userAccessService;

    @Around("execution (* com.netease.nim.camellia.console.controller.*.*(..))")
    public Object testAop(ProceedingJoinPoint pro) throws Throwable {
        //获取request请求提(需要时备用)
//        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        //进入方法前执行的代码
        Signature signature = pro.getSignature();
        if(!(signature instanceof MethodSignature)){
            return WebResult.internalFail("internal wrong: 只能作用于方法");
        }

        MethodSignature methodSignature=(MethodSignature) signature;
        Method currentMethod=pro.getTarget().getClass().getMethod(methodSignature.getName(),methodSignature.getParameterTypes());
        ActionSecurity annotation = currentMethod.getAnnotation(ActionSecurity.class);
        if(annotation!=null) {
            BaseUser user = AppInfoContext.getUser();
            LogBean.get().addProps("BaseUser", user);
//            LogBean.get().addProps("ActionSecurity action:", annotation.action());
//            LogBean.get().addProps("ActionSecurity resource:", annotation.resource());
//            LogBean.get().addProps("ActionSecurity role:", annotation.role());
            IdentityDashboardBaseAO baseAO=null;
            if(annotation.action().equals(ActionType.WRITE)) {
                Object[] args = pro.getArgs();
                for (Object o : args) {
                    if (o instanceof IdentityDashboardBaseAO) {
                        baseAO = (IdentityDashboardBaseAO) o;
                        AppInfoContext.setIdentityDashboardBaseAO(baseAO);
                        break;
                    }
                }
            }
            if(AppInfoContext.getIdentityDashboardBaseAO()!=null)
                LogBean.get().addProps("did",AppInfoContext.getIdentityDashboardBaseAO().getDid());
            if(userAccessService.authorityJudge(annotation,user,AppInfoContext.getIdentityDashboardBaseAO())==false){
                return WebResult.fail(AppCode.FORBIDDEN,user.getUsername()+" HAS NO RIGHT FOR THIS ACTION");
            }
        }
        //执行调用的方法
        Object proceed = pro.proceed();
        //方法执行完成后执行的方法
        return proceed;
    }

}
