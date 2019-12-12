package com.tvd12.ezyhttp.server.core;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.tvd12.ezyfox.bean.EzyBeanContext;
import com.tvd12.ezyfox.builder.EzyBuilder;
import com.tvd12.ezyfox.reflect.EzyReflection;
import com.tvd12.ezyfox.reflect.EzyReflectionProxy;
import com.tvd12.ezyhttp.core.annotation.Interceptor;
import com.tvd12.ezyhttp.server.core.annotation.ApplicationBootstrap;
import com.tvd12.ezyhttp.server.core.annotation.ComponentClasses;
import com.tvd12.ezyhttp.server.core.annotation.ComponentsScan;
import com.tvd12.ezyhttp.server.core.annotation.Controller;
import com.tvd12.ezyhttp.server.core.annotation.ExceptionHandler;
import com.tvd12.ezyhttp.server.core.asm.ExceptionHandlersImplementer;
import com.tvd12.ezyhttp.server.core.asm.RequestHandlersImplementer;
import com.tvd12.ezyhttp.server.core.handler.RequestHandler;
import com.tvd12.ezyhttp.server.core.handler.UncaughtExceptionHandler;
import com.tvd12.ezyhttp.server.core.interceptor.RequestInterceptor;
import com.tvd12.ezyhttp.server.core.manager.ComponentManager;
import com.tvd12.ezyhttp.server.core.manager.ControllerManager;
import com.tvd12.ezyhttp.server.core.manager.ExceptionHandlerManager;
import com.tvd12.ezyhttp.server.core.manager.InterceptorManager;
import com.tvd12.ezyhttp.server.core.manager.RequestHandlerManager;
import com.tvd12.ezyhttp.server.core.request.RequestURI;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class ApplicationContextBuilder implements EzyBuilder<ApplicationContext> {

	protected final Set<String> packageToScans;
	protected final Set<Class> componentClasses;
	protected final ComponentManager componentManager;
	protected final ControllerManager controllerManager;
	protected final InterceptorManager interceptorManager;
	protected final RequestHandlerManager requestHandlerManager;
	protected final ExceptionHandlerManager exceptionHandlerManager;
	
	public ApplicationContextBuilder() {
		this.packageToScans = new HashSet<>();
		this.componentClasses = new HashSet<>();
		this.componentManager = ComponentManager.getInstance();
		this.controllerManager = componentManager.getControllerManager();
		this.interceptorManager = componentManager.getInterceptorManager();
		this.requestHandlerManager = componentManager.getRequestHandlerManager();
		this.exceptionHandlerManager = componentManager.getExceptionHandlerManager();
	}
	
	public ApplicationContextBuilder scan(String packageName) {
		this.packageToScans.add(packageName);
		return this;
	}
	
	public ApplicationContextBuilder scan(String... packageNames) {
		for(String packageName : packageNames)
			scan(packageName);
		return this;
	}
	
	public ApplicationContextBuilder scan(Iterable<String> packageNames) {
		for(String packageName : packageNames)
			scan(packageName);
		return this;
	}
	
	public ApplicationContextBuilder addComponentClass(Class<?> componentClass) {
		ComponentsScan componentsScan = componentClass.getAnnotation(ComponentsScan.class);
		if(componentsScan != null)
			scan(componentsScan.value());
		ComponentClasses componentClasses = componentClass.getAnnotation(ComponentClasses.class);
		if(componentClasses != null)
			addComponentClasses(componentClasses.value());
		this.componentClasses.add(componentClass);
		return this;
	}
	
	public ApplicationContextBuilder addComponentClasses(Class<?>... componentClasses) {
		for(Class<?> clazz : componentClasses)
			addComponentClass(clazz);
		return this;
	}
	
	public ApplicationContextBuilder addComponentClasses(Iterable<Class<?>> componentClasses) {
		for(Class<?> clazz : componentClasses)
			addComponentClass(clazz);
		return this;
	}
	
	@Override
	public ApplicationContext build() {
		EzyReflection reflection = new EzyReflectionProxy(packageToScans);
		EzyBeanContext beanContext = createBeanContext(reflection);
		SimpleApplicationContext context = new SimpleApplicationContext();
		context.setBeanContext(beanContext);
		return context;
	}
	
	protected EzyBeanContext createBeanContext(EzyReflection reflection) {
		Set controllerClasses = reflection.getAnnotatedClasses(Controller.class);
		Set interceptorClases = reflection.getAnnotatedClasses(Interceptor.class);
		Set exceptionHandlerClasses = reflection.getAnnotatedClasses(ExceptionHandler.class);
		Set bootstrapClasses = reflection.getAnnotatedClasses(ApplicationBootstrap.class);
		EzyBeanContext beanContext = EzyBeanContext.builder()
				.addAllClasses(reflection)
				.addSingletonClasses(componentClasses)
				.addSingletonClasses(controllerClasses)
				.addSingletonClasses(interceptorClases)
				.addSingletonClasses(exceptionHandlerClasses)
				.addSingletonClasses(bootstrapClasses)
				.build();
		registerComponents(beanContext);
		addRequestHandlers();
		addExceptionHandlers();
		return beanContext;
	}
	
	protected void registerComponents(EzyBeanContext beanContext) {
		List controllers = beanContext.getSingletons(Controller.class);
		controllerManager.addControllers(controllers);
		List exceptionHandlers = beanContext.getSingletons(ExceptionHandler.class);
		exceptionHandlerManager.addExceptionHandlers(exceptionHandlers);
		List<RequestInterceptor> requestInterceptors = beanContext.getSingletons(Interceptor.class);
		interceptorManager.addRequestInterceptors(requestInterceptors);
	}
	
	protected void addRequestHandlers() {
		List<Object> controllerList = controllerManager.getControllerList();
		RequestHandlersImplementer implementer = newRequestHandlersImplementer();
		Map<RequestURI, RequestHandler> requestHandlers = implementer.implement(controllerList);
		requestHandlerManager.addHandlers(requestHandlers);
	}
	
	protected void addExceptionHandlers() {
		List<Object> exceptionHandlerList = exceptionHandlerManager.getExceptionHandlerList();
		ExceptionHandlersImplementer implementer = newExceptionHandlersImplementer();
		Map<Class<?>, UncaughtExceptionHandler> exceptionHandlers = implementer.implement(exceptionHandlerList);
		exceptionHandlerManager.addUncaughtExceptionHandlers(exceptionHandlers);
	}
	
	protected RequestHandlersImplementer newRequestHandlersImplementer() {
		return new RequestHandlersImplementer();
	}
	
	protected ExceptionHandlersImplementer newExceptionHandlersImplementer() {
		return new ExceptionHandlersImplementer();
	}
}
