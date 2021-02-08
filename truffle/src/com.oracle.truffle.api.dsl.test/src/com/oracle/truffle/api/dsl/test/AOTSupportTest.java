/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.truffle.api.dsl.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.TruffleLanguage.Env;
import com.oracle.truffle.api.TruffleLanguage.Registration;
import com.oracle.truffle.api.dsl.AOTSupport;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CachedLanguage;
import com.oracle.truffle.api.dsl.GenerateAOT;
import com.oracle.truffle.api.dsl.ImplicitCast;
import com.oracle.truffle.api.dsl.Introspectable;
import com.oracle.truffle.api.dsl.Introspection;
import com.oracle.truffle.api.dsl.Introspection.SpecializationInfo;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystem;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.dsl.test.AOTSupportTestFactory.NoSpecializationTestNodeGen;
import com.oracle.truffle.api.dsl.test.AOTSupportTestFactory.TestNodeGen;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.ExecutionSignature;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ByteValueProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.DoubleValueProfile;
import com.oracle.truffle.api.profiles.FloatValueProfile;
import com.oracle.truffle.api.profiles.IntValueProfile;
import com.oracle.truffle.api.profiles.LongValueProfile;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.api.profiles.PrimitiveValueProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.api.test.polyglot.AbstractPolyglotTest;

/**
 * Note that this test is also used in AOTSupportCompilationTest.
 */
public class AOTSupportTest extends AbstractPolyglotTest {

    public static final String LANGUAGE_ID = "AOTSupportTest_TestLanguage";

    public AOTSupportTest() {
        enterContext = false;
    }

    @Test
    public void testNoSpecializations() {
        assertEquals(42, NoSpecializationTestNodeGen.create().execute(42));
    }

    @GenerateAOT
    @Introspectable
    abstract static class NoSpecializationTestNode extends Node {

        abstract Object execute(Object arg);

        @Specialization
        Object doDefault(Object arg) {
            return arg;
        }

    }

    public static class TestRootNode extends RootNode {

        @Child TestNode node = TestNodeGen.create(false);

        final TestLanguage language;

        public TestRootNode(TestLanguage language) {
            super(language);
            this.language = language;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            int sum = 0;
            for (int i = 0; i < TestNode.AOT_SPECIALIZATIONS; i++) {
                sum += node.execute(i);
            }
            return sum;
        }

        @Override
        protected ExecutionSignature prepareForAOT() {
            AOTSupport.prepareForAOT(this);
            return ExecutionSignature.create(Integer.class, new Class<?>[0]);
        }

    }

    @Test
    public void testNoLock() {
        TestRootNode root = setup();
        assertFails(() -> ((GenerateAOT.Provider) root.node).prepareForAOT(root.language, root), AssertionError.class, (e) -> {
            assertTrue(e.getMessage(), e.getMessage().contains("AST lock"));
        });
    }

    @Test
    public void testNode() {
        TestRootNode root = setup();
        AOTSupport.prepareForAOT(root);
        assertEquals(TestNode.AOT_SPECIALIZATIONS, countActiveSpecializations(root));

        context.enter();
        for (int i = 0; i < TestNode.AOT_SPECIALIZATIONS; i++) {
            root.node.execute(i);
            assertEquals(i + 1, countActiveSpecializations(root));
        }
        context.leave();
    }

    private static int countActiveSpecializations(TestRootNode root) {
        return (int) Introspection.getSpecializations(root.node).stream().filter(SpecializationInfo::isActive).count();
    }

    private TestRootNode setup() {
        setupEnv();
        context.initialize(LANGUAGE_ID);
        context.enter();
        TestRootNode root = new TestRootNode(TestLanguage.getCurrentLanguage());
        Truffle.getRuntime().createCallTarget(root);
        context.leave();
        return root;
    }

    @TypeSystem
    public static class AOTTypeSystem {

        @ImplicitCast
        public static long castInt(int v) {
            return v;
        }

    }

    @GenerateAOT
    @Introspectable
    @TypeSystemReference(AOTTypeSystem.class)
    @SuppressWarnings("unused")
    public abstract static class TestNode extends Node {

        abstract int execute(Object arg);

        final boolean recursive;

        TestNode(boolean recursive) {
            this.recursive = recursive;
        }

        @Specialization(guards = "arg == 0")
        int basic(int arg) {
            return arg;
        }

        @Specialization(guards = "arg == 1")
        int basicCached(int arg, @Cached("1") int one,
                        @Cached("2") int two,
                        @Cached("3") int three,
                        @Cached("4") int four) {
            return arg;
        }

        @Specialization(guards = "arg == 2")
        int nodeCachedSingle(int arg,
                        @Cached NoSpecializationTestNode node) {
            return arg;
        }

        @Specialization(guards = "arg == 3")
        int nodeCachedMulti(int arg, @Cached("1") int one,
                        @Cached("2") int two,
                        @Cached("3") int three,
                        @Cached("4") int four,
                        @Cached NoSpecializationTestNode node) {
            return arg;
        }

        @Specialization(guards = "arg == 4")
        int languageReferenceLookup1(int arg,
                        @CachedLanguage TestLanguage lang1) {
            return arg;
        }

        @Specialization(guards = "arg == 5", limit = "1")
        int languageReferenceLookup2(int arg,
                        @CachedLanguage TestLanguage lang1,
                        @CachedLibrary("lang1.getValue()") InteropLibrary lib) {
            return arg;
        }

        @Specialization(guards = "arg == 6", assumptions = "createAssumption()")
        int assumptionUsage(int arg) {
            return arg;
        }

        static Assumption createAssumption() {
            return Truffle.getRuntime().createAssumption();
        }

        @Specialization(guards = {"arg == 7", "cachedArg == 6"})
        int implicitCast(long arg, @Cached("6") int cachedArg) {
            return (int) arg;
        }

        @Specialization(guards = {"arg == 8", "arg == cachedArg"})
        int ignoredCache(int arg, @Cached("arg") int cachedArg) {
            return arg;
        }

        @Specialization(guards = "arg == 8", replaces = "ignoredCache")
        int genericCache(int arg) {
            return arg;
        }

        @Specialization(guards = {"arg == 9", "!recursive"})
        int recursiveCache(int arg, @Cached("create(true)") TestNode recursiveNode) {
            return recursiveNode.execute(arg);
        }

        @Specialization(guards = {"arg == 9", "recursive"})
        int noRecursiveCache(int arg) {
            return arg;
        }

        @Specialization(guards = {"arg == 10"})
        int profiles(int arg, @Cached BranchProfile branch,
                        @Cached("createBinaryProfile()") ConditionProfile binaryCondition,
                        @Cached("createCountingProfile()") ConditionProfile countingCondition,
                        @Cached("createCountingProfile()") LoopConditionProfile loopCondition,
                        @Cached("createIdentityProfile()") ByteValueProfile byteValue,
                        @Cached("createIdentityProfile()") IntValueProfile intValue,
                        @Cached("createIdentityProfile()") LongValueProfile longValue,
                        @Cached("createRawIdentityProfile()") FloatValueProfile floatValue,
                        @Cached("createRawIdentityProfile()") DoubleValueProfile doubleValue,
                        @Cached("createEqualityProfile()") PrimitiveValueProfile primitiveValue,
                        @Cached("createClassProfile()") ValueProfile classValue,
                        @Cached("createIdentityProfile()") ValueProfile identityValue,
                        @Cached("createEqualityProfile()") ValueProfile equalityValue) {

            branch.enter();
            binaryCondition.profile(true);
            binaryCondition.profile(false);
            countingCondition.profile(true);
            countingCondition.profile(false);
            loopCondition.profile(true);
            loopCondition.profile(false);

            byteValue.profile((byte) 1);
            byteValue.profile((byte) 2);
            intValue.profile(1);
            intValue.profile(2);
            longValue.profile(1);
            longValue.profile(2);
            floatValue.profile(1);
            floatValue.profile(2);
            doubleValue.profile(1);
            doubleValue.profile(2);

            primitiveValue.profile(true);
            primitiveValue.profile(false);
            primitiveValue.profile((byte) 1);
            primitiveValue.profile((byte) 2);
            primitiveValue.profile((short) 1);
            primitiveValue.profile((short) 2);
            primitiveValue.profile((char) 1);
            primitiveValue.profile((char) 2);
            primitiveValue.profile(1);
            primitiveValue.profile(2);
            primitiveValue.profile(1L);
            primitiveValue.profile(2L);
            primitiveValue.profile(1f);
            primitiveValue.profile(2f);
            primitiveValue.profile(1d);
            primitiveValue.profile(2d);
            primitiveValue.profile(Integer.valueOf(1));
            primitiveValue.profile(Integer.valueOf(2));

            classValue.profile(Integer.class);
            identityValue.profile(this);
            equalityValue.profile(this);

            return arg;
        }

        public static final int AOT_SPECIALIZATIONS = 11;

    }

    @Registration(id = LANGUAGE_ID, name = LANGUAGE_ID)
    public static class TestLanguage extends TruffleLanguage<Env> {

        Object value = 42;

        @Override
        protected Env createContext(Env env) {
            return env;
        }

        public Object getValue() {
            return value;
        }

        public static TestLanguage getCurrentLanguage() {
            return getCurrentLanguage(TestLanguage.class);
        }

    }

    @GenerateAOT
    abstract static class ErrorDynamicParameterBoundCache extends Node {

        abstract Object execute(Object arg);

        @Specialization
        @SuppressWarnings("unused")
        int basicCached(int arg,
                        @ExpectError("Failed to generate code for @GenerateAOT: Cached values in specializations included for AOT must not bind dynamic values. " + //
                                        "Such caches are only allowed to bind static values, values read from the node or values from the current language instance using a language reference. Resolve this problem by either: %n" + //
                                        " - Exclude this specialization from AOT with @GenerateAOT.Exclude if it is acceptable to deoptimize for this specialization in AOT compiled code. %n" + //
                                        " - Configure the specialization to be replaced with a more generic specialization. %n" + //
                                        " - Remove the cached parameter value. %n" +
                                        " - Avoid binding dynamic parameters in the cache initializer expression.")//
                        @Cached("arg") int cachedArg) {
            return arg;
        }
    }

    @GenerateAOT
    abstract static class ErrorDynamicLibraryNoGuardBound extends Node {

        abstract Object execute(Object arg);

        @Specialization(limit = "1")
        @SuppressWarnings("unused")
        int basicCached(Object arg,
                        @ExpectError("Failed to generate code for @GenerateAOT: Cached values in specializations included for AOT must not bind dynamic values. " + //
                                        "Such caches are only allowed to bind static values, values read from the node or values from the current language instance using a language reference. Resolve this problem by either: %n" + //
                                        " - Exclude this specialization from AOT with @GenerateAOT.Exclude if it is acceptable to deoptimize for this specialization in AOT compiled code. %n" + //
                                        " - Configure the specialization to be replaced with a more generic specialization. %n" + //
                                        " - Remove the cached parameter value. %n" +
                                        " - Avoid binding dynamic parameters in the cache initializer expression.")//
                        @CachedLibrary("arg") InteropLibrary lib) {
            try {
                return lib.asInt(arg);
            } catch (UnsupportedMessageException e) {
                throw CompilerDirectives.shouldNotReachHere(e);
            }
        }
    }

    @GenerateAOT
    abstract static class ErrorDynamicLibraryWithGuardBound extends Node {

        abstract Object execute(Object arg);

        @Specialization(guards = "lib.fitsInInt(arg)", limit = "1")
        @SuppressWarnings("unused")
        int basicCached(Object arg,
                        @ExpectError("Failed to generate code for @GenerateAOT: Cached values in specializations included for AOT must not bind dynamic values. " + //
                                        "Such caches are only allowed to bind static values, values read from the node or values from the current language instance using a language reference. Resolve this problem by either: %n" + //
                                        " - Exclude this specialization from AOT with @GenerateAOT.Exclude if it is acceptable to deoptimize for this specialization in AOT compiled code. %n" + //
                                        " - Configure the specialization to be replaced with a more generic specialization. %n" + //
                                        " - Remove the cached parameter value. %n" +
                                        " - Avoid binding dynamic parameters in the cache initializer expression.")//
                        @CachedLibrary("arg") InteropLibrary lib) {
            try {
                return lib.asInt(arg);
            } catch (UnsupportedMessageException e) {
                throw CompilerDirectives.shouldNotReachHere(e);
            }
        }
    }

    @GenerateAOT
    abstract static class ErrorDynamicDispatchedLibrary extends Node {

        abstract Object execute(Object arg);

        @Specialization(guards = "lib.fitsInInt(arg)")
        @SuppressWarnings("unused")
        int basicCached(Object arg,
                        @ExpectError("Failed to generate code for @GenerateAOT: @CachedLibrary with automatic dispatch cannot be prepared for AOT.Resolve this problem by either: %n" + //
                                        " - Exclude this specialization from AOT with @GenerateAOT.Exclude if it is acceptable to deoptimize for this specialization in AOT compiled code. %n" + //
                                        " - Configure the specialization to be replaced with a more generic specialization. %n" + //
                                        " - Remove the cached parameter value. %n" + //
                                        " - Define a cached library initializer expression for manual dispatch.")//
                        @CachedLibrary(limit = "3") InteropLibrary lib) {
            try {
                return lib.asInt(arg);
            } catch (UnsupportedMessageException e) {
                throw CompilerDirectives.shouldNotReachHere(e);
            }
        }
    }

    abstract static class WithoutAOTSupportDSLNode extends Node {

        abstract Object execute(Object arg);

        @Specialization
        @SuppressWarnings("unused")
        int s0(int arg) {
            return arg;
        }
    }

    @GenerateAOT
    abstract static class ErrorCachedDSLNodeWithNoAOT extends Node {

        abstract Object execute(Object arg);

        @Specialization
        @SuppressWarnings("unused")
        int basicCached(Object arg,
                        @ExpectError("Failed to generate code for @GenerateAOT: Referenced node type cannot be initialized for AOT.Resolve this problem by either: %n" +
                                        " - Exclude this specialization from AOT with @GenerateAOT.Exclude if it is acceptable to deoptimize for this specialization in AOT compiled code. %n" +
                                        " - Configure the specialization to be replaced with a more generic specialization. %n" +
                                        " - Remove the cached parameter value. %n" +
                                        " - Add the @GenerateAOT annotation to node type 'WithoutAOTSupportDSLNode' or one of its super types.") //
                        @Cached WithoutAOTSupportDSLNode dslNode) {
            return (int) dslNode.execute(arg);
        }
    }

}
