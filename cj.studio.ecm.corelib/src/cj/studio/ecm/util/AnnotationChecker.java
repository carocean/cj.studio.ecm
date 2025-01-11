package cj.studio.ecm.util;

import java.lang.annotation.Annotation;

public class AnnotationChecker {
    /**
     * 递归检查类及其父类是否具有指定的注解类型。
     * @param clazz 要检查的类。
     * @param annotationClass 注解的类型。
     * @return 如果类或其父类包含指定注解，返回 true；否则返回 false。
     */
    public static boolean hasAnnotation(Class<?> clazz, Class<? extends Annotation> annotationClass) {
        // 如果类本身具有注解
        if (clazz.isAnnotationPresent(annotationClass)) {
            return true;
        }

        // 检查父类
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && hasAnnotation(superClass, annotationClass)) {
            return true;
        }

        // 检查接口
        for (Class<?> iface : clazz.getInterfaces()) {
            if (hasAnnotation(iface, annotationClass)) {
                return true;
            }
        }

        // 如果都没有，返回 false
        return false;
    }

}