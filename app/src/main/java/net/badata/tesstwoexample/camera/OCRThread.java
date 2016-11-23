package net.badata.tesstwoexample.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.Handler;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by jsjem on 18.11.2016.
 */
public class OCRThread extends Thread {

	private final Handler handler;
	private final TextRecognitionHelper textRecognitionHelper;
	private final AtomicBoolean bitmapChanged;

	private boolean runFlag;
	private Bitmap bitmap;
	private TextRegionsListener regionsListener;
	private TextRecognitionListener textRecognitionListener;

	/**
	 * Constructor.
	 *
	 * @param context Application context.
	 */
	public OCRThread(final Context context) {
		this.textRecognitionHelper = new TextRecognitionHelper(context);
		this.bitmapChanged = new AtomicBoolean();
		this.handler = new Handler();
	}

	/**
	 * Update image data for recognition.
	 *
	 * @param bitmap camera frame data.
	 */
	public void updateBitmap(final Bitmap bitmap) {
		this.bitmap = bitmap;
		bitmapChanged.set(true);
	}

	@Override
	public synchronized void start() {
		this.runFlag = true;
		super.start();
	}

	/**
	 * Stop thread execution.
	 */
	public void cancel() {
		runFlag = false;
		this.regionsListener = null;
		this.textRecognitionListener = null;
	}

	/**
	 * Setter for recognized text region updates listener.
	 *
	 * @param regionsListener Listener for recognized text regions updates.
	 */
	public void setRegionsListener(final TextRegionsListener regionsListener) {
		this.regionsListener = regionsListener;
	}

	/**
	 * Setter for recognized text updates listener.
	 *
	 * @param textRecognitionListener Listener for recognized text updates.
	 */
	public void setTextRecognitionListener(final TextRecognitionListener textRecognitionListener) {
		this.textRecognitionListener = textRecognitionListener;
	}

	/**
	 * Perform text recognition.
	 */
	@Override
	public void run() {
		textRecognitionHelper.prepareTesseract("ukr");
		while (runFlag) {
			if (bitmapChanged.compareAndSet(true, false)) {
				Matrix matrix = new Matrix();
				matrix.postRotate(90);
				Bitmap rotatedBitmap = Bitmap
						.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
				textRecognitionHelper.setBitmap(rotatedBitmap);
				updateTextRegions();
				updateOCRText();
				rotatedBitmap.recycle();
			}
		}
		textRecognitionHelper.stop();
	}

	private void updateTextRegions() {
		if (regionsListener != null) {
			final List<Rect> regions = textRecognitionHelper.getTextRegions();
			handler.post(new Runnable() {
				@Override
				public void run() {
					regionsListener.onTextRegionsRecognized(regions);
				}
			});

		}
	}

	private void updateOCRText() {
		if (textRecognitionListener != null) {
			final String text = textRecognitionHelper.getText();
			handler.post(new Runnable() {
				@Override
				public void run() {
					textRecognitionListener.onTextRecognized(text);
					textRecognitionListener = null;
				}
			});
		}
	}

	/**
	 * Listener for recognized text regions updates.
	 */
	public interface TextRegionsListener {

		/**
		 * Notify about recognized text regions update.
		 *
		 * @param textRegions list of recognized text regions.
		 */
		void onTextRegionsRecognized(final List<Rect> textRegions);
	}

	/**
	 * Listener for recognized text updates.
	 */
	public interface TextRecognitionListener {

		/**
		 * Notify text recognized.
		 *
		 * @param text Recognized text.
		 */
		void onTextRecognized(final String text);
	}
}
