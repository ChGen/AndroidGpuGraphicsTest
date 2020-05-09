package com.example.graphicstest1;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.view.View;

public class HwUi4Render extends HwUiRender {

	private Object mAttachInfo;
	private Object mHwRenderer;
	private int mWidth;
	private int mHeight;
	private Canvas mHwCanvas;
	private Method mSetViewport;
	private Method mOnPreDraw;
	private Method mOnPostDraw;
	private Method mDrawDisplayList;
	private Method mGetDisplayList;
	private Class<?> mDisplayListClass;
	private TexSurfaceRenderTarget mTarget;

	public HwUi4Render(Activity activity) {
		try {
			mTarget = new TexSurfaceRenderTarget();
			mAttachInfo = getFieldVal(activity.getWindow().getDecorView(), "mAttachInfo");
			mHwRenderer = getFieldVal(mAttachInfo, "mHardwareRenderer"); //android.view.HardwareRenderer$Gl20Renderer
			Method getWidthMethod = getMethod(mHwRenderer, "getWidth", (Class<?>[])null);
			Method getHeightMethod = getMethod(mHwRenderer, "getHeight", (Class<?>[])null);
			mWidth = (int) getWidthMethod.invoke(mHwRenderer, (Object[])null);
			mHeight = (int) getHeightMethod.invoke(mHwRenderer, (Object[])null);

			Class<?> glCanvasClass = Class.forName("android.view.GLES20Canvas");
			Constructor<?> ctor = glCanvasClass.getDeclaredConstructor(new Class[] {boolean.class});
			ctor.setAccessible(true);
			mHwCanvas = (Canvas)ctor.newInstance(true);
			mSetViewport = getMethod(glCanvasClass, "setViewport", int.class, int.class);
			mOnPreDraw = getMethod(glCanvasClass, "onPreDraw", Rect.class);
			mOnPostDraw = getMethod(glCanvasClass,"onPostDraw", (Class[])null);
			mDisplayListClass = Class.forName("android.view.DisplayList");
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN)
				mDrawDisplayList = getMethod(glCanvasClass, "drawDisplayList", mDisplayListClass, Rect.class, int.class);
			else
				mDrawDisplayList = getMethod(glCanvasClass, "drawDisplayList", mDisplayListClass, int.class, int.class, Rect.class);
			mGetDisplayList = Class.forName("android.view.View").getMethod("getDisplayList", (Class<?>[])null);
		} catch(Exception e) {
			onError(e, null, null);
		}
	}

	@Override
	public void setSurface(SurfaceTexture surface) {
		if (surface != null) {
			surface.setDefaultBufferSize(mWidth, mHeight);
			mTarget.init(surface);
		}
		else
		{
			mTarget.cleanup();
		}
	}

	@Override
	public void drawToSurface(View view) {
		mTarget.begin();
		try {
			mSetViewport.invoke(mHwCanvas, mWidth, mHeight);
			mOnPreDraw.invoke(mHwCanvas, (Rect)null);
			int count = mHwCanvas.save();
			Object displayList = mGetDisplayList.invoke(view, (Object[])null);
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN)
				mDrawDisplayList.invoke(mHwCanvas, displayList, null, 1);
			else
				mDrawDisplayList.invoke(mHwCanvas, displayList, view.getWidth(), view.getHeight(), null);//TODO: try to pass mWidth/mHeight?

			//for 4.0 both types of full window drawing cant't be done from onSurfaceTextureAvailable because display list recording 
			//already started and it get recursive and causes exceptions! But if no recursion (single View) it works with some glitches

			//TODO: offer translate() for drawing only one subview and at (0,0)

			mHwCanvas.restoreToCount(count);
			mOnPostDraw.invoke(mHwCanvas, (Object[])null);
		} catch (Exception e) {
			onError(e, null, null);
		}
		mTarget.end();
	}

	@Override
	public Rect getSurfaceSize() {
		return new Rect(0, 0, mWidth, mHeight);
	}

}
