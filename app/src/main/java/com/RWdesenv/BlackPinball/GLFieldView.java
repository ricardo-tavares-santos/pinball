package com.RWdesenv.BlackPinball;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.RWdesenv.BlackPinball.elements.FieldElement;
import com.RWdesenv.BlackPinball.util.GLVertexList;
import com.RWdesenv.BlackPinball.util.GLVertexListManager;

public class GLFieldView extends GLSurfaceView implements IFieldRenderer, GLSurfaceView.Renderer {

    public GLFieldView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        setFocusable(true);
    }

    FieldViewManager manager;

    @Override public void setManager(FieldViewManager value) {
        this.manager = value;
    }


    /**
     * Called when the view is touched. Activates flippers, starts a new game if one is not in
     * progress, and launches a ball if one is not in play.
     */
    @Override public boolean onTouchEvent(MotionEvent event) {
        return manager.handleTouchEvent(event);
    }

    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        return manager.handleKeyDown(keyCode, event);
    }

    @Override public boolean onKeyUp(int keyCode, KeyEvent event) {
        return manager.handleKeyUp(keyCode, event);
    }


    GLVertexListManager vertexListManager = new GLVertexListManager();
    GLVertexList lineVertexList;

    // Lookup tables for sin/cos, used to draw circles by approximating with polygons.
    // Larger circles are drawn with more points.
    static float[] SIN_VALUES = new float[20];
    static float[] COS_VALUES = new float[20];
    static float[] HQ_SIN_VALUES = new float[60];
    static float[] HQ_COS_VALUES = new float[60];

    static {
        buildSinCosTables(SIN_VALUES, COS_VALUES);
        buildSinCosTables(HQ_SIN_VALUES, HQ_COS_VALUES);
    }

    static void buildSinCosTables(float[] sinValues, float[] cosValues) {
        if (sinValues.length != cosValues.length) {
            throw new IllegalArgumentException("Array lengths don't match");
        }
        for (int i = 0; i < sinValues.length; i++) {
            double angle = 2 * Math.PI * i / sinValues.length;
            sinValues[i] = (float) Math.sin(angle);
            cosValues[i] = (float) Math.cos(angle);
        }
    }

    void startGLElements(GL10 gl) {
        vertexListManager.begin();
        lineVertexList = vertexListManager.addVertexListForMode(GL10.GL_LINES);
    }

    void endGLElements(GL10 gl) {
        vertexListManager.end();

        gl.glEnable(GL10.GL_DITHER);
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();

        gl.glLineWidth(manager.getLineWidth());

        vertexListManager.render(gl);
    }

    // Implementation of IFieldRenderer drawing methods that FieldElement classes can call.
    // Assumes cacheScaleAndOffsets has been called.
    @Override public void drawLine(float x1, float y1, float x2, float y2, int color) {
        lineVertexList.addVertex(manager.world2pixelX(x1), manager.world2pixelY(y1));
        lineVertexList.addVertex(manager.world2pixelX(x2), manager.world2pixelY(y2));

        float rf = Color.getRed(color) / 255f;
        float gf = Color.getGreen(color) / 255f;
        float bf = Color.getBlue(color) / 255f;
        float af = Color.getAlpha(color) / 255f;
        lineVertexList.addColor(rf, gf, bf, af);
        lineVertexList.addColor(rf, gf, bf, af);
    }

    @Override public void fillCircle(float cx, float cy, float radius, int color) {
        drawCircle(cx, cy, radius, color, GL10.GL_TRIANGLE_FAN);
    }

    @Override public void frameCircle(float cx, float cy, float radius, int color) {
        drawCircle(cx, cy, radius, color, GL10.GL_LINE_LOOP);
    }

    void drawCircle(float cx, float cy, float radius, int color, int mode) {
        GLVertexList circleVertexList = vertexListManager.addVertexListForMode(mode);
        circleVertexList.addColor(Color.getRed(color) / 255f, Color.getGreen(color) / 255f, Color.getBlue(color) / 255f,
                Color.getAlpha(color) / 255f);

        float radiusInPixels = manager.world2pixelX(radius) - manager.world2pixelX(0);
        float[] sinValues = (radiusInPixels > 60) ? HQ_SIN_VALUES : SIN_VALUES;
        float[] cosValues = (radiusInPixels > 60) ? HQ_COS_VALUES : COS_VALUES;
        for (int i = 0; i < sinValues.length; i++) {
            float x = cx + radius * sinValues[i];
            float y = cy + radius * cosValues[i];
            circleVertexList.addVertex(manager.world2pixelX(x), manager.world2pixelY(y));
        }
    }

    final Object renderLock = new Object();
    boolean renderDone;

    @Override public void onDrawFrame(GL10 gl) {
        Field field = manager.getField();
        if (field == null) return;
        synchronized (field) {
            startGLElements(gl);
            field.draw(this);
            endGLElements(gl);
        }

        synchronized (renderLock) {
            renderDone = true;
            renderLock.notify();
        }
    }

    /* requestRender() returns immediately and schedules onDrawFrame for execution on a separate
     * thread. In this case, we want to block until onDrawFrame returns so that the simulation
     * thread in FieldDriver stays in sync with the rendering thread. (Without the locks,
     * FieldDriver registers 60fps even if the actual drawing is much slower).
     */
    @Override public void doDraw() {
        synchronized (renderLock) {
            renderDone = false;
        }

        this.requestRender();

        synchronized (renderLock) {
            while (!renderDone) {
                try {
                    renderLock.wait();
                }
                catch (InterruptedException ex) {
                }
            }
        }
    }

    @Override public void onSurfaceChanged(GL10 gl, int width, int height) {
        gl.glViewport(0, 0, width, height);
    }

    @Override public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        gl.glClearColor(0.0f, 0.0f, 0.0f, 1);
        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST);
        gl.glShadeModel(GL10.GL_FLAT);
        gl.glDisable(GL10.GL_DEPTH_TEST);

        // Alpha support.
        gl.glEnable(GL10.GL_BLEND);
        gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

        // This is supposed to enable antialiased lines, but it seems to both not do that and
        // to make the lines narrow, ignoring the glLineWidth call above.
        // gl.glEnable(GL10.GL_LINE_SMOOTH);

        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();

        GLU.gluOrtho2D(gl, 0, getWidth(), getHeight(), 0);
    }
}
