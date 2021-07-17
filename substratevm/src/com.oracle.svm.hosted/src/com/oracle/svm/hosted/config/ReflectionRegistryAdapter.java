/*
 * Copyright (c) 2019, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.svm.hosted.config;

import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.List;

import org.graalvm.nativeimage.impl.ConfigurationPredicate;
import org.graalvm.nativeimage.impl.ReflectionRegistry;

import com.oracle.svm.core.TypeResult;
import com.oracle.svm.core.configure.PredicatedElement;
import com.oracle.svm.core.configure.ReflectionConfigurationParserDelegate;
import com.oracle.svm.hosted.ImageClassLoader;
import com.oracle.svm.util.ClassUtil;

import jdk.vm.ci.meta.MetaUtil;

public class ReflectionRegistryAdapter implements ReflectionConfigurationParserDelegate<PredicatedElement<Class<?>>> {
    private final ReflectionRegistry registry;
    private final ImageClassLoader classLoader;

    public ReflectionRegistryAdapter(ReflectionRegistry registry, ImageClassLoader classLoader) {
        this.registry = registry;
        this.classLoader = classLoader;
    }

    @Override
    public void registerType(PredicatedElement<Class<?>> type) {
        registry.register(type.getPredicate(), type.getElement());
    }

    @Override
    public TypeResult<ConfigurationPredicate> resolvePredicate(String typeName) {
        String canonicalizedName = canonicalizeTypeName(typeName);
        TypeResult<Class<?>> clazz = classLoader.findClass(canonicalizedName);
        return clazz.map(Class::getTypeName)
                        .map(ConfigurationPredicate::create);
    }

    @Override
    public TypeResult<PredicatedElement<Class<?>>> resolveType(ConfigurationPredicate predicate, String typeName) {
        String name = canonicalizeTypeName(typeName);
        TypeResult<Class<?>> clazz = classLoader.findClass(name);
        return clazz.map(c -> new PredicatedElement<>(predicate, c));
    }

    private static String canonicalizeTypeName(String typeName) {
        String name = typeName;
        if (name.indexOf('[') != -1) {
            /* accept "int[][]", "java.lang.String[]" */
            name = MetaUtil.internalNameToJava(MetaUtil.toInternalName(name), true, true);
        }
        return name;
    }

    @Override
    public void registerPublicClasses(PredicatedElement<Class<?>> type) {
        registry.register(type.getPredicate(), type.getElement().getClasses());
    }

    @Override
    public void registerDeclaredClasses(PredicatedElement<Class<?>> type) {
        registry.register(type.getPredicate(), type.getElement().getDeclaredClasses());
    }

    @Override
    public void registerPublicFields(PredicatedElement<Class<?>> type) {
        registry.register(type.getPredicate(), false, type.getElement().getFields());
    }

    @Override
    public void registerDeclaredFields(PredicatedElement<Class<?>> type) {
        registry.register(type.getPredicate(), false, type.getElement().getDeclaredFields());
    }

    @Override
    public void registerPublicMethods(PredicatedElement<Class<?>> type) {
        registry.register(type.getPredicate(), type.getElement().getMethods());
    }

    @Override
    public void registerDeclaredMethods(PredicatedElement<Class<?>> type) {
        registry.register(type.getPredicate(), type.getElement().getDeclaredMethods());
    }

    @Override
    public void registerPublicConstructors(PredicatedElement<Class<?>> type) {
        registry.register(type.getPredicate(), type.getElement().getConstructors());
    }

    @Override
    public void registerDeclaredConstructors(PredicatedElement<Class<?>> type) {
        registry.register(type.getPredicate(), type.getElement().getDeclaredConstructors());
    }

    @Override
    public void registerField(PredicatedElement<Class<?>> type, String fieldName, boolean allowWrite) throws NoSuchFieldException {
        registry.register(type.getPredicate(), allowWrite, type.getElement().getDeclaredField(fieldName));
    }

    @Override
    public boolean registerAllMethodsWithName(PredicatedElement<Class<?>> type, String methodName) {
        boolean found = false;
        Executable[] methods = type.getElement().getDeclaredMethods();
        for (Executable method : methods) {
            if (method.getName().equals(methodName)) {
                registry.register(type.getPredicate(), method);
                found = true;
            }
        }
        return found;
    }

    @Override
    public boolean registerAllConstructors(PredicatedElement<Class<?>> type) {
        Executable[] methods = type.getElement().getDeclaredConstructors();
        for (Executable method : methods) {
            registry.register(type.getPredicate(), method);
        }
        return methods.length > 0;
    }

    @Override
    public void registerMethod(PredicatedElement<Class<?>> type, String methodName, List<PredicatedElement<Class<?>>> methodParameterTypes) throws NoSuchMethodException {
        Class<?>[] parameterTypesArray = getParameterTypes(methodParameterTypes);
        Method method;
        try {
            method = type.getElement().getDeclaredMethod(methodName, parameterTypesArray);
        } catch (NoClassDefFoundError e) {
            /*
             * getDeclaredMethod() builds a set of all the declared methods, which can fail when a
             * symbolic reference from another method to a type (via parameters, return value)
             * cannot be resolved. getMethod() builds a different set of methods and can still
             * succeed. This case must be handled for predefined classes when, during the run
             * observed by the agent, a referenced class was not loaded and is not available now
             * precisely because the application used getMethod() instead of getDeclaredMethod().
             */
            try {
                method = type.getElement().getMethod(methodName, parameterTypesArray);
            } catch (Throwable ignored) {
                throw e;
            }
        }
        registry.register(type.getPredicate(), method);
    }

    @Override
    public void registerConstructor(PredicatedElement<Class<?>> type, List<PredicatedElement<Class<?>>> methodParameterTypes) throws NoSuchMethodException {
        Class<?>[] parameterTypesArray = getParameterTypes(methodParameterTypes);
        registry.register(type.getPredicate(), type.getElement().getDeclaredConstructor(parameterTypesArray));
    }

    private static Class<?>[] getParameterTypes(List<PredicatedElement<Class<?>>> methodParameterTypes) {
        return methodParameterTypes.stream()
                        .map(PredicatedElement::getElement)
                        .toArray(Class<?>[]::new);
    }

    @Override
    public String getTypeName(PredicatedElement<Class<?>> type) {
        return type.getElement().getTypeName();
    }

    @Override
    public String getSimpleName(PredicatedElement<Class<?>> type) {
        return ClassUtil.getUnqualifiedName(type.getElement());
    }
}
