package net.badata.tesstwoexample;


import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import net.badata.tesstwoexample.camera.CameraView;
import net.badata.tesstwoexample.camera.OCRThread;

/**
 * Created by jsjem on 18.11.2016.
 */
public class RecognitionActivity extends AppCompatActivity implements OCRThread.TextRecognitionListener {

	private CameraView cameraView;
	private ProgressDialog progressDialog;

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_recognition);
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayShowHomeEnabled(false);
		init();
	}

	private void init() {
		cameraView = (CameraView) findViewById(R.id.camera_surface);
		progressDialog = new ProgressDialog(this);
		progressDialog.setMessage(getString(R.string.progress_message_ocr));
	}

	/**
	 * Perform OCR for camera frame.
	 *
	 * @param view Button instance.
	 */
	public void makeOCR(final View view) {
		progressDialog.show();
		cameraView.makeOCR(this);
	}

	private void showOCRDialog(final String recognizedText) {
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
		dialogBuilder.setTitle(R.string.dialog_ocr_title);
		dialogBuilder.setPositiveButton(R.string.dialog_ocr_button, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, final int which) {
				// empty
			}
		});
		dialogBuilder.setMessage(recognizedText);
		dialogBuilder.create().show();
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		getMenuInflater().inflate(R.menu.recognition, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_show_bounds:
				item.setChecked(!item.isChecked());
				cameraView.setShowTextBounds(item.isChecked());
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onTextRecognized(final String text) {
		progressDialog.dismiss();
		showOCRDialog(text);
	}
}
