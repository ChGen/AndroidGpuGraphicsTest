package com.example.graphicstest1;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

import android.graphics.SurfaceTexture;
import android.opengl.GLUtils;
import android.util.Log;

public class TexSurfaceRenderTarget {

	private static final String LOG_TAG = "TexSurfaceRenderTarget";

	private SurfaceTexture mSurfaceTexture;

	private EGL10 mEgl;
	private EGLDisplay mEglDisplay;
	private EGLContext mEglContext;
	private EGLSurface mEglSurface;
	private EGLConfig mEglConfig;
	private int mEglCfgId;

	public TexSurfaceRenderTarget() {
		mEgl = (EGL10) EGLContext.getEGL();
		EGLSurface origDrawSurface = mEgl.eglGetCurrentSurface(EGL10.EGL_DRAW);
		int[] vals = new int[1];
		if (!mEgl.eglQuerySurface(mEgl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY), origDrawSurface,  EGL10.EGL_CONFIG_ID, vals)) {
			Log.w(LOG_TAG, "Failed to query current surface");
			return;
		}
		mEglCfgId = vals[0];
	}

	public void init(SurfaceTexture surface) {
		mSurfaceTexture = surface;
		mEglDisplay = mEgl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
		if (mEglDisplay == EGL10.EGL_NO_DISPLAY) {
			throw new RuntimeException("eglGetDisplay failed "
					+ GLUtils.getEGLErrorString(mEgl.eglGetError()));
		}

		//TODO: size must fit. screen/surface size, or you activity got shifted image on screen.

		int[] vals = new int[1];
		EGLConfig[] configs = new EGLConfig[512];
		if (!mEgl.eglGetConfigs(mEglDisplay, configs, configs.length, vals))
			throw new RuntimeException("egl");
		int cfgs = vals[0];
		EGLConfig origCfg = null;
		for (int i = 0; i < cfgs; ++i)
		{
			if (!mEgl.eglGetConfigAttrib(mEglDisplay, configs[i], EGL10.EGL_CONFIG_ID, vals))
				throw new RuntimeException("egl");
			if (vals[0] == mEglCfgId)
			{
				origCfg = configs[i];
				if (!mEgl.eglGetConfigAttrib(mEglDisplay, configs[i], EGL10.EGL_RENDERABLE_TYPE, vals))
					throw new RuntimeException("egl");
				Log.w(LOG_TAG, "" + vals[0]);
				if (!mEgl.eglGetConfigAttrib(mEglDisplay, configs[i], EGL10.EGL_RED_SIZE, vals))
					throw new RuntimeException("egl");
				Log.w(LOG_TAG, "" + vals[0]);
				if (!mEgl.eglGetConfigAttrib(mEglDisplay, configs[i], EGL10.EGL_GREEN_SIZE, vals))
					throw new RuntimeException("egl");
				Log.w(LOG_TAG, "" + vals[0]);
				if (!mEgl.eglGetConfigAttrib(mEglDisplay, configs[i], EGL10.EGL_BLUE_SIZE, vals))
					throw new RuntimeException("egl");
				Log.w(LOG_TAG, "" + vals[0]);
				if (!mEgl.eglGetConfigAttrib(mEglDisplay, configs[i], EGL10.EGL_ALPHA_SIZE, vals))
					throw new RuntimeException("egl");
				Log.w(LOG_TAG, "" + vals[0]);
				if (!mEgl.eglGetConfigAttrib(mEglDisplay, configs[i], EGL10.EGL_DEPTH_SIZE, vals))
					throw new RuntimeException("egl");
				Log.w(LOG_TAG, "" + vals[0]);
				if (!mEgl.eglGetConfigAttrib(mEglDisplay, configs[i], EGL10.EGL_STENCIL_SIZE, vals))
					throw new RuntimeException("egl");
				Log.w(LOG_TAG, "" + vals[0]);
				break;
			}
		}
		mEglConfig = origCfg == null? chooseEglConfig(): origCfg;
		if (mEglConfig == null) {
			throw new RuntimeException("eglConfig not initialized");
		}
		mEglContext = mEgl.eglGetCurrentContext();
		if (mEglContext == null || mEglContext == EGL10.EGL_NO_CONTEXT ) {
			Log.w(LOG_TAG, "Current context not found. Creating...");
			mEglContext = createContext(mEgl, mEglDisplay, mEglConfig);
		}
		mEglSurface = mEgl.eglCreateWindowSurface(mEglDisplay, mEglConfig, mSurfaceTexture, null);
		if (mEglSurface == null || mEglSurface == EGL10.EGL_NO_SURFACE) {
			int error = mEgl.eglGetError();
			if (error == EGL10.EGL_BAD_NATIVE_WINDOW) {
				Log.e(LOG_TAG, "createWindowSurface returned EGL_BAD_NATIVE_WINDOW.");
				return;
			}
			throw new RuntimeException("createWindowSurface failed "
					+ GLUtils.getEGLErrorString(error));
		}
	}

	public boolean begin() {
		boolean r = mEgl.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext);
		if (r) {
			glClearColor(0, 0, 0, 1.0f);
			glClear(GL_COLOR_BUFFER_BIT);
		}
		return r;
	}

	public void end() {
		mEgl.eglSwapBuffers(mEglDisplay, mEglSurface);
		//TODO: make current original?
	}

	public void cleanup() {
		if (mEglSurface != null) {
			mEgl.eglMakeCurrent(mEglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
			mEgl.eglDestroyContext(mEglDisplay, mEglContext);
			mEgl.eglDestroySurface(mEglDisplay, mEglSurface);
			mEglSurface = null;
			mEglContext = null;
		}
	}

	protected void finalize() throws Throwable {
		cleanup();
	}

	private static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;

	private static EGLContext createContext(EGL10 egl, EGLDisplay eglDisplay, EGLConfig eglConfig) {
		int[] attrib_list = { EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE };
		return egl.eglCreateContext(eglDisplay, eglConfig, EGL10.EGL_NO_CONTEXT, attrib_list);
	}

	private EGLConfig chooseEglConfig() {
		int[] configsCount = new int[1];
		EGLConfig[] configs = new EGLConfig[1];
		int[] configSpec = getConfig();
		if (!mEgl.eglChooseConfig(mEglDisplay, configSpec, configs, 1, configsCount)) {
			throw new IllegalArgumentException("eglChooseConfig failed " +
					GLUtils.getEGLErrorString(mEgl.eglGetError()));
		} else if (configsCount[0] > 0) {
			return configs[0];
		}
		return null;
	}

	static final int EGL_OPENGL_ES2_BIT = 4;

	private static int[] getConfig() {
		return new int[] {
				EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
				EGL10.EGL_RED_SIZE, 8,
				EGL10.EGL_GREEN_SIZE, 8,
				EGL10.EGL_BLUE_SIZE, 8,
				EGL10.EGL_ALPHA_SIZE, 8,
				EGL10.EGL_DEPTH_SIZE, 0,
				EGL10.EGL_STENCIL_SIZE, 0,
				EGL10.EGL_NONE
		};
	}
}
