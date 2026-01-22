package io.camunda.zeebe.spring.client.bean;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;

import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MethodInfo implements BeanInfo {

  // Spring 5/6 compatible; avoids removed LocalVariableTableParameterNameDiscoverer
  private static final ParameterNameDiscoverer parameterNameDiscoverer =
    new DefaultParameterNameDiscoverer();

  private ClassInfo classInfo;
  private Method method;

  private MethodInfo(ClassInfo classInfo, Method method) {
    this.classInfo = classInfo;
    this.method = method;
  }

  @Override
  public Object getBean() {
    return classInfo.getBean();
  }

  @Override
  public String getBeanName() {
    return classInfo.getBeanName();
  }

  public String getMethodName() {
    return method.getName();
  }

  public Object invoke(final Object... args) {
    try {
      return method.invoke(getBean(), args);
    } catch (InvocationTargetException e) {
      final Throwable targetException = e.getTargetException();
      if (targetException instanceof RuntimeException) {
        throw (RuntimeException) targetException;
      }
      else {
        throw new RuntimeException("Failed to invoke method: " + method.getName(), targetException);
      }
    } catch (IllegalAccessException e) {
      throw new RuntimeException("Failed to invoke method: " + method.getName(), e);
    }
  }

  public <T extends Annotation> Optional<T> getAnnotation(final Class<T> type) {
    return Optional.ofNullable(findAnnotation(method, type));
  }

  public List<ParameterInfo> getParameters() {
    Parameter[] parameters = method.getParameters();
    String[] discoveredNames = parameterNameDiscoverer.getParameterNames(method);

    ArrayList<ParameterInfo> result = new ArrayList<>();
    for (int i = 0; i < parameters.length; i++) {
      final String name =
        (discoveredNames != null && i < discoveredNames.length && discoveredNames[i] != null)
          ? discoveredNames[i]
          : parameters[i].getName(); // fallback (requires javac -parameters for real names)

      result.add(new ParameterInfo(parameters[i], name));
    }
    return result;
  }

  public List<ParameterInfo> getParametersFilteredByAnnotation(final Class type) {
    Parameter[] parameters = method.getParameters();
    String[] discoveredNames = parameterNameDiscoverer.getParameterNames(method);

    ArrayList<ParameterInfo> result = new ArrayList<>();
    for (int i = 0; i < parameters.length; i++) {
      if (parameters[i].isAnnotationPresent(type)) {
        final String name =
          (discoveredNames != null && i < discoveredNames.length && discoveredNames[i] != null)
            ? discoveredNames[i]
            : parameters[i].getName();

        result.add(new ParameterInfo(parameters[i], name));
      }
    }
    return result;
  }

  public static MethodInfoBuilder builder() {
    return new MethodInfoBuilder();
  }

  public static final class MethodInfoBuilder {

    private ClassInfo classInfo;
    private Method method;

    private MethodInfoBuilder() {
    }

    public MethodInfoBuilder classInfo(ClassInfo classInfo) {
      this.classInfo = classInfo;
      return this;
    }

    public MethodInfoBuilder method(Method method) {
      this.method = method;
      return this;
    }

    public MethodInfo build() {
      return new MethodInfo(classInfo, method);
    }
  }
}
