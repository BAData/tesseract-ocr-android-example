package net.badata.tesstwoexample.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jsjem on 18.11.2016.
 *
 */
public class CameraView extends SurfaceView implements Camera.PreviewCallback, SurfaceHolder.Callback,
		OCRThread.TextRegionsListener {

	private final List<Rect> regions = new ArrayList<>();

	private Camera mCamera;
	private byte[] mVideoSource;
	private Bitmap mBackBuffer;
	private OCRThread ocrThread;

	private Paint focusPaint;
	private Rect focusRect;

	private Paint paintText;

	private float horizontalRectRation;
	private float verticalRectRation;


	static {
		System.loadLibrary("livecamera");
	}

	public CameraView(final Context context) {
		this(context, null);
	}

	public CameraView(final Context context, final AttributeSet attributes) {
		super(context, attributes);
		getHolder().addCallback(this);
		setWillNotDraw(false);
		focusPaint = new Paint();
		focusPaint.setColor(0xeed7d7d7);
		focusPaint.setStyle(Paint.Style.STROKE);
		focusPaint.setStrokeWidth(2);
		focusRect = new Rect(0, 0, 0, 0);

		paintText = new Paint();
		paintText.setColor(0xeeff0000);
		paintText.setStyle(Paint.Style.STROKE);
		paintText.setStrokeWidth(4);

		ocrThread = new OCRThread(context);
		horizontalRectRation = 1.0f;
		verticalRectRation = 1.0f;
	}

	public void setShowTextBounds(final boolean show) {
		regions.clear();
		ocrThread.setRegionsListener(show ? this : null);
		invalidate();
	}

	public void makeOCR(final OCRThread.TextRecognitionListener listener) {
		ocrThread.setTextRecognitionListener(listener);
	}

	public native void decode(final Bitmap pTarget, final byte[] pSource);


	@Override
	protected void onDraw(final Canvas canvas) {
		if (focusRect.width() > 0) {
			canvas.drawRect(focusRect, focusPaint);
		}
		if (mCamera != null) {
			mCamera.addCallbackBuffer(mVideoSource);
			drawTextBounds(canvas);
		}
	}

	private void drawTextBounds(final Canvas canvas) {
		for (Rect region : regions) {
			canvas.drawRect(region.left * horizontalRectRation, region.top * verticalRectRation,
					region.right * horizontalRectRation, region.bottom * verticalRectRation, paintText);
		}
	}

	@Override
	public void onPreviewFrame(final byte[] bytes, final Camera camera) {
		decode(mBackBuffer, bytes);
		ocrThread.updateBitmap(mBackBuffer);
	}

	@Override
	public void surfaceCreated(final SurfaceHolder surfaceHolder) {
		try {
			mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
			mCamera.setDisplayOrientation(90);
			mCamera.setPreviewDisplay(surfaceHolder);
			mCamera.setPreviewCallbackWithBuffer(this);
			startOcrThread();
		} catch (IOException eIOException) {
			mCamera.release();
			mCamera = null;
			throw new IllegalStateException();
		}
	}

	private void startOcrThread() {
		ocrThread.start();
	}

	@Override
	public void surfaceChanged(final SurfaceHolder surfaceHolder, final int format, final int width, final int height) {
		mCamera.stopPreview();
		Size lSize = findBestResolution();
		updateTextRectsRatio(width, height, lSize);
		PixelFormat lPixelFormat = new PixelFormat();
		PixelFormat.getPixelFormatInfo(mCamera.getParameters()
				.getPreviewFormat(), lPixelFormat);
		int lSourceSize = lSize.width * lSize.height * lPixelFormat.bitsPerPixel / 8;
		mVideoSource = new byte[lSourceSize];
		mBackBuffer = Bitmap.createBitmap(lSize.width, lSize.height,
				Bitmap.Config.ARGB_8888);
		Camera.Parameters lParameters = mCamera.getParameters();
		lParameters.setPreviewSize(lSize.width, lSize.height);
		mCamera.setParameters(lParameters);
		mCamera.addCallbackBuffer(mVideoSource);
		mCamera.startPreview();
	}

	private Size findBestResolution() {
		List<Size> lSizes = mCamera.getParameters().getSupportedPreviewSizes();
		Size lSelectedSize = mCamera.new Size(0, 0);
		for (Size lSize : lSizes) {
			if ((lSize.width >= lSelectedSize.width) && (lSize.height >= lSelectedSize.height)) {
				lSelectedSize = lSize;
			}
		}
		if ((lSelectedSize.width == 0) || (lSelectedSize.height == 0)) {
			lSelectedSize = lSizes.get(0);
		}
		return lSelectedSize;
	}

	private void updateTextRectsRatio(final int width, final int height, final Size cameraSize) {
		verticalRectRation = ((float) height) / cameraSize.width;
		horizontalRectRation = ((float) width) / cameraSize.height;
	}

	@Override
	public void surfaceDestroyed(final SurfaceHolder surfaceHolder) {
		if (mCamera != null) {
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;
			mVideoSource = null;
			mBackBuffer = null;
			stopOcrThread();
		}
	}

	private void stopOcrThread() {
		boolean retry = true;
		ocrThread.cancel();
		ocrThread.setRegionsListener(null);
		while (retry) {
			try {
				ocrThread.join();
				retry = false;
			} catch (InterruptedException e) {
			}
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			float x = event.getX();
			float y = event.getY();

			Rect touchRect = new Rect(
					(int) (x - 100),
					(int) (y - 100),
					(int) (x + 100),
					(int) (y + 100));

			final Rect targetFocusRect = new Rect(
					touchRect.left * 2000 / this.getWidth() - 1000,
					touchRect.top * 2000 / this.getHeight() - 1000,
					touchRect.right * 2000 / this.getWidth() - 1000,
					touchRect.bottom * 2000 / this.getHeight() - 1000);

			doTouchFocus(targetFocusRect);
			focusRect = touchRect;
			invalidate();

			Handler handler = new Handler();
			handler.postDelayed(new Runnable() {

				@Override
				public void run() {
					focusRect = new Rect(0, 0, 0, 0);
					invalidate();
				}
			}, 1000);
		}
		return false;
	}

	private void doTouchFocus(final Rect tfocusRect) {
		try {
			final List<Camera.Area> focusList = new ArrayList<Camera.Area>();
			Camera.Area focusArea = new Camera.Area(tfocusRect, 1000);
			focusList.add(focusArea);

			Camera.Parameters para = mCamera.getParameters();
			para.setFocusAreas(focusList);
			para.setMeteringAreas(focusList);
			mCamera.setParameters(para);
			mCamera.autoFocus(myAutoFocusCallback);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * AutoFocus callback
	 */
	Camera.AutoFocusCallback myAutoFocusCallback = new Camera.AutoFocusCallback() {

		@Override
		public void onAutoFocus(boolean arg0, Camera arg1) {
			if (arg0) {
				mCamera.cancelAutoFocus();
			}
		}
	};

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onTextRegionsRecognized(final List<Rect> textRegions) {
		regions.clear();
		regions.addAll(textRegions);
		invalidate();
	}
}
