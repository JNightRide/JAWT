/*
 * Copyright (c) 2009-2023 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.system.lwjgl;

import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.awt.AwtKeyInput;
import com.jme3.input.awt.AwtMouseInput;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeCanvasContext;
import com.jme3.system.JmeContext;

import java.awt.Canvas;
import java.awt.GraphicsConfiguration;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.geom.AffineTransform;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lwjgl.opengl.awt.AWTGLCanvas;
import org.lwjgl.opengl.awt.GLData;
import org.lwjgl.system.Platform;

/**
 * Class <code>LwjglCanvas</code> that integrates <a href="https://github.com/LWJGLX/lwjgl3-awt">LWJGLX</a>
 * which allows using AWT-Swing components.
 * 
 * <p>
 * If <b>LwjglCanvas</b> throws an exception due to configuration problems, we can debug as follows:
 * <br>
 * - In <code>AppSettings</code>, set this property to enable a debug that displays
 * the effective data for the context.
 * <pre><code>
 * ....
 *  AppSettings settings = new AppSettings(true);
 *  settings.putBoolean("GLDataEffectiveDebug", true);
 * ...
 * </code></pre>
 * 
 * @author wil
 */
public class LwjglCanvas extends LwjglWindow implements JmeCanvasContext, Runnable {

    /** Logger class. */
    private static final Logger LOGGER = Logger.getLogger(LwjglCanvas.class.getName());
    
    /** GL versions map. */
    private static final Map<String, Consumer<GLData>> RENDER_CONFIGS = new HashMap<>();
    
    /** Type of operating system where this context is running. */
    private static final Platform OS = Platform.get();
    
    /*
        Register the different versions.
    */
    static {
        RENDER_CONFIGS.put(AppSettings.LWJGL_OPENGL30, (data) -> {
            data.majorVersion = 3;
            data.minorVersion = 0;
        });
        RENDER_CONFIGS.put(AppSettings.LWJGL_OPENGL31, (data) -> {
            data.majorVersion = 3;
            data.minorVersion = 1;
        });
        RENDER_CONFIGS.put(AppSettings.LWJGL_OPENGL32, (data) -> {
            data.majorVersion = 3;
            data.minorVersion = 2;
            data.profile = GLData.Profile.COMPATIBILITY;
        });
        RENDER_CONFIGS.put(AppSettings.LWJGL_OPENGL33, (data) -> {
            data.majorVersion = 3;
            data.minorVersion = 3;
            data.profile = GLData.Profile.COMPATIBILITY;
        });
        RENDER_CONFIGS.put(AppSettings.LWJGL_OPENGL40, (data) -> {
            data.majorVersion = 4;
            data.minorVersion = 0;
            data.profile = GLData.Profile.COMPATIBILITY;
        });
        RENDER_CONFIGS.put(AppSettings.LWJGL_OPENGL41, (data) -> {
            data.majorVersion = 4;
            data.minorVersion = 1;
            data.profile = GLData.Profile.COMPATIBILITY;
        });
        RENDER_CONFIGS.put(AppSettings.LWJGL_OPENGL42, (data) -> {
            data.majorVersion = 4;
            data.minorVersion = 2;
            data.profile = GLData.Profile.COMPATIBILITY;
        });
        RENDER_CONFIGS.put(AppSettings.LWJGL_OPENGL43, (data) -> {
            data.majorVersion = 4;
            data.minorVersion = 3;
            data.profile = GLData.Profile.COMPATIBILITY;
        });
        RENDER_CONFIGS.put(AppSettings.LWJGL_OPENGL44, (data) -> {
            data.majorVersion = 4;
            data.minorVersion = 4;
            data.profile = GLData.Profile.COMPATIBILITY;
        });
        RENDER_CONFIGS.put(AppSettings.LWJGL_OPENGL45, (data) -> {
            data.majorVersion = 4;
            data.minorVersion = 5;
            data.profile = GLData.Profile.COMPATIBILITY;
        });
    }
    
    private class LwjglAWTGLCanvas extends AWTGLCanvas {

        public LwjglAWTGLCanvas(GLData data) {
            super(data);
        }

        @Override
        public void initGL() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void paintGL() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void afterRender() {
            super.afterRender();
        }

        @Override
        public void beforeRender() {
            super.beforeRender();
        }

        @Override
        public void disposeCanvas() { /* no */ }
        
        @Override
        public synchronized void addComponentListener(ComponentListener l) {
            super.addComponentListener(l);
        }
        
        public GLData getGLDataEffective() {
            return effective;
        }
        
        public void doDisposeCanvas() {
            if (OS == Platform.MACOSX) {
                platformCanvas.dispose();
            }
        }
        
        @Override
        public void addNotify() {
            super.addNotify();
            synchronized (lock) {
                hasNativePeer.set(true);
            }
            requestFocusInWindow();
        }
        
        @Override
        public void removeNotify() {
            synchronized (lock) {
                hasNativePeer.set(false);
            }       
            long oldglcontext = context;
            super.removeNotify();
            context = oldglcontext;
        }
    }
    
    /** Canvas-AWT. */
    private final LwjglAWTGLCanvas canvas;
    
    /**
     * Configuration data to start the AWT context, this is used by the
     * {@code lwjgl-awt} library.
     */
    private GLData glData;
    
    /** Used to display the effective data for the {@code AWT-Swing} drawing surface per console. */
    private final AtomicBoolean showGLDataEffective = new AtomicBoolean(false);
    
    /** Used to notify the canvas status ({@code remove()/add()}). */
    private final AtomicBoolean hasNativePeer = new AtomicBoolean(false);
    
    /** Notify if the canvas is visible and has a parent.*/
    private final AtomicBoolean showing = new AtomicBoolean(false);
    
    /** Notify if there is a change in canvas dimensions. */
    private AtomicBoolean needResize = new AtomicBoolean(false);

    /** Semaphort used to check the "terminate" signal. */
    private final Semaphore signalTerminate = new Semaphore(0);

    /** lock-object. */
    private final Object lock = new Object();
    private int framebufferWidth = 1;
    private int framebufferHeight = 1;

    private AwtKeyInput keyInput;
    private AwtMouseInput mouseInput;
    
    public LwjglCanvas() {
        super(JmeContext.Type.Canvas);

        glData = new GLData();
        canvas = new LwjglAWTGLCanvas(glData);
        canvas.setIgnoreRepaint(true);
        canvas.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {                
                synchronized (lock) {
                    GraphicsConfiguration gc = canvas.getGraphicsConfiguration();
                    if (gc == null) {
                        return;
                    }
                    
                    AffineTransform at = gc.getDefaultTransform();
                    float sx = (float) at.getScaleX(),
                          sy = (float) at.getScaleY();

                    int fw = (int) (canvas.getWidth() * sx);
                    int fh = (int) (canvas.getHeight() * sy);

                    if (fw != framebufferWidth || fh != framebufferHeight) {
                        framebufferWidth = Math.max(fw, 1);
                        framebufferHeight = Math.max(fh, 1);
                        needResize.set(true);
                    }
                }
            }            
        });
    }
    
    @Override
    protected void createContext(AppSettings settings)  {
        super.createContext(settings);        
        RENDER_CONFIGS.computeIfAbsent(settings.getRenderer(), (t) -> {
            return (data) -> {
                data.majorVersion = 2;
                data.minorVersion = 0;
            };
        }).accept(glData);
        
        if (settings.getBitsPerPixel() == 24) {
            glData.redSize = 8;
            glData.greenSize = 8;
            glData.blueSize = 8;            
        } else if (settings.getBitsPerPixel() == 16) {
            glData.redSize = 5;
            glData.greenSize = 6;
            glData.blueSize = 5;            
        }
        
        // Enable vsync for LWJGL3-AWT
        if (settings.isVSync()) {
            glData.swapInterval = 1;
        } else {
            glData.swapInterval = 0;
        }
        
        showGLDataEffective.set(settings.getBoolean("GLDataEffectiveDebug"));
        
        glData.depthSize = settings.getBitsPerPixel();
        glData.alphaSize = settings.getAlphaBits();
        glData.sRGB = settings.isGammaCorrection();
        
        glData.depthSize = settings.getDepthBits();
        glData.stencilSize = settings.getStencilBits();
        glData.samples = settings.getSamples();
        glData.stereo = settings.useStereo3D();  
       
        glData.debug = settings.isGraphicsDebug();
        glData.api = GLData.API.GL;
        allowSwapBuffers = false;
    }
    
    @Override
    public Canvas getCanvas() {
        return canvas;
    }

    @Override
    protected void showWindow() {
    }

    @Override
    protected void setWindowIcon(final AppSettings settings) {
    }

    @Override
    public void setTitle(String title) {
    }

    @Override
    public KeyInput getKeyInput() {
        if (keyInput == null) {
            keyInput = new AwtKeyInput();
            keyInput.setInputSource(canvas);
        }
        return keyInput;
    }

    @Override
    public MouseInput getMouseInput() {
        if (mouseInput == null) {
            mouseInput = new AwtMouseInput();
            mouseInput.setInputSource(canvas);
        }
        return mouseInput;
    }

    public boolean checkVisibilityState() {
        if (!hasNativePeer.get()) {
            synchronized (lock) {
                canvas.doDisposeCanvas();
            }
            return false;
        }

        boolean currentShowing = canvas.isShowing();
        showing.set(currentShowing);
        return currentShowing;
    }


    @Override
    protected void destroyContext() {
        synchronized (lock) {
            canvas.doDisposeCanvas();
        }

        // request the cleanup
        signalTerminate.release();
        super.destroyContext();
    }

    @Override
    protected void runLoop() {
        if (needResize.get()) {
            needResize.set(false);
            settings.setResolution(framebufferWidth, framebufferHeight);
            listener.reshape(framebufferWidth, framebufferHeight);
        }

        if (!checkVisibilityState()) {
            return;
        }

        canvas.beforeRender();
        try {
            super.runLoop();
            canvas.swapBuffers();
        } finally {
            canvas.afterRender();
        }
        
        // Whether it is necessary to know the effective attributes to 
        // initialize the LWJGL3-AWT context
        if (showGLDataEffective.get()) {
            showGLDataEffective.set(false);
            System.out.println(MessageFormat.format("[ DEBUGGER ] :Effective data to initialize the LWJGL3-AWT context\n{0}", 
                                                getPrintContextInitInfo(canvas.getGLDataEffective())));
        }

        try {
            if (signalTerminate.tryAcquire(10, TimeUnit.MILLISECONDS)) {
                canvas.doDisposeCanvas();
            }
        } catch (InterruptedException ignored) { }
    }
    
    @Override
    protected void printContextInitInfo() {
        super.printContextInitInfo();
        LOGGER.log(Level.INFO, "Initializing LWJGL3-AWT with jMonkeyEngine\n{0}", getPrintContextInitInfo(glData));
    }
    
    /**
     * Returns a string with the information obtained from <code>GLData</code>
     * so that it can be displayed.
     * 
     * @param glData context information
     * @return String
     */
    protected String getPrintContextInitInfo(GLData glData) {
        StringBuilder sb = new StringBuilder();
            sb.append(" *  Double Buffer: ").append(glData.doubleBuffer);
            sb.append('\n')
              .append(" *  Stereo: ").append(glData.stereo);
            sb.append('\n')
              .append(" *  Red Size: ").append(glData.redSize);
            sb.append('\n')
              .append(" *  Rreen Size: ").append(glData.greenSize);
            sb.append('\n')
              .append(" *  Blue Size: ").append(glData.blueSize);
            sb.append('\n')
              .append(" *  Alpha Size: ").append(glData.alphaSize);
            sb.append('\n')
              .append(" *  Depth Size: ").append(glData.depthSize);
            sb.append('\n')
              .append(" *  Stencil Size: ").append(glData.stencilSize);
            sb.append('\n')
              .append(" *  Accum Red Size: ").append(glData.accumRedSize);
            sb.append('\n')
              .append(" *  Accum Green Size: ").append(glData.accumGreenSize);
            sb.append('\n')
              .append(" *  Accum Blue Size: ").append(glData.accumBlueSize);
            sb.append('\n')
              .append(" *  Accum Alpha Size: ").append(glData.accumAlphaSize);
            sb.append('\n')
              .append(" *  Sample Buffers: ").append(glData.sampleBuffers);
            sb.append('\n')
              .append(" *  Share Context: ").append(glData.shareContext);
            sb.append('\n')
              .append(" *  Major Version: ").append(glData.majorVersion);
            sb.append('\n')
              .append(" *  Minor Version: ").append(glData.minorVersion);
            sb.append('\n')
              .append(" *  Forward Compatible: ").append(glData.forwardCompatible);
            sb.append('\n')
              .append(" *  Profile: ").append(glData.profile);
            sb.append('\n')
              .append(" *  API: ").append(glData.api);
            sb.append('\n')
              .append(" *  Debug: ").append(glData.debug);
            sb.append('\n')
              .append(" *  Swap Interval: ").append(glData.swapInterval);
            sb.append('\n')
              .append(" *  SRGB (Gamma Correction): ").append(glData.sRGB);
            sb.append('\n')
              .append(" *  Pixel Format Float: ").append(glData.pixelFormatFloat);
            sb.append('\n')
              .append(" *  Context Release Behavior: ").append(glData.contextReleaseBehavior);
            sb.append('\n')
              .append(" *  Color Samples NV: ").append(glData.colorSamplesNV);
            sb.append('\n')
              .append(" *  Swap Group NV: ").append(glData.swapGroupNV);
            sb.append('\n')
              .append(" *  Swap Barrier NV: ").append(glData.swapBarrierNV);
            sb.append('\n')
              .append(" *  Robustness: ").append(glData.robustness);
            sb.append('\n')
              .append(" *  Lose Context On Reset: ").append(glData.loseContextOnReset);
            sb.append('\n')
              .append(" *  Context Reset Isolation: ").append(glData.contextResetIsolation);
        return String.valueOf(sb);
    }
    
    @Override
    public int getFramebufferHeight() {
        return framebufferHeight;
    }

    @Override
    public int getFramebufferWidth() {
        return framebufferWidth;
    }
}
