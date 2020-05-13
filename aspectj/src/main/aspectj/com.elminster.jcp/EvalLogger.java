package com.elminster.jcp;

import com.elminster.jcp.eval.context.EvalContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Aspect
public class EvalLogger {

  private static final Logger logger = LoggerFactory.getLogger(EvalLogger.class);

  @Pointcut("execution(* com.elminster.workflow.core.eval.Evaluable.eval(*))")
  public void execBlock() {
  }

  @Around("execBlock()")
  public Object traceEval(ProceedingJoinPoint joinPoint) throws Throwable {
    Object target = joinPoint.getTarget();
    Object[] args = joinPoint.getArgs();
    assert args[0] instanceof EvalContext;
    if (logger.isTraceEnabled()) {
      logger.trace("START eval [{}] with context [{}]", target, args[0]);
    }
    Object flowData = joinPoint.proceed(args);
    if (logger.isTraceEnabled()) {
      logger.trace("END eval [{}] with context [{}], result: [{}].", target, args[0], flowData);
    }
    return flowData;
  }
}