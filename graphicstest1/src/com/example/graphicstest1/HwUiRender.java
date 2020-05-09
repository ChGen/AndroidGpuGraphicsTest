package com.example.graphicstest1;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import android.app.Activity;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.view.View;

public abstract class HwUiRender {

	public static HwUiRender create(Activity activity) {
		if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.KITKAT) {
			return new HwUi5Render(activity);
		} else {
			return new HwUi4Render(activity);
		}
	}
	protected void onError(Exception ex, Class<?> clz, String memberName) {
		if (ex != null)
			ex.printStackTrace();
	}

	protected final Field getField(Object object, String field) {
		Class<?> clz = object instanceof Class<?>? (Class<?>)object: object.getClass();
		Field f = null;
		do
		{
			for (Field fld: clz.getDeclaredFields()) {
				if (fld.getName().equals(field)) {
					f = fld;
					f.setAccessible(true);
					break;
				}
			}
			clz = clz.getSuperclass();
		} while (clz != null);
		if (clz == null)
			onError(null, clz, field);
		return f;
	}

	protected final Object getFieldVal(Object object, String field) {
		Field f = getField(object, field);
		try {
			return f.get(object);
		} catch (Exception e) {
			onError(e, object.getClass(), field);
		}
		return null;
	}

	protected final Method getMethod(Object object, String method, Class<?>... parameterTypes) {
		Class<?> clz = object instanceof Class<?>? (Class<?>)object: object.getClass();
		Method m = null;
		do
		{
			try {
				m = clz.getDeclaredMethod(method, parameterTypes);
				m.setAccessible(true);
			} catch(Exception e) {
				onError(null, clz, method);
			}
			clz = clz.getSuperclass();
		} while (clz != null);
		if (clz == null)
			onError(null, clz, method);
		return m;
	}

	protected final Class<?> getNestedClass(Object object, String nestedClass) {
		final Class<?> clz = object instanceof Class<?>? (Class<?>)object: object.getClass();
		for (Class<?> cl: clz.getDeclaredClasses()) {
			if (cl.getName().contains(nestedClass))
				return cl;
		}
		return null;
	}

	@Override
	protected void finalize() throws Throwable {
		setSurface(null);
		super.finalize();
	}

	public abstract Rect getSurfaceSize();
	public abstract void setSurface(SurfaceTexture surface);
	public abstract void drawToSurface(View view);
}
