package com.example.graphicstest1;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGL10;

import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.widget.ToggleButton;

import static android.opengl.GLES20.*;

public class MainActivity extends ActionBarActivity {

	private static final String LOG_TAG = "MainActivity";
	private ToggleButton mToggleButton;
	
	private Handler mHandler;

	private SurfaceRenderThread mRenderThread;

	private static final String sSimpleVS =
			"attribute vec4 position;\n" +
					"attribute vec2 texCoords;\n" + 
					"varying vec2 outTexCoords;\n" +
					"uniform mat4 textureTransform;\n" +
					"\nvoid main(void) {\n" +
					" outTexCoords = (textureTransform * vec4(texCoords.x, texCoords.y, 0, 1)).xy;\n" +
					" gl_Position = position;\n" +
					"}\n\n";
	private static final String sSimpleFS =
			"#extension GL_OES_EGL_image_external : require\n" +
					"precision mediump float;\n\n" +
					"varying vec2 outTexCoords;\n" +
					"uniform samplerExternalOES texture;\n" +
					"vec2 dirs[8];\n" +
					"float coefs[8];\n" +
					"\nvoid main(void) {\n" +
					"float blurSize = 0.008;\n" +
					"dirs[0]=vec2(1.0,0.0);dirs[1]=vec2(0.0,1.0);\n" +
					"dirs[2]=vec2(-1.0,0.0);dirs[3]=vec2(0.0,-1.0);\n" +
					"dirs[4]=vec2(0.71,0.71);dirs[5]=vec2(0.71,-0.71);\n" +
					"dirs[6]=vec2(-0.71,-0.71);dirs[7]=vec2(-0.71,0.71);\n" +
					"coefs[7]=0.002;coefs[6]=0.004;coefs[5]=0.01;coefs[4]=0.02;coefs[3]=0.04;coefs[2]=0.06;coefs[1]=0.08;coefs[0]=0.09;\n" +
					"vec4 r = texture2D(texture, outTexCoords)*coefs[0]/4.0;\n" + //"\n" +
					"for(int i=0;i<8;++i)\n" +
					" for(int j=1;j<8;++j)\n" +
					"  r=r+texture2D(texture, outTexCoords+dirs[i]*blurSize*float(j))*(coefs[j]+coefs[j-1])/2.0/2.0;\n" +
					" gl_FragColor = r;\n" +
					"}\n\n";
	private static final String sBasicFS =
			"#extension GL_OES_EGL_image_external : require\n" +
					"precision mediump float;\n\n" +
					"varying vec2 outTexCoords;\n" +
					"uniform samplerExternalOES texture;\n" +
					"\nvoid main(void) {\n" +
					"vec4 r = texture2D(texture, outTexCoords);\n" + //"\n" +
					" gl_FragColor = r;\n" +
					"}\n\n";
	private static final int FLOAT_SIZE_BYTES = 4;
	private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;
	private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
	private static final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 3;
	private final float[] mTriangleVerticesData = {
			// X, Y, Z, U, V
			-1.0f, -1.0f, 0.0f, 0.0f, 0.0f,
			1.0f, -1.0f, 0.0f, 1.0f, 0.0f,
			-1.0f, 1.0f, 0.0f, 0.0f, 1.0f,
			1.0f, 1.0f, 0.0f, 1.0f, 1.0f,
	};
	private static int buildProgram(String vertex, String fragment) {
		int vertexShader = buildShader(vertex, GL_VERTEX_SHADER);
		if (vertexShader == 0) return 0;
		int fragmentShader = buildShader(fragment, GL_FRAGMENT_SHADER);
		if (fragmentShader == 0) return 0;
		int program = glCreateProgram();
		glAttachShader(program, vertexShader);
		checkGlError();  
		glAttachShader(program, fragmentShader);
		checkGlError();
		glLinkProgram(program);
		checkGlError();
		int[] status = new int[1];
		glGetProgramiv(program, GL_LINK_STATUS, status, 0);
		if (status[0] != GL_TRUE) {
			String error = glGetProgramInfoLog(program);
			Log.d(LOG_TAG, "Error while linking program:\n" + error);
			glDeleteShader(vertexShader);
			glDeleteShader(fragmentShader);
			glDeleteProgram(program);
			return 0;
		}
		return program;
	}
	private static int buildShader(String source, int type) {
		int shader = glCreateShader(type);
		glShaderSource(shader, source);
		checkGlError();
		glCompileShader(shader);
		checkGlError();
		int[] status = new int[1];
		glGetShaderiv(shader, GL_COMPILE_STATUS, status, 0);
		if (status[0] != GL_TRUE) {
			String error = glGetShaderInfoLog(shader);
			Log.d(LOG_TAG, "Error while compiling shader:\n" + error);
			glDeleteShader(shader);
			return 0;
		}
		return shader;
	}
	void doJob() {
		//mRenderThread = new RenderThread(getResources(), surface, v);
		
		//init
		final TextureView tv = (TextureView) findViewById(R.id.textureView1);
		SurfaceTexture surface = tv.getSurfaceTexture();
		TexSurfaceRenderTarget rt = new TexSurfaceRenderTarget();
		View view = getWindow().getDecorView();
		rt.init(surface);
		rt.begin();
		int[] buf = new int[1];
		glGenTextures(1, buf, 0);
		int texName = buf[0];
		glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES , texName);
		glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
		FloatBuffer triangleVertices = ByteBuffer.allocateDirect(mTriangleVerticesData.length
				* FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
		triangleVertices.put(mTriangleVerticesData).position(0);
		int program = buildProgram(sSimpleVS, sBasicFS /*sSimpleFS*/);
		int attribPosition = glGetAttribLocation(program, "position");
		checkGlError();
		int attribTexCoords = glGetAttribLocation(program, "texCoords");
		checkGlError();
		int uniformTexture = glGetUniformLocation(program, "texture");
		checkGlError();
	    int textureTranformHandle = glGetUniformLocation(program, "textureTransform");
		checkGlError();
		SurfaceTexture tex = new SurfaceTexture(texName);//TODO: wait for onFrameAvailable() ??
		HwUiRender render = HwUiRender.create(this);
		render.setSurface(tex);
		
		//draw
		for(int i=0;i<1;++i) {
		long startTime = System.currentTimeMillis();
		render.drawToSurface(view);
		rt.begin();//TODO: replace on check & makecurrent
		tex.updateTexImage();
		float[] texTransform = new float[16];
		tex.getTransformMatrix(texTransform);
		glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES , texName);
		checkGlError();
		glUseProgram(program);
		checkGlError();
		glEnableVertexAttribArray(attribPosition);
		checkGlError();
		glEnableVertexAttribArray(attribTexCoords);
		checkGlError();
		glUniform1i(uniformTexture, 0);
		glUniformMatrix4fv(textureTranformHandle, 1, false, texTransform, 0);
		checkGlError();
		triangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);
		glVertexAttribPointer(attribPosition, 3, GL_FLOAT, false,
				TRIANGLE_VERTICES_DATA_STRIDE_BYTES, triangleVertices);
		checkGlError();
		triangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET);
		glVertexAttribPointer(attribTexCoords, 3, GL_FLOAT, false,
				TRIANGLE_VERTICES_DATA_STRIDE_BYTES, triangleVertices);
		glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
		rt.end();
		mToggleButton.setText("" + (System.currentTimeMillis() - startTime));
		}
		render.setSurface(null);
		//rt.cleanup();
	}
	private static void checkGlError() {
		int error = glGetError();
		if (error != GL_NO_ERROR) {
			Log.w(LOG_TAG, "GL error = 0x" + Integer.toHexString(error));
		}
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mHandler = new Handler();
		final TextureView tv = (TextureView) findViewById(R.id.textureView1);
		tv.setSurfaceTextureListener(new SurfaceTextureListener() {
			@Override
			public void onSurfaceTextureUpdated(SurfaceTexture surface) {
			}	
			@Override
			public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
			}
			@Override
			public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
				return false;
			}
			@Override
			public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						doJob();	
					}
				});
				return;	
			}
		});
		mToggleButton = (ToggleButton)findViewById(R.id.toggleButton1);
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		final TextureView tv = (TextureView) findViewById(R.id.textureView1);
		tv.getSurfaceTexture().updateTexImage();
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}


}
