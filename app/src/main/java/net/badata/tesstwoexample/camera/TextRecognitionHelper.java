package net.badata.tesstwoexample.camera;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;

import com.googlecode.leptonica.android.Pixa;
import com.googlecode.tesseract.android.TessBaseAPI;

/**
 * Created by jsjem on 17.11.2016.
 */
public class TextRecognitionHelper {

	private static final String TAG = "TextRecognitionHelper";

	private static final String TESSERACT_TRAINED_DATA_FOLDER = "tessdata";
	private static final String TESSERACT_PATH = Environment.getExternalStorageDirectory()
			.getAbsolutePath() + "/tess_ocr_example/";

	private final Context applicationContext;
	private final TessBaseAPI tessBaseApi;

	/**
	 * Constructor.
	 *
	 * @param context Application context.
	 */
	public TextRecognitionHelper(final Context context) {
		this.applicationContext = context.getApplicationContext();
		this.tessBaseApi = new TessBaseAPI();
	}

	/**
	 * Initialize tesseract engine.
	 *
	 * @param language Language code in ISO-639-3 format.
	 */
	public void prepareTesseract(final String language) {
		try {
			prepareDirectory(TESSERACT_PATH + TESSERACT_TRAINED_DATA_FOLDER);
		} catch (Exception e) {
			e.printStackTrace();
		}

		copyTessDataFiles(TESSERACT_TRAINED_DATA_FOLDER);
		tessBaseApi.init(TESSERACT_PATH, language);
	}

	private void prepareDirectory(String path) {

		File dir = new File(path);
		if (!dir.exists()) {
			if (!dir.mkdirs()) {
				Log.e(TAG,
						"ERROR: Creation of directory " + path + " failed, check does Android Manifest have permission to write to external storage.");
			}
		} else {
			Log.i(TAG, "Created directory " + path);
		}
	}

	private void copyTessDataFiles(String path) {
		try {
			String fileList[] = applicationContext.getAssets().list(path);

			for (String fileName : fileList) {
				String pathToDataFile = TESSERACT_PATH + path + "/" + fileName;
				if (!(new File(pathToDataFile)).exists()) {
					InputStream in = applicationContext.getAssets().open(path + "/" + fileName);
					OutputStream out = new FileOutputStream(pathToDataFile);
					byte[] buf = new byte[1024];
					int length;
					while ((length = in.read(buf)) > 0) {
						out.write(buf, 0, length);
					}
					in.close();
					out.close();
					Log.d(TAG, "Copied " + fileName + "to tessdata");
				}
			}
		} catch (IOException e) {
			Log.e(TAG, "Unable to copy files to tessdata " + e.getMessage());
		}
	}

	/**
	 * Set image for recognition.
	 *
	 * @param bitmap Image data.
	 */
	public void setBitmap(final Bitmap bitmap) {
		tessBaseApi.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK);
		tessBaseApi.setImage(bitmap);
	}

	/**
	 * Get recognized words regions for image.
	 *
	 * @return List of words regions.
	 */
	public List<Rect> getTextRegions() {
		Pixa regions = tessBaseApi.getWords();
		List<Rect> lineRects = new ArrayList<>(regions.getBoxRects());
		regions.recycle();
		return lineRects;
	}

	/**
	 * Get recognized text for image.
	 *
	 * @return Recognized text string.
	 */
	public String getText() {
		return tessBaseApi.getUTF8Text();
	}

	/**
	 * Clear tesseract data.
	 */
	public void stop() {
		tessBaseApi.clear();
	}
}
