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
package com.oracle.graal.pointsto.infrastructure;

import java.lang.reflect.Executable;

import org.graalvm.compiler.api.replacements.SnippetReflectionProvider;

import com.oracle.graal.pointsto.util.AnalysisError;

import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;

public interface OriginalMethodProvider {

    static Executable getJavaMethod(SnippetReflectionProvider reflectionProvider, ResolvedJavaMethod method) {
        if (method instanceof OriginalMethodProvider) {
            return ((OriginalMethodProvider) method).getJavaMethod();
        }
        try {
            return getJavaMethodInternal(reflectionProvider, method);
        } catch (NoSuchMethodException e) {
            throw AnalysisError.shouldNotReachHere();
        }
    }

    static boolean hasJavaMethod(SnippetReflectionProvider reflectionProvider, ResolvedJavaMethod method) {
        if (method instanceof OriginalMethodProvider) {
            return ((OriginalMethodProvider) method).hasJavaMethod();
        }
        try {
            getJavaMethodInternal(reflectionProvider, method);
            return true;
        } catch (NoSuchMethodException | LinkageError | NullPointerException e) {
            return false;
        }
    }

    static Executable getJavaMethodInternal(SnippetReflectionProvider reflectionProvider, ResolvedJavaMethod method) throws NoSuchMethodException {
        ResolvedJavaMethod.Parameter[] parameters = method.getParameters();
        Class<?>[] parameterTypes = new Class<?>[parameters.length];
        ResolvedJavaType declaringClassType = method.getDeclaringClass();
        for (int i = 0; i < parameterTypes.length; i++) {
            parameterTypes[i] = OriginalClassProvider.getJavaClass(reflectionProvider, parameters[i].getType().resolve(declaringClassType));
        }
        Class<?> declaringClass = OriginalClassProvider.getJavaClass(reflectionProvider, declaringClassType);
        if (method.isConstructor()) {
            return declaringClass.getDeclaredConstructor(parameterTypes);
        } else {
            return declaringClass.getDeclaredMethod(method.getName(), parameterTypes);
        }
    }

    Executable getJavaMethod();

    boolean hasJavaMethod();
}
