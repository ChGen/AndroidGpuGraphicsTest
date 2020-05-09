package com.example.graphicstest1;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import android.app.Activity;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.view.Surface;
import android.view.View;

public class HwUi5Render extends HwUiRender {

	private Surface mSurface;

	private Object mViewRootImpl;
	private Object mHwRenderer;
	private Object mChoreographer;
	private Object mDrawCallbacks;
	private Object mAttachInfo;
	private Method mUpdateSurfaceMethod;
	private Method mDrawMethod;
	private Field mCallbacksRunningField;
	private Field mRootNodeNeedsUpdateField;
	private int mWidth;
	private int mHeight;

	public HwUi5Render(Activity activity) {
		try {
			mAttachInfo = getFieldVal(activity.getWindow().getDecorView(), "mAttachInfo");
			mViewRootImpl = getFieldVal(mAttachInfo, "mViewRootImpl"); //android.view.ViewRootImpl
			//Surface surface = (Surface)getFieldVal(mViewRootImpl, "mSurface");
			//SurfaceHolder surfaceHolder = (SurfaceHolder)getFieldVal(mViewRootImpl, "mSurfaceHolder");
			//View mView = (View)getFieldVal(mViewRootImpl, "mView");
			mHwRenderer = getFieldVal(mAttachInfo, "mHardwareRenderer"); //android.view.ThreadedRenderer
			mUpdateSurfaceMethod = getMethod(mHwRenderer, "updateSurface", Surface.class);
			//Method pauseSurfaceMethod = getMethod(mHwRenderer, "pauseSurface", Surface.class); 
			Method getWidthMethod = getMethod(mHwRenderer, "getWidth", (Class<?>[])null);
			Method getHeightMethod = getMethod(mHwRenderer, "getHeight", (Class<?>[])null);
			mWidth = (int) getWidthMethod.invoke(mHwRenderer, (Object[])null);
			mHeight = (int) getHeightMethod.invoke(mHwRenderer, (Object[])null);
			Class<?> hardwareDrawCallbacksInterface = getNestedClass(Class.forName("android.view.HardwareRenderer"), "HardwareDrawCallbacks");
			mDrawMethod = getMethod(mHwRenderer, "draw", View.class, mAttachInfo.getClass(), hardwareDrawCallbacksInterface);
			//Method stopDrawingMethod = getMethod(mHwRenderer, "stopDrawing", (Class<?>[]) null);
			mRootNodeNeedsUpdateField = getField(mHwRenderer, "mRootNodeNeedsUpdate"); 
			mChoreographer = getFieldVal(mHwRenderer, "mChoreographer");
			mCallbacksRunningField = getField(mChoreographer, "mCallbacksRunning");
			mDrawCallbacks = Proxy.newProxyInstance( hardwareDrawCallbacksInterface.getClassLoader(), new Class[] { hardwareDrawCallbacksInterface }, 
					new InvocationHandler() {
				@Override
				public Object invoke(Object proxy, Method method, Object[] args)
						throws Throwable {

					return null;
				}
			});
		} catch(Exception e) {
			onError(e, null, null);
		}
	}

	@Override
	public void setSurface(SurfaceTexture surface) {
		if (surface != null) {
			surface.setDefaultBufferSize(mWidth, mHeight); //TODO: may be we should allow not full sized surfaces??
			mSurface = new Surface(surface);
		} else {
			mSurface = null;
		}
	}

	@Override
	public void drawToSurface(View view) {
		//TODO: set handler and execute all code later, after first drawing will be finished.
		//(try lockCanvas, lockSurface and all other there + stopDrawing() etc.
		Surface oldSurface = (Surface)getFieldVal(mViewRootImpl, "mSurface");
		try {
			mUpdateSurfaceMethod.invoke(mHwRenderer, mSurface);
			boolean old = mCallbacksRunningField.getBoolean(mChoreographer);
			mCallbacksRunningField.set(mChoreographer, true); //TODO: dirty! maybe better invoke updateRootDisplayList & nSyncAndDrawFrame directly?
			mRootNodeNeedsUpdateField.set(mHwRenderer, true);
			mDrawMethod.invoke(mHwRenderer, view, mAttachInfo, mDrawCallbacks);
			mRootNodeNeedsUpdateField.set(mHwRenderer, true);//TODO: methods for fast (W/O mRootNodeNeedsUpdateField) receiving full screenshot
			mCallbacksRunningField.set(mChoreographer, old);
			mUpdateSurfaceMethod.invoke(mHwRenderer, oldSurface);
		} catch (Exception e) {
			onError(e, view.getClass(), null);
		}
	}

	@Override
	public Rect getSurfaceSize() {
		return new Rect(0, 0, mWidth, mHeight);
	}
}
