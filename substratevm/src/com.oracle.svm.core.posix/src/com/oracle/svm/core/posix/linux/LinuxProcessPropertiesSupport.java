/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.svm.core.posix.linux;

import org.graalvm.nativeimage.ImageSingletons;
import org.graalvm.nativeimage.StackValue;
import org.graalvm.nativeimage.c.function.CFunction;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CLongPointer;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.impl.ProcessPropertiesSupport;
import org.graalvm.word.PointerBase;

import com.oracle.svm.core.annotate.AutomaticFeature;
import com.oracle.svm.core.c.CGlobalData;
import com.oracle.svm.core.c.CGlobalDataFactory;
import com.oracle.svm.core.posix.PosixProcessPropertiesSupport;
import com.oracle.svm.core.posix.PosixVirtualMemoryProvider;

public class LinuxProcessPropertiesSupport extends PosixProcessPropertiesSupport {

    @Override
    public String getExecutableName() {
        final String exefileString = "/proc/self/exe";
        return realpath(exefileString);
    }

    interface FILE extends PointerBase {
    }

    @CFunction(value = "fopen", transition = CFunction.Transition.NO_TRANSITION)
    private static native FILE fopen(CCharPointer path, CCharPointer mode);

    @CFunction(value = "fclose", transition = CFunction.Transition.NO_TRANSITION)
    private static native int fclose(FILE f);

    @CFunction(value = "fscanf", transition = CFunction.Transition.NO_TRANSITION)
    private static native int fscanfL(FILE stream, CCharPointer format, CLongPointer lp);

    private static final CGlobalData<CCharPointer> GET_RSS_PROC_SELF_STAT = CGlobalDataFactory.createCString("/proc/self/stat");
    private static final CGlobalData<CCharPointer> GET_RSS_FOPEN_MODE = CGlobalDataFactory.createCString("r");
    private static final CGlobalData<CCharPointer> GET_RSS_SCANF_FORMAT = CGlobalDataFactory
                    .createCString("%*d %*s %*c %*d %*d %*d %*d %*d %*u %*lu %*lu %*lu %*lu %*lu %*lu %*ld %*ld %*ld %*ld %*ld %*ld %*llu %*lu %ld");

    @Override
    public long getResidentSetSize() {
        CLongPointer lp = StackValue.get(CLongPointer.class);
        FILE f = fopen(GET_RSS_PROC_SELF_STAT.get(), GET_RSS_FOPEN_MODE.get());
        if (f.isNull() || fscanfL(f, GET_RSS_SCANF_FORMAT.get(), lp) != 1) {
            return -1;
        }
        fclose(f);
        long npages = lp.read();
        return npages * PosixVirtualMemoryProvider.getPageSize().rawValue();
    }

    @AutomaticFeature
    public static class ImagePropertiesFeature implements Feature {

        @Override
        public void afterRegistration(AfterRegistrationAccess access) {
            ImageSingletons.add(ProcessPropertiesSupport.class, new LinuxProcessPropertiesSupport());
        }
    }

}
