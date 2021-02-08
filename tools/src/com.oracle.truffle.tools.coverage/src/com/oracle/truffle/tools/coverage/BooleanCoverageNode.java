/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.tools.coverage;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.SourceSection;

final class BooleanCoverageNode extends AbstractCoverageNode {

    private final Assumption noReset;
    @CompilerDirectives.CompilationFinal volatile boolean coveredFinal;
    volatile boolean covered;

    BooleanCoverageNode(SourceSection sourceSection, Node instrumentedNode, boolean isRoot, boolean isStatement, Assumption noReset) {
        super(sourceSection, instrumentedNode, isRoot, isStatement);
        this.noReset = noReset;
    }

    @Override
    boolean isCovered() {
        if (noReset.isValid()) {
            return coveredFinal;
        } else {
            return covered;
        }
    }

    @Override
    void reset() {
        covered = false;
    }

    @Override
    protected void onEnter(VirtualFrame frame) {
        if (noReset.isValid()) {
            if (!coveredFinal) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                coveredFinal = true;
                covered = true;
            }
        } else {
            covered = true;
        }
    }

}
