package com.mopl.mopl.global.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Aspect
@Slf4j
@Component
public class LoggingAspect {
    @Around("execution(* com.mopl.mopl.domain..controller..*(..))")
    public Object logController(ProceedingJoinPoint joinPoint) throws Throwable {
        String method = joinPoint.getSignature().toShortString();

        log.debug("[REQUEST] {}", method);

        try {
            Object result = joinPoint.proceed();

            if (result instanceof ResponseEntity<?> response) {
                log.debug("[RESPONSE] {} status={}", method, response.getStatusCode());
            } else {
                log.debug("[RESPONSE] {}", method);
            }

            return result;
        } catch (Exception e) {
            log.warn("[CONTROLLER FAIL] {} - {}", method, e.getMessage());
            throw e;
        }
    }

    @Around("execution(* com.mopl.mopl.domain..service..*(..))")
    public Object logService(ProceedingJoinPoint joinPoint) throws Throwable {
        String method = joinPoint.getSignature().toShortString();
        long start = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - start;

            log.debug("[SERVICE] {} - {}ms", method, elapsed);
            return result;
        } catch (Exception e) {
            log.warn("[SERVICE FAIL] {} - {}", method, e.getMessage());
            throw e;
        }
    }
}
