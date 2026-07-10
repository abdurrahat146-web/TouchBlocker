package com.touchblocker.app;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1001;
    private static final int PERMISSION_REQUEST = 2001;

    private boolean blocked = false;
    private ImageView photoView;
    private TextView statusBadge;
    private TextView hintText;
    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Keep screen on and bright while tracing
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        photoView = findViewById(R.id.photoView);
        statusBadge = findViewById(R.id.statusBadge);
        hintText = findViewById(R.id.hintText);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        photoView.setOnClickListener(v -> {
            if (!blocked) {
                pickImage();
            }
        });
        hintText.setOnClickListener(v -> {
            if (!blocked) {
                pickImage();
            }
        });

        updateBadge();
    }

    private void pickImage() {
        String permission = Build.VERSION.SDK_INT >= 33
                ? android.Manifest.permission.READ_MEDIA_IMAGES
                : android.Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, PERMISSION_REQUEST);
            return;
        }

        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            pickImage();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            photoView.setImageURI(imageUri);
            hintText.setVisibility(View.GONE);
        }
    }

    // --- Touch blocking: consumes touches on THIS app's own window only.
    // No overlay permission needed because we never draw over other apps -
    // we only decide whether our own view accepts input.
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (blocked) {
            // Swallow the event completely - do not pass to any child view
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }

    // --- Volume Down toggles the block. Volume Up is left alone so you can
    // still control media volume normally when not actively drawing.
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN
                && event.getAction() == KeyEvent.ACTION_DOWN) {
            toggleBlock();
            return true; // consume it, don't change media volume
        }
        return super.dispatchKeyEvent(event);
    }

    private void toggleBlock() {
        blocked = !blocked;
        updateBadge();
        buzz();
    }

    private void updateBadge() {
        if (blocked) {
            statusBadge.setText("LOCKED (vol-down to unlock)");
            statusBadge.setBackgroundColor(0x66CC0000);
        } else {
            statusBadge.setText("UNLOCKED (vol-down to lock)");
            statusBadge.setBackgroundColor(0x66008800);
        }
    }

    private void buzz() {
        if (vibrator == null) return;
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(60);
        }
    }
}
