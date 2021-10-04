/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.svm.hosted.jdk;

import com.oracle.svm.core.SubstrateOptions;
import com.oracle.svm.core.annotate.AutomaticFeature;
import com.oracle.svm.core.configure.ResourcesRegistry;
import com.oracle.svm.core.jdk.JNIRegistrationUtil;
import com.oracle.svm.core.jdk.NativeLibrarySupport;
import com.oracle.svm.core.jdk.PlatformNativeLibrarySupport;
import com.oracle.svm.core.jni.JNIRuntimeAccess;
import com.oracle.svm.hosted.FeatureImpl;
import com.oracle.svm.hosted.c.NativeLibraries;
import org.graalvm.compiler.serviceprovider.JavaVersionUtil;
import org.graalvm.nativeimage.ImageSingletons;
import org.graalvm.nativeimage.Platform;
import org.graalvm.nativeimage.Platforms;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;
import org.graalvm.nativeimage.impl.InternalPlatform;

import java.util.Arrays;
import java.util.List;
import java.awt.GraphicsEnvironment;

@Platforms({InternalPlatform.PLATFORM_JNI.class})
@AutomaticFeature
@SuppressWarnings({"unused"})
public class JNIRegistrationAwt extends JNIRegistrationUtil implements Feature {

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        if (JavaVersionUtil.JAVA_SPEC >= 11 && Platform.includedIn(Platform.LINUX.class)) {
            access.registerReachabilityHandler(JNIRegistrationAwt::handlePreferencesClassReachable,
                            clazz(access, "java.awt.Toolkit"),
                            clazz(access, "sun.java2d.cmm.lcms.LCMS"),
                            clazz(access, "java.awt.event.NativeLibLoader"),
                            clazz(access, "sun.awt.NativeLibLoader"),
                            clazz(access, "sun.awt.image.NativeLibLoader"),
                            clazz(access, "java.awt.image.ColorModel"),
                            clazz(access, "sun.awt.X11GraphicsEnvironment"),
                            clazz(access, "sun.font.FontManagerNativeLibrary"),
                            clazz(access, "sun.java2d.Disposer"));
            PlatformNativeLibrarySupport.singleton().addBuiltinPkgNativePrefix("java_awt");
            PlatformNativeLibrarySupport.singleton().addBuiltinPkgNativePrefix("sun_awt");
            PlatformNativeLibrarySupport.singleton().addBuiltinPkgNativePrefix("sun_java2d");
            PlatformNativeLibrarySupport.singleton().addBuiltinPkgNativePrefix("com_sun_java_swing_plaf_gtk");

            access.registerReachabilityHandler(JNIRegistrationAwt::registerFreeType,
                            clazz(access, "sun.font.FontManagerNativeLibrary"));
            PlatformNativeLibrarySupport.singleton().addBuiltinPkgNativePrefix("sun_font");

            access.registerReachabilityHandler(JNIRegistrationAwt::registerLCMS,
                            clazz(access, "sun.java2d.cmm.lcms.LCMS"));

            access.registerReachabilityHandler(JNIRegistrationAwt::registerImagingLib,
                            clazz(access, "sun.awt.image.ImagingLib"));

            access.registerReachabilityHandler(JNIRegistrationAwt::registerJPEG,
                            clazz(access, "sun.awt.image.JPEGImageDecoder"),
                            clazz(access, "com.sun.imageio.plugins.jpeg.JPEGImageReader"),
                            clazz(access, "com.sun.imageio.plugins.jpeg.JPEGImageWriter"));
            PlatformNativeLibrarySupport.singleton().addBuiltinPkgNativePrefix("com_sun_imageio_plugins_jpeg");

            access.registerReachabilityHandler(JNIRegistrationAwt::registerColorProfiles,
                            clazz(access, "java.awt.color.ICC_Profile"));

            access.registerReachabilityHandler(JNIRegistrationAwt::registerFlavorMapProps,
                            clazz(access, "java.awt.datatransfer.SystemFlavorMap"));

            access.registerReachabilityHandler(JNIRegistrationAwt::registerRTFReaderCharsets,
                            clazz(access, "javax.swing.text.rtf.RTFReader"));

            access.registerReachabilityHandler(JNIRegistrationAwt::registerOceanThemeIcons,
                            clazz(access, "javax.swing.plaf.metal.OceanTheme"));

            if (!isHeadless()) {
                access.registerReachabilityHandler(JNIRegistrationAwt::registerBasicLafResources,
                        clazz(access, "javax.swing.plaf.basic.BasicLookAndFeel"));

                access.registerReachabilityHandler(JNIRegistrationAwt::registerDndIcons,
                            clazz(access, "java.awt.dnd.DragSource"));

                access.registerReachabilityHandler(JNIRegistrationAwt::registerKeyCodes,
                        clazz(access, "java.awt.event.KeyEvent"));

                access.registerReachabilityHandler(JNIRegistrationAwt::registerDataTransferer,
                        clazz(access, "sun.awt.datatransfer.DataTransferer"));

                access.registerReachabilityHandler(JNIRegistrationAwt::registerShellFolderManager,
                        clazz(access, "sun.swing.FilePane"));

                access.registerReachabilityHandler(JNIRegistrationAwt::registerFontManager,
                        clazz(access, "sun.font.SunFontManager"));

                access.registerReachabilityHandler(JNIRegistrationAwt::registerGtkFileDialog,
                        clazz(access, "sun.awt.X11.GtkFileDialogPeer"));
            }
        }
    }

    private static void handlePreferencesClassReachable(DuringAnalysisAccess access) {

        JNIRuntimeAccess.register(method(access, "java.lang.System", "setProperty", String.class, String.class));
        JNIRuntimeAccess.register(method(access, "java.lang.System", "loadLibrary", String.class));

        JNIRuntimeAccess.register(java.awt.GraphicsEnvironment.class);
        JNIRuntimeAccess.register(method(access, "java.awt.GraphicsEnvironment", "isHeadless"));

        registerCommonClasses(access);

        NativeLibraries nativeLibraries = getNativeLibraries(access);

        NativeLibrarySupport.singleton().preregisterUninitializedBuiltinLibrary("awt");
        nativeLibraries.addStaticJniLibrary("awt");

        if (isHeadless()) {
            NativeLibrarySupport.singleton().preregisterUninitializedBuiltinLibrary("awt_headless");
            nativeLibraries.addStaticJniLibrary("awt_headless", "awt");
        } else {
            registerHeadfullClasses(access);
            registerLookAndFeelClasses(access);
            registerLinuxX11Classes(access);

            NativeLibrarySupport.singleton().preregisterUninitializedBuiltinLibrary("awt_xawt");
            nativeLibraries.addStaticJniLibrary("awt_xawt", "awt");

            nativeLibraries.addDynamicNonJniLibrary("X11");
            nativeLibraries.addDynamicNonJniLibrary("Xrender");
            nativeLibraries.addDynamicNonJniLibrary("Xext");
            nativeLibraries.addDynamicNonJniLibrary("Xi");
        }

        nativeLibraries.addDynamicNonJniLibrary("stdc++");
        nativeLibraries.addDynamicNonJniLibrary("m");

        access.registerReachabilityHandler(JNIRegistrationAwt::registerHtml32bdtd,
                        clazz(access, "javax.swing.text.html.HTMLEditorKit"));
    }

    private static void registerJPEG(DuringAnalysisAccess access) {
        NativeLibraries nativeLibraries = getNativeLibraries(access);

        NativeLibrarySupport.singleton().preregisterUninitializedBuiltinLibrary("javajpeg");
        nativeLibraries.addStaticJniLibrary("javajpeg");

        JNIRuntimeAccess.register(sun.awt.image.JPEGImageDecoder.class);
        JNIRuntimeAccess.register(method(access, "sun.awt.image.JPEGImageDecoder", "sendHeaderInfo",
                int.class, int.class, boolean.class, boolean.class, boolean.class));
        JNIRuntimeAccess.register(method(access, "sun.awt.image.JPEGImageDecoder", "sendPixels",
                byte[].class, int.class));
        JNIRuntimeAccess.register(method(access, "sun.awt.image.JPEGImageDecoder", "sendPixels",
                int[].class, int.class));
    }

    private static void registerImagingLib(DuringAnalysisAccess access) {
        NativeLibraries nativeLibraries = getNativeLibraries(access);

        NativeLibrarySupport.singleton().preregisterUninitializedBuiltinLibrary("mlib_image");
        nativeLibraries.addStaticJniLibrary("mlib_image");
    }

    private static void registerLCMS(DuringAnalysisAccess access) {
        NativeLibraries nativeLibraries = getNativeLibraries(access);

        NativeLibrarySupport.singleton().preregisterUninitializedBuiltinLibrary("lcms");
        nativeLibraries.addStaticJniLibrary("lcms");
    }

    private static void registerFreeType(DuringAnalysisAccess access) {
        if (SubstrateOptions.StaticExecutable.getValue()) {
            /*
             * Freetype uses fontconfig through dlsym. This may not work in a statically linked
             * executable
             */
            return;
        }
        NativeLibraries nativeLibraries = getNativeLibraries(access);

        NativeLibrarySupport.singleton().preregisterUninitializedBuiltinLibrary("fontmanager");
        nativeLibraries.addStaticJniLibrary("fontmanager", isHeadless() ? "awt_headless" : "awt_xawt");
        nativeLibraries.addStaticJniLibrary("harfbuzz");

        nativeLibraries.addDynamicNonJniLibrary("freetype");

        JNIRuntimeAccess.register(clazz(access, "sun.font.FontConfigManager$FontConfigInfo"));
        JNIRuntimeAccess.register(fields(access, "sun.font.FontConfigManager$FontConfigInfo", "fcVersion", "cacheDirs"));
        JNIRuntimeAccess.register(clazz(access, "sun.font.FontConfigManager$FcCompFont"));
        JNIRuntimeAccess.register(fields(access, "sun.font.FontConfigManager$FcCompFont", "fcName", "firstFont", "allFonts"));
        JNIRuntimeAccess.register(clazz(access, "sun.font.FontConfigManager$FontConfigFont"));
        JNIRuntimeAccess.register(constructor(access, "sun.font.FontConfigManager$FontConfigFont"));
        JNIRuntimeAccess.register(fields(access, "sun.font.FontConfigManager$FontConfigFont", "familyName", "styleStr", "fullName", "fontFile"));
    }

    private static void registerColorProfiles(DuringAnalysisAccess duringAnalysisAccess) {
        ResourcesRegistry resourcesRegistry = ImageSingletons.lookup(ResourcesRegistry.class);
        resourcesRegistry.addResources("sun.java2d.cmm.profiles.*");
    }

    private static void registerFlavorMapProps(DuringAnalysisAccess duringAnalysisAccess) {
        ResourcesRegistry resourcesRegistry = ImageSingletons.lookup(ResourcesRegistry.class);
        resourcesRegistry.addResources("sun.datatransfer.resources.flavormap.properties");
    }

    private static void registerRTFReaderCharsets(DuringAnalysisAccess duringAnalysisAccess) {
        ResourcesRegistry resourcesRegistry = ImageSingletons.lookup(ResourcesRegistry.class);
        resourcesRegistry.addResources("javax.swing.text.rtf.charsets.*");
    }

    private static void registerOceanThemeIcons(DuringAnalysisAccess duringAnalysisAccess) {
        ResourcesRegistry resourcesRegistry = ImageSingletons.lookup(ResourcesRegistry.class);
        resourcesRegistry.addResources("javax.swing.plaf.metal.icons.*");
        resourcesRegistry.addResources("javax.swing.plaf.basic.icons.*");
    }

    private static void registerBasicLafResources(DuringAnalysisAccess duringAnalysisAccess) {
        ResourcesRegistry resourcesRegistry = ImageSingletons.lookup(ResourcesRegistry.class);
        resourcesRegistry.addResourceBundles("com.sun.swing.internal.plaf.basic.resources.basic");
    }

    private static void registerDndIcons(DuringAnalysisAccess duringAnalysisAccess) {
        ResourcesRegistry resourcesRegistry = ImageSingletons.lookup(ResourcesRegistry.class);
        resourcesRegistry.addResources("sun.awt.*");
    }

    private static void registerKeyCodes(DuringAnalysisAccess access) {

        String[] keys = Arrays.stream(java.awt.event.KeyEvent.class
                .getDeclaredFields())
                .filter(f -> f.getType() == Integer.TYPE && f.getName().startsWith("VK_"))
                .map(f -> f.getName())
                .toArray(size -> new String[size]);

        RuntimeReflection.register(fields(access, "java.awt.event.KeyEvent", keys));
    }

    private static void registerDataTransferer(DuringAnalysisAccess access) {

        RuntimeReflection.register(java.lang.String.class);
        RuntimeReflection.register(java.util.List.class);

        RuntimeReflection.register(java.io.Reader.class);
        RuntimeReflection.register(java.io.InputStream.class);
        RuntimeReflection.register(java.io.Serializable.class);
        RuntimeReflection.register(java.nio.CharBuffer.class);
        RuntimeReflection.register(java.nio.ByteBuffer.class);

        RuntimeReflection.register(java.awt.Image.class);

        RuntimeReflection.register(byte[].class);
        RuntimeReflection.register(char[].class);

        RuntimeReflection.register(constructor(access, "java.rmi.MarshalledObject",
                java.lang.Object.class));
        RuntimeReflection.register(method(access, "java.rmi.MarshalledObject", "get"));

        JNIRuntimeAccess.register(method(access, "com.sun.imageio.plugins.jpeg.JPEGImageReader",
                "readInputData", byte[].class, int.class, int.class));
        JNIRuntimeAccess.register(method(access, "com.sun.imageio.plugins.jpeg.JPEGImageReader",
                "skipInputBytes", long.class));
        JNIRuntimeAccess.register(method(access, "com.sun.imageio.plugins.jpeg.JPEGImageReader",
                "warningOccurred", int.class));
        JNIRuntimeAccess.register(method(access, "com.sun.imageio.plugins.jpeg.JPEGImageReader",
                "warningWithMessage", String.class));
        JNIRuntimeAccess.register(method(access, "com.sun.imageio.plugins.jpeg.JPEGImageReader",
                "setImageData", int.class, int.class, int.class, int.class, int.class, byte[].class));
        JNIRuntimeAccess.register(method(access, "com.sun.imageio.plugins.jpeg.JPEGImageReader",
                "acceptPixels", int.class, boolean.class));
        JNIRuntimeAccess.register(method(access, "com.sun.imageio.plugins.jpeg.JPEGImageReader",
                "passStarted", int.class));
        JNIRuntimeAccess.register(method(access, "com.sun.imageio.plugins.jpeg.JPEGImageReader",
                "passComplete"));
        JNIRuntimeAccess.register(method(access, "com.sun.imageio.plugins.jpeg.JPEGImageReader",
                "pushBack", int.class));
        JNIRuntimeAccess.register(method(access, "com.sun.imageio.plugins.jpeg.JPEGImageReader",
                "skipPastImage", int.class));

        JNIRuntimeAccess.register(method(access, "com.sun.imageio.plugins.jpeg.JPEGImageWriter",
                "writeOutputData", byte[].class, int.class, int.class));
        JNIRuntimeAccess.register(method(access, "com.sun.imageio.plugins.jpeg.JPEGImageWriter",
                "warningOccurred", int.class));
        JNIRuntimeAccess.register(method(access, "com.sun.imageio.plugins.jpeg.JPEGImageWriter",
                "warningWithMessage", String.class));
        JNIRuntimeAccess.register(method(access, "com.sun.imageio.plugins.jpeg.JPEGImageWriter",
                "writeMetadata"));
        JNIRuntimeAccess.register(method(access, "com.sun.imageio.plugins.jpeg.JPEGImageWriter",
                "grabPixels", int.class));

        JNIRuntimeAccess.register(fields(access, "javax.imageio.plugins.jpeg.JPEGQTable", "qTable"));
        JNIRuntimeAccess.register(fields(access, "javax.imageio.plugins.jpeg.JPEGHuffmanTable",
                "lengths", "values"));
    }

    private static void registerShellFolderManager(DuringAnalysisAccess access) {
        RuntimeReflection.register(clazz(access, "sun.awt.shell.ShellFolderManager"));
        RuntimeReflection.register(constructor(access, "sun.awt.shell.ShellFolderManager"));
    }

    private static void registerFontManager(DuringAnalysisAccess access) {
        JNIRuntimeAccess.register(fields(access, "sun.font.GlyphLayout$GVData",
                "_count", "_flags", "_glyphs", "_indices", "_positions"));
        JNIRuntimeAccess.register(method(access, "sun.font.GlyphLayout$GVData",
                "grow"));

        if (isLinux()) {
            JNIRuntimeAccess.register(method(access, "java.awt.GraphicsEnvironment",
                    "getLocalGraphicsEnvironment"));
            JNIRuntimeAccess.register(sun.java2d.SunGraphicsEnvironment.class);
            JNIRuntimeAccess.register(method(access, "sun.java2d.SunGraphicsEnvironment",
                    "isDisplayLocal"));
        }
    }

    private static void registerGtkFileDialog(DuringAnalysisAccess access) {
        JNIRuntimeAccess.register(fields(access, "sun.awt.X11.GtkFileDialogPeer", "widget"));
        JNIRuntimeAccess.register(method(access, "sun.awt.X11.GtkFileDialogPeer",
                "filenameFilterCallback", java.lang.String.class));
        JNIRuntimeAccess.register(method(access, "sun.awt.X11.GtkFileDialogPeer",
                "setFileInternal", java.lang.String.class, java.lang.String[].class));
        JNIRuntimeAccess.register(method(access, "sun.awt.X11.GtkFileDialogPeer",
                "setWindow", long.class));
    }

    private static void registerHtml32bdtd(DuringAnalysisAccess duringAnalysisAccess) {
        ResourcesRegistry resourcesRegistry = ImageSingletons.lookup(ResourcesRegistry.class);
        resourcesRegistry.addResources("javax.swing.text.html.parser.html32.bdtd");
    }

    private static void registerDefaultCSS(DuringAnalysisAccess duringAnalysisAccess) {
        ResourcesRegistry resourcesRegistry = ImageSingletons.lookup(ResourcesRegistry.class);
        resourcesRegistry.addResources("javax.swing.text.html.default.css");
    }

    private static NativeLibraries getNativeLibraries(DuringAnalysisAccess access) {
        FeatureImpl.DuringAnalysisAccessImpl a = (FeatureImpl.DuringAnalysisAccessImpl) access;
        return a.getNativeLibraries();
    }

    private static boolean isHeadless() {
        return GraphicsEnvironment.isHeadless();
    }

    private static void registerCommonClasses(DuringAnalysisAccess access) {
        JNIRuntimeAccess.register(java.awt.AlphaComposite.class);
        JNIRuntimeAccess.register(fields(access, "java.awt.AlphaComposite", "extraAlpha", "rule"));

        JNIRuntimeAccess.register(java.awt.Color.class);
        JNIRuntimeAccess.register(method(access, "java.awt.Color", "getRGB"));

        JNIRuntimeAccess.register(java.awt.geom.AffineTransform.class);
        JNIRuntimeAccess.register(fields(access, "java.awt.geom.AffineTransform",
                "m00", "m01", "m02", "m10", "m11", "m12"));

        JNIRuntimeAccess.register(java.awt.geom.GeneralPath.class);
        JNIRuntimeAccess.register(constructor(access, "java.awt.geom.GeneralPath"));
        JNIRuntimeAccess.register(constructor(access, "java.awt.geom.GeneralPath",
                int.class, byte[].class, int.class, float[].class, int.class));

        JNIRuntimeAccess.register(java.awt.geom.Path2D.class);
        JNIRuntimeAccess.register(fields(access, "java.awt.geom.Path2D", "numTypes", "pointTypes", "windingRule"));

        JNIRuntimeAccess.register(java.awt.geom.Path2D.Float.class);
        JNIRuntimeAccess.register(fields(access, "java.awt.geom.Path2D$Float", "floatCoords"));

        JNIRuntimeAccess.register(java.awt.geom.Point2D.Float.class);
        JNIRuntimeAccess.register(fields(access, "java.awt.geom.Point2D$Float", "x", "y"));
        JNIRuntimeAccess.register(constructor(access, "java.awt.geom.Point2D$Float", float.class, float.class));

        JNIRuntimeAccess.register(java.awt.geom.Rectangle2D.Float.class);
        JNIRuntimeAccess.register(fields(access, "java.awt.geom.Rectangle2D$Float", "height", "width", "x", "y"));
        JNIRuntimeAccess.register(constructor(access, "java.awt.geom.Rectangle2D$Float"));
        JNIRuntimeAccess.register(constructor(access, "java.awt.geom.Rectangle2D$Float",
                float.class, float.class, float.class, float.class));


        JNIRuntimeAccess.register(java.awt.image.ColorModel.class);
        JNIRuntimeAccess.register(fields(access, "java.awt.image.ColorModel",
                "colorSpace", "colorSpaceType", "isAlphaPremultiplied", "is_sRGB", "nBits",
                "numComponents", "pData", "supportsAlpha", "transparency"));
        JNIRuntimeAccess.register(method(access, "java.awt.image.ColorModel", "getRGBdefault"));

        JNIRuntimeAccess.register(java.awt.image.IndexColorModel.class);
        JNIRuntimeAccess.register(fields(access, "java.awt.image.IndexColorModel",
                "allgrayopaque", "colorData", "map_size", "rgb", "transparent_index"));

        JNIRuntimeAccess.register(sun.awt.SunHints.class);
        JNIRuntimeAccess.register(fields(access, "sun.awt.SunHints", "INTVAL_STROKE_PURE"));

        JNIRuntimeAccess.register(sun.font.CharToGlyphMapper.class);
        JNIRuntimeAccess.register(method(access, "sun.font.CharToGlyphMapper", "charToGlyph", int.class));

        JNIRuntimeAccess.register(sun.font.Font2D.class);
        JNIRuntimeAccess.register(method(access, "sun.font.Font2D", "canDisplay", char.class));
        JNIRuntimeAccess.register(method(access, "sun.font.Font2D", "charToGlyph", int.class));
        JNIRuntimeAccess.register(method(access, "sun.font.Font2D", "charToVariationGlyph", int.class, int.class));
        JNIRuntimeAccess.register(method(access, "sun.font.Font2D", "getMapper"));
        JNIRuntimeAccess.register(method(access, "sun.font.Font2D", "getTableBytes", int.class));

        JNIRuntimeAccess.register(sun.font.FontStrike.class);
        JNIRuntimeAccess.register(method(access, "sun.font.FontStrike", "getGlyphMetrics", int.class));

        JNIRuntimeAccess.register(clazz(access, "sun.font.FreetypeFontScaler"));
        JNIRuntimeAccess.register(method(access, "sun.font.FreetypeFontScaler", "invalidateScaler"));

        JNIRuntimeAccess.register(sun.font.GlyphList.class);
        JNIRuntimeAccess.register(fields(access, "sun.font.GlyphList",
                "images", "lcdRGBOrder", "lcdSubPixPos", "len", "positions", "usePositions", "x", "y"));

        JNIRuntimeAccess.register(sun.font.PhysicalStrike.class);
        JNIRuntimeAccess.register(fields(access, "sun.font.PhysicalStrike", "pScalerContext"));
        JNIRuntimeAccess.register(method(access, "sun.font.PhysicalStrike", "adjustPoint",
                java.awt.geom.Point2D.Float.class));
        JNIRuntimeAccess.register(method(access, "sun.font.PhysicalStrike",
                "getGlyphPoint", int.class, int.class));

        JNIRuntimeAccess.register(sun.font.StrikeMetrics.class);
        JNIRuntimeAccess.register(constructor(access, "sun.font.StrikeMetrics",
                float.class, float.class, float.class, float.class, float.class,
                float.class, float.class, float.class, float.class, float.class));

        JNIRuntimeAccess.register(sun.font.TrueTypeFont.class);
        JNIRuntimeAccess.register(method(access, "sun.font.TrueTypeFont", "readBlock",
                java.nio.ByteBuffer.class, int.class, int.class));
        JNIRuntimeAccess.register(method(access, "sun.font.TrueTypeFont", "readBytes", int.class, int.class));

        JNIRuntimeAccess.register(sun.font.Type1Font.class);
        JNIRuntimeAccess.register(method(access, "sun.font.Type1Font", "readFile", java.nio.ByteBuffer.class));

        JNIRuntimeAccess.register(sun.java2d.Disposer.class);
        JNIRuntimeAccess.register(method(access, "sun.java2d.Disposer", "addRecord",
                java.lang.Object.class, long.class, long.class));

        JNIRuntimeAccess.register(sun.java2d.InvalidPipeException.class);

        JNIRuntimeAccess.register(sun.java2d.NullSurfaceData.class);

        JNIRuntimeAccess.register(sun.java2d.SunGraphics2D.class);
        JNIRuntimeAccess.register(fields(access, "sun.java2d.SunGraphics2D",
                "clipRegion", "composite", "eargb", "lcdTextContrast", "pixel", "strokeHint"));

        JNIRuntimeAccess.register(sun.java2d.SurfaceData.class);
        JNIRuntimeAccess.register(fields(access, "sun.java2d.SurfaceData", "pData", "valid"));

        JNIRuntimeAccess.register(sun.java2d.loops.Blit.class);
        JNIRuntimeAccess.register(constructor(access, "sun.java2d.loops.Blit", long.class,
                sun.java2d.loops.SurfaceType.class,
                sun.java2d.loops.CompositeType.class,
                sun.java2d.loops.SurfaceType.class));

        JNIRuntimeAccess.register(sun.java2d.loops.BlitBg.class);
        JNIRuntimeAccess.register(constructor(access, "sun.java2d.loops.BlitBg", long.class,
                sun.java2d.loops.SurfaceType.class,
                sun.java2d.loops.CompositeType.class,
                sun.java2d.loops.SurfaceType.class));

        JNIRuntimeAccess.register(sun.java2d.loops.CompositeType.class);
        JNIRuntimeAccess.register(fields(access, "sun.java2d.loops.CompositeType",
                "AnyAlpha", "Src", "SrcNoEa", "SrcOver", "SrcOverNoEa", "Xor"));

        JNIRuntimeAccess.register(sun.java2d.loops.DrawGlyphList.class);
        JNIRuntimeAccess.register(constructor(access, "sun.java2d.loops.DrawGlyphList", long.class,
                sun.java2d.loops.SurfaceType.class,
                sun.java2d.loops.CompositeType.class,
                sun.java2d.loops.SurfaceType.class));

        JNIRuntimeAccess.register(sun.java2d.loops.DrawGlyphListAA.class);
        JNIRuntimeAccess.register(constructor(access, "sun.java2d.loops.DrawGlyphListAA", long.class,
                sun.java2d.loops.SurfaceType.class,
                sun.java2d.loops.CompositeType.class,
                sun.java2d.loops.SurfaceType.class));

        JNIRuntimeAccess.register(sun.java2d.loops.DrawGlyphListLCD.class);
        JNIRuntimeAccess.register(constructor(access, "sun.java2d.loops.DrawGlyphListLCD", long.class,
                sun.java2d.loops.SurfaceType.class,
                sun.java2d.loops.CompositeType.class,
                sun.java2d.loops.SurfaceType.class));

        JNIRuntimeAccess.register(sun.java2d.loops.DrawLine.class);
        JNIRuntimeAccess.register(constructor(access, "sun.java2d.loops.DrawLine", long.class,
                sun.java2d.loops.SurfaceType.class,
                sun.java2d.loops.CompositeType.class,
                sun.java2d.loops.SurfaceType.class));

        JNIRuntimeAccess.register(sun.java2d.loops.DrawParallelogram.class);
        JNIRuntimeAccess.register(constructor(access, "sun.java2d.loops.DrawParallelogram", long.class,
                sun.java2d.loops.SurfaceType.class,
                sun.java2d.loops.CompositeType.class,
                sun.java2d.loops.SurfaceType.class));

        JNIRuntimeAccess.register(sun.java2d.loops.DrawPath.class);
        JNIRuntimeAccess.register(constructor(access, "sun.java2d.loops.DrawPath", long.class,
                sun.java2d.loops.SurfaceType.class,
                sun.java2d.loops.CompositeType.class,
                sun.java2d.loops.SurfaceType.class));

        JNIRuntimeAccess.register(sun.java2d.loops.DrawPolygons.class);
        JNIRuntimeAccess.register(constructor(access, "sun.java2d.loops.DrawPolygons", long.class,
                sun.java2d.loops.SurfaceType.class,
                sun.java2d.loops.CompositeType.class,
                sun.java2d.loops.SurfaceType.class));

        JNIRuntimeAccess.register(sun.java2d.loops.DrawRect.class);
        JNIRuntimeAccess.register(constructor(access, "sun.java2d.loops.DrawRect", long.class,
                sun.java2d.loops.SurfaceType.class,
                sun.java2d.loops.CompositeType.class,
                sun.java2d.loops.SurfaceType.class));

        JNIRuntimeAccess.register(sun.java2d.loops.FillParallelogram.class);
        JNIRuntimeAccess.register(constructor(access, "sun.java2d.loops.FillParallelogram",
                long.class, sun.java2d.loops.SurfaceType.class,
                sun.java2d.loops.CompositeType.class,
                sun.java2d.loops.SurfaceType.class));

        JNIRuntimeAccess.register(sun.java2d.loops.FillPath.class);
        JNIRuntimeAccess.register(constructor(access, "sun.java2d.loops.FillPath", long.class,
                sun.java2d.loops.SurfaceType.class,
                sun.java2d.loops.CompositeType.class,
                sun.java2d.loops.SurfaceType.class));

        JNIRuntimeAccess.register(sun.java2d.loops.FillRect.class);
        JNIRuntimeAccess.register(constructor(access, "sun.java2d.loops.FillRect", long.class,
                sun.java2d.loops.SurfaceType.class,
                sun.java2d.loops.CompositeType.class,
                sun.java2d.loops.SurfaceType.class));

        JNIRuntimeAccess.register(sun.java2d.loops.FillSpans.class);
        JNIRuntimeAccess.register(constructor(access, "sun.java2d.loops.FillSpans", long.class,
                sun.java2d.loops.SurfaceType.class,
                sun.java2d.loops.CompositeType.class,
                sun.java2d.loops.SurfaceType.class));

        JNIRuntimeAccess.register(sun.java2d.loops.GraphicsPrimitive.class);
        JNIRuntimeAccess.register(fields(access, "sun.java2d.loops.GraphicsPrimitive", "pNativePrim"));

        JNIRuntimeAccess.register(sun.java2d.loops.GraphicsPrimitiveMgr.class);
        JNIRuntimeAccess.register(method(access, "sun.java2d.loops.GraphicsPrimitiveMgr",
                "register", sun.java2d.loops.GraphicsPrimitive[].class));

        JNIRuntimeAccess.register(sun.java2d.loops.GraphicsPrimitive[].class);

        JNIRuntimeAccess.register(sun.java2d.loops.MaskBlit.class);
        JNIRuntimeAccess.register(constructor(access, "sun.java2d.loops.MaskBlit", long.class,
                sun.java2d.loops.SurfaceType.class,
                sun.java2d.loops.CompositeType.class,
                sun.java2d.loops.SurfaceType.class));

        JNIRuntimeAccess.register(sun.java2d.loops.MaskFill.class);
        JNIRuntimeAccess.register(constructor(access, "sun.java2d.loops.MaskFill", long.class,
                sun.java2d.loops.SurfaceType.class,
                sun.java2d.loops.CompositeType.class,
                sun.java2d.loops.SurfaceType.class));

        JNIRuntimeAccess.register(sun.java2d.loops.ScaledBlit.class);
        JNIRuntimeAccess.register(constructor(access, "sun.java2d.loops.ScaledBlit", long.class,
                sun.java2d.loops.SurfaceType.class,
                sun.java2d.loops.CompositeType.class,
                sun.java2d.loops.SurfaceType.class));

        JNIRuntimeAccess.register(sun.java2d.loops.SurfaceType.class);
        JNIRuntimeAccess.register(fields(access, "sun.java2d.loops.SurfaceType",
                "Any3Byte", "Any4Byte", "AnyByte", "AnyColor", "AnyInt", "AnyShort",
                "ByteBinary1Bit", "ByteBinary2Bit", "ByteBinary4Bit", "ByteGray", "ByteIndexed",
                "ByteIndexedBm", "FourByteAbgr", "FourByteAbgrPre", "Index12Gray", "Index8Gray",
                "IntArgb", "IntArgbBm", "IntArgbPre", "IntBgr", "IntRgb", "IntRgbx", "OpaqueColor",
                "ThreeByteBgr", "Ushort4444Argb", "Ushort555Rgb", "Ushort555Rgbx", "Ushort565Rgb",
                "UshortGray", "UshortIndexed"));

        JNIRuntimeAccess.register(sun.java2d.loops.TransformHelper.class);
        JNIRuntimeAccess.register(constructor(access, "sun.java2d.loops.TransformHelper", long.class,
                sun.java2d.loops.SurfaceType.class,
                sun.java2d.loops.CompositeType.class,
                sun.java2d.loops.SurfaceType.class));

        JNIRuntimeAccess.register(sun.java2d.loops.XORComposite.class);
        JNIRuntimeAccess.register(fields(access, "sun.java2d.loops.XORComposite",
                "alphaMask", "xorColor", "xorPixel"));


        List.of("sun.java2d.loops.OpaqueCopyAnyToArgb",
                "sun.java2d.loops.OpaqueCopyArgbToAny",
                "sun.java2d.loops.XorCopyArgbToAny",
                "sun.java2d.loops.SetFillRectANY",
                "sun.java2d.loops.SetFillPathANY",
                "sun.java2d.loops.SetFillSpansANY",
                "sun.java2d.loops.SetDrawLineANY",
                "sun.java2d.loops.SetDrawPolygonsANY",
                "sun.java2d.loops.SetDrawPathANY",
                "sun.java2d.loops.SetDrawRectANY",
                "sun.java2d.loops.XorFillRectANY",
                "sun.java2d.loops.XorFillPathANY",
                "sun.java2d.loops.XorFillSpansANY",
                "sun.java2d.loops.XorDrawLineANY",
                "sun.java2d.loops.XorDrawPolygonsANY",
                "sun.java2d.loops.XorDrawPathANY",
                "sun.java2d.loops.XorDrawRectANY",
                "sun.java2d.loops.XorDrawGlyphListANY",
                "sun.java2d.loops.XorDrawGlyphListAAANY")
                .forEach(graphicsPrimitive -> {
                    System.out.printf("[jni registration awt] register primitive (class and constructor): %s%n", graphicsPrimitive);
                    RuntimeReflection.register(clazz(access, graphicsPrimitive));
                    RuntimeReflection.register(constructor(access, graphicsPrimitive));
                });

        JNIRuntimeAccess.register(sun.java2d.pipe.Region.class);
        JNIRuntimeAccess.register(fields(access, "sun.java2d.pipe.Region",
                "bands", "endIndex", "hix", "hiy", "lox", "loy"));

        JNIRuntimeAccess.register(sun.java2d.pipe.RegionIterator.class);
        JNIRuntimeAccess.register(fields(access, "sun.java2d.pipe.RegionIterator",
                "curIndex", "numXbands", "region"));

        JNIRuntimeAccess.register(java.awt.image.BufferedImage.class);
        JNIRuntimeAccess.register(fields(access, "java.awt.image.BufferedImage",
                "colorModel", "imageType", "raster"));
        JNIRuntimeAccess.register(method(access, "java.awt.image.BufferedImage",
                "getRGB", int.class, int.class, int.class, int.class, int[].class, int.class, int.class));
        JNIRuntimeAccess.register(method(access, "java.awt.image.BufferedImage",
                "setRGB", int.class, int.class, int.class, int.class, int[].class, int.class, int.class));

        JNIRuntimeAccess.register(java.awt.image.Raster.class);
        JNIRuntimeAccess.register(fields(access, "java.awt.image.Raster",
                "dataBuffer", "height", "minX", "minY", "numBands", "numDataElements",
                "sampleModel", "sampleModelTranslateX", "sampleModelTranslateY", "width"));

        JNIRuntimeAccess.register(java.awt.image.SampleModel.class);
        JNIRuntimeAccess.register(fields(access, "java.awt.image.SampleModel", "height", "width"));
        JNIRuntimeAccess.register(method(access, "java.awt.image.SampleModel",
                "getPixels", int.class, int.class, int.class, int.class, int[].class,
                java.awt.image.DataBuffer.class));
        JNIRuntimeAccess.register(method(access, "java.awt.image.SampleModel",
                "setPixels", int.class, int.class, int.class, int.class, int[].class,
                java.awt.image.DataBuffer.class));

        JNIRuntimeAccess.register(java.awt.image.SinglePixelPackedSampleModel.class);
        JNIRuntimeAccess.register(fields(access, "java.awt.image.SinglePixelPackedSampleModel",
                "bitMasks", "bitOffsets", "bitSizes", "maxBitSize"));

        JNIRuntimeAccess.register(sun.awt.image.BufImgSurfaceData.ICMColorData.class);
        JNIRuntimeAccess.register(fields(access, "sun.awt.image.BufImgSurfaceData$ICMColorData", "pData"));
        JNIRuntimeAccess.register(constructor(access, "sun.awt.image.BufImgSurfaceData$ICMColorData", long.class));

        JNIRuntimeAccess.register(sun.awt.image.IntegerComponentRaster.class);
        JNIRuntimeAccess.register(fields(access, "sun.awt.image.IntegerComponentRaster",
                "data", "dataOffsets", "pixelStride", "scanlineStride", "type"));
    }

    private static void registerHeadfullClasses(DuringAnalysisAccess access) {
        JNIRuntimeAccess.register(java.awt.AWTEvent.class);
        JNIRuntimeAccess.register(fields(access, "java.awt.AWTEvent", "bdata", "consumed", "id"));

        JNIRuntimeAccess.register(fields(access, "java.awt.Color", "value"));

        JNIRuntimeAccess.register(java.awt.Component.class);
        JNIRuntimeAccess.register(fields(access, "java.awt.Component",
                "appContext", "background", "foreground", "graphicsConfig", "height",
                "isPacked", "name", "peer", "width", "x", "y"));
        JNIRuntimeAccess.register(method(access, "java.awt.Component", "getLocationOnScreen_NoTreeLock"));
        JNIRuntimeAccess.register(method(access, "java.awt.Component", "getParent_NoClientCode"));

        JNIRuntimeAccess.register(java.awt.Font.class);
        JNIRuntimeAccess.register(fields(access, "java.awt.Font", "pData", "size", "style"));
        JNIRuntimeAccess.register(method(access, "java.awt.Font", "getFamily_NoClientCode"));
        JNIRuntimeAccess.register(method(access, "java.awt.Font", "getFontPeer"));

        JNIRuntimeAccess.register(java.awt.Insets.class);
        JNIRuntimeAccess.register(fields(access, "java.awt.Insets", "bottom", "left", "right", "top"));

        JNIRuntimeAccess.register(java.awt.Rectangle.class);
        JNIRuntimeAccess.register(constructor(access, "java.awt.Rectangle",
                int.class, int.class, int.class, int.class));

        JNIRuntimeAccess.register(java.awt.event.InputEvent.class);
        JNIRuntimeAccess.register(fields(access, "java.awt.event.InputEvent", "modifiers"));

        JNIRuntimeAccess.register(java.awt.event.KeyEvent.class);
        JNIRuntimeAccess.register(fields(access, "java.awt.event.KeyEvent",
                "isProxyActive", "keyChar", "keyCode"));

        JNIRuntimeAccess.register(java.awt.image.DirectColorModel.class);
        JNIRuntimeAccess.register(constructor(access, "java.awt.image.DirectColorModel",
                int.class, int.class, int.class, int.class, int.class));

        JNIRuntimeAccess.register(java.lang.ClassLoader.class);
        JNIRuntimeAccess.register(method(access, "java.lang.ClassLoader", "getPlatformClassLoader"));
        JNIRuntimeAccess.register(method(access, "java.lang.ClassLoader", "loadClass", java.lang.String.class));

        JNIRuntimeAccess.register(java.lang.Thread.class);
        JNIRuntimeAccess.register(method(access, "java.lang.Thread", "yield"));

        JNIRuntimeAccess.register(sun.awt.SunToolkit.class);
        JNIRuntimeAccess.register(method(access, "sun.awt.SunToolkit", "awtLock"));
        JNIRuntimeAccess.register(method(access, "sun.awt.SunToolkit", "awtLockNotify"));
        JNIRuntimeAccess.register(method(access, "sun.awt.SunToolkit", "awtLockNotifyAll"));
        JNIRuntimeAccess.register(method(access, "sun.awt.SunToolkit", "awtLockWait", long.class));
        JNIRuntimeAccess.register(method(access, "sun.awt.SunToolkit", "awtUnlock"));

        JNIRuntimeAccess.register(sun.java2d.xr.XRBackendNative.class);
        JNIRuntimeAccess.register(fields(access, "sun.java2d.xr.XRBackendNative",
                "FMTPTR_A8", "FMTPTR_ARGB32", "MASK_XIMG"));

        JNIRuntimeAccess.register(sun.java2d.xr.XRSurfaceData.class);
        JNIRuntimeAccess.register(fields(access, "sun.java2d.xr.XRSurfaceData", "picture", "xid"));

        JNIRuntimeAccess.register(sun.awt.image.ByteComponentRaster.class);
        JNIRuntimeAccess.register(fields(access, "sun.awt.image.ByteComponentRaster",
                "data", "dataOffsets", "pixelStride", "scanlineStride", "type"));

        JNIRuntimeAccess.register(sun.awt.image.GifImageDecoder.class);
        JNIRuntimeAccess.register(fields(access, "sun.awt.image.GifImageDecoder",
                "outCode", "prefix", "suffix"));
        JNIRuntimeAccess.register(method(access, "sun.awt.image.GifImageDecoder",
                "readBytes", byte[].class, int.class, int.class));
        JNIRuntimeAccess.register(method(access, "sun.awt.image.GifImageDecoder", "sendPixels",
                int.class, int.class, int.class, int.class,
                byte[].class, java.awt.image.ColorModel.class));

        JNIRuntimeAccess.register(sun.awt.image.ImageRepresentation.class);
        JNIRuntimeAccess.register(fields(access, "sun.awt.image.ImageRepresentation",
                "numSrcLUT", "srcLUTtransIndex"));

        JNIRuntimeAccess.register(sun.font.FontConfigManager.FcCompFont.class);
        JNIRuntimeAccess.register(fields(access, "sun.font.FontConfigManager$FcCompFont",
                "allFonts", "fcName", "firstFont"));

        JNIRuntimeAccess.register(sun.font.FontConfigManager.FontConfigFont.class);
        JNIRuntimeAccess.register(fields(access, "sun.font.FontConfigManager$FontConfigFont",
                "familyName", "fontFile", "fullName", "styleStr"));

        JNIRuntimeAccess.register(sun.java2d.pipe.ShapeSpanIterator.class);
        JNIRuntimeAccess.register(fields(access, "sun.java2d.pipe.ShapeSpanIterator", "pData"));

        JNIRuntimeAccess.register(java.io.InputStream.class);
        JNIRuntimeAccess.register(method(access, "java.io.InputStream", "available"));
        JNIRuntimeAccess.register(method(access, "java.io.InputStream", "read",
                byte[].class, int.class, int.class));

        JNIRuntimeAccess.register(java.lang.Boolean.class);
        JNIRuntimeAccess.register(constructor(access, "java.lang.Boolean", boolean.class));
        JNIRuntimeAccess.register(java.lang.Float.class);
        JNIRuntimeAccess.register(constructor(access, "java.lang.Float", float.class));
        JNIRuntimeAccess.register(java.lang.Integer.class);
        JNIRuntimeAccess.register(constructor(access, "java.lang.Integer", int.class));
    }

    private static void registerLinuxX11Classes(DuringAnalysisAccess access) {

        JNIRuntimeAccess.register(sun.awt.UNIXToolkit.class);
        JNIRuntimeAccess.register(method(access, "sun.awt.UNIXToolkit", "loadIconCallback",
                byte[].class, int.class, int.class, int.class, int.class, int.class, boolean.class));

        JNIRuntimeAccess.register(clazz(access, "sun.awt.X11.XWindow"));
        JNIRuntimeAccess.register(fields(access, "sun.awt.X11.XWindow", "drawState",
                "graphicsConfig", "target"));

        JNIRuntimeAccess.register(clazz(access, "sun.awt.X11.XFramePeer"));
        JNIRuntimeAccess.register(clazz(access, "sun.awt.X11.XDialogPeer"));

        JNIRuntimeAccess.register(clazz(access, "sun.awt.X11.XRootWindow"));
        JNIRuntimeAccess.register(method(access, "sun.awt.X11.XRootWindow", "getXRootWindow"));

        JNIRuntimeAccess.register(sun.awt.X11.XBaseWindow.class);
        JNIRuntimeAccess.register(fields(access, "sun.awt.X11.XBaseWindow", "window"));
        JNIRuntimeAccess.register(method(access, "sun.awt.X11.XBaseWindow", "getWindow"));

        JNIRuntimeAccess.register(sun.awt.X11.XContentWindow.class);

        JNIRuntimeAccess.register(sun.awt.X11.XErrorHandlerUtil.class);
        JNIRuntimeAccess.register(method(access, "sun.awt.X11.XErrorHandlerUtil", "init", long.class));

        JNIRuntimeAccess.register(sun.awt.X11.XToolkit.class);
        JNIRuntimeAccess.register(fields(access, "sun.awt.X11.XToolkit", "modLockIsShiftLock", "numLockMask"));

        JNIRuntimeAccess.register(sun.awt.X11GraphicsConfig.class);
        JNIRuntimeAccess.register(fields(access, "sun.awt.X11GraphicsConfig", "aData", "bitsPerPixel", "screen"));

        JNIRuntimeAccess.register(sun.awt.X11GraphicsDevice.class);
        JNIRuntimeAccess.register(fields(access, "sun.awt.X11GraphicsDevice", "screen"));
        JNIRuntimeAccess.register(method(access, "sun.awt.X11GraphicsDevice", "addDoubleBufferVisual", int.class));

        JNIRuntimeAccess.register(sun.awt.X11InputMethodBase.class);
        JNIRuntimeAccess.register(fields(access, "sun.awt.X11InputMethodBase", "pData"));

        JNIRuntimeAccess.register(method(access, "sun.awt.X11.XErrorHandlerUtil", "globalErrorHandler",
                long.class, long.class));
    }

    private static void registerLookAndFeelClasses(DuringAnalysisAccess access) {

        RuntimeReflection.register(clazz(access, "com.sun.java.swing.plaf.motif.MotifLookAndFeel"));
        RuntimeReflection.register(constructor(access, "com.sun.java.swing.plaf.motif.MotifLookAndFeel"));

        List.of("MotifButtonUI",
                "MotifCheckBoxUI",
                "MotifFileChooserUI",
                "MotifLabelUI",
                "MotifMenuBarUI",
                "MotifMenuUI",
                "MotifMenuItemUI",
                "MotifCheckBoxMenuItemUI",
                "MotifRadioButtonMenuItemUI",
                "MotifRadioButtonUI",
                "MotifToggleButtonUI",
                "MotifPopupMenuUI",
                "MotifProgressBarUI",
                "MotifScrollBarUI",
                "MotifScrollPaneUI",
                "MotifSliderUI",
                "MotifSplitPaneUI",
                "MotifTabbedPaneUI",
                "MotifTextAreaUI",
                "MotifTextFieldUI",
                "MotifPasswordFieldUI",
                "MotifTextPaneUI",
                "MotifEditorPaneUI",
                "MotifTreeUI",
                "MotifInternalFrameUI",
                "MotifDesktopPaneUI",
                "MotifSeparatorUI",
                "MotifPopupMenuSeparatorUI",
                "MotifOptionPaneUI",
                "MotifComboBoxUI",
                "MotifDesktopIconUI")
                .stream()
                .map(name -> "com.sun.java.swing.plaf.motif." + name)
                .forEach(componentUI -> {
                    RuntimeReflection.register(clazz(access, componentUI));
                    RuntimeReflection.register(method(access, componentUI, "createUI",
                            clazz(access, "javax.swing.JComponent")));
                });

        ResourcesRegistry resourcesRegistry = ImageSingletons.lookup(ResourcesRegistry.class);
        resourcesRegistry.addResources("com.sun.java.swing.plaf.motif.icons.*");

        RuntimeReflection.register(clazz(access, "com.sun.java.swing.plaf.gtk.GTKLookAndFeel"));
        RuntimeReflection.register(constructor(access, "com.sun.java.swing.plaf.gtk.GTKLookAndFeel"));
        RuntimeReflection.register(method(access, "com.sun.java.swing.plaf.gtk.GTKLookAndFeel", "createUI",
                javax.swing.JComponent.class));

        RuntimeReflection.register(clazz(access, "com.sun.java.swing.plaf.gtk.GTKIconFactory"));
        RuntimeReflection.register(method(access, "com.sun.java.swing.plaf.gtk.GTKIconFactory", "getCheckBoxMenuItemCheckIcon"));

        resourcesRegistry.addResources("com.sun.java.swing.plaf.gtk.resources.metacity.*");

        List.of("paintTreeExpandedIcon",
                "paintTreeCollapsedIcon",
                "paintCheckBoxIcon",
                "paintRadioButtonIcon",
                "paintCheckBoxMenuItemCheckIcon",
                "paintRadioButtonMenuItemCheckIcon",
                "paintAscendingSortIcon",
                "paintDescendingSortIcon")
                .forEach(paintIconMethod -> {
                    RuntimeReflection.register(method(access, "com.sun.java.swing.plaf.gtk.GTKPainter", paintIconMethod,
                            javax.swing.plaf.synth.SynthContext.class, java.awt.Graphics.class,
                            int.class, int.class, int.class, int.class, int.class));
                });

        RuntimeReflection.register(method(access, "com.sun.java.swing.plaf.gtk.GTKPainter", "paintMenuArrowIcon",
                javax.swing.plaf.synth.SynthContext.class, java.awt.Graphics.class,
                int.class, int.class, int.class, int.class, int.class,
                clazz(access, "com.sun.java.swing.plaf.gtk.GTKConstants$ArrowType")));

        RuntimeReflection.register(method(access, "com.sun.java.swing.plaf.gtk.GTKPainter", "paintToolBarHandleIcon",
                javax.swing.plaf.synth.SynthContext.class, java.awt.Graphics.class,
                int.class, int.class, int.class, int.class, int.class,
                clazz(access, "com.sun.java.swing.plaf.gtk.GTKConstants$Orientation")));

        List.of("getAscendingSortIcon",
                "getDescendingSortIcon",
                "getTreeExpandedIcon",
                "getTreeCollapsedIcon",
                "getRadioButtonIcon",
                "getCheckBoxIcon",
                "getMenuArrowIcon",
                "getCheckBoxMenuItemCheckIcon",
                "getRadioButtonMenuItemCheckIcon",
                "getToolBarHandleIcon")
                .forEach(getIconMethod -> {
                    RuntimeReflection.register(method(access, "com.sun.java.swing.plaf.gtk.GTKIconFactory", getIconMethod));
                });

        RuntimeReflection.register(method(access, "com.sun.java.swing.plaf.gtk.GTKPainter$ListTableFocusBorder",
                "getSelectedCellBorder"));
    }
}
