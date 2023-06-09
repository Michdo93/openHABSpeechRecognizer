package com.openhab.qr.openhabspeechrecognizer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.RecognitionListener;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private Button btnRecord;
    private TextView textRecording;
    private SpeechRecognizer speechRecognizer;
    private boolean isRecording = false;
    private String recordingText;
    private String startRecordingText;

    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    private static final int PERMISSIONS_REQUEST_INTERNET = 1;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        btnRecord = findViewById(R.id.btn_record);
        textRecording = findViewById(R.id.text_recording);

        btnRecord.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    requestRecordAudioPermission();
                    return true;
                case MotionEvent.ACTION_UP:
                    stopRecording();
                    return true;
            }
            return false;
        });

        ImageView settingsIcon = findViewById(R.id.settings_icon);
        settingsIcon.setOnClickListener(this::openSettings);

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateTexts();
    }

    private void requestRecordAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    PERMISSIONS_REQUEST_RECORD_AUDIO);
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.INTERNET},
                    PERMISSIONS_REQUEST_INTERNET);
        } else {
            startRecording();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.INTERNET},
                            PERMISSIONS_REQUEST_INTERNET);
                } else {
                    startRecording();
                }
            }
        } else if (requestCode == PERMISSIONS_REQUEST_INTERNET) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startRecording();
            }
        }
    }

    private void startRecording() {
        isRecording = true;
        updateButtonAppearance();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String languagePreference = sharedPreferences.getString("language_preference", "en");
        Locale locale = getResources().getConfiguration().locale;

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        if (languagePreference.equals("de")) {
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "de-DE");
            recordingText = getString(R.string.recording_text_de);
            startRecordingText = getString(R.string.start_recording_text_de);
        } else if (languagePreference.equals("en")) {
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
            recordingText = getString(R.string.recording_text_en);
            startRecordingText = getString(R.string.start_recording_text_en);
        } else {
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toString());
            recordingText = getString(R.string.recording_text_en);
            startRecordingText = getString(R.string.start_recording_text_en);
        }

        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, recordingText);

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {}

            @Override
            public void onBeginningOfSpeech() {}

            @Override
            public void onRmsChanged(float rmsdB) {}

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {}

            @Override
            public void onError(int error) {}

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String text = matches.get(0);
                    openResultActivity(text);
                }

                isRecording = false;
                updateButtonAppearance();
            }

            @Override
            public void onPartialResults(Bundle partialResults) {}

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });

        speechRecognizer.startListening(intent);
    }

    private void stopRecording() {
        speechRecognizer.stopListening();
        isRecording = false;
        updateButtonAppearance();
    }

    @SuppressLint("SetTextI18n")
    private void updateButtonAppearance() {
        if (isRecording) {
            textRecording.setText(recordingText);
        } else {
            textRecording.setText(startRecordingText);
        }
    }

    private void updateTexts() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String languagePreference = sharedPreferences.getString("language_preference", "en");

        if (languagePreference.equals("de")) {
            recordingText = getString(R.string.recording_text_de);
            startRecordingText = getString(R.string.start_recording_text_de);
        } else if (languagePreference.equals("en")) {
            recordingText = getString(R.string.recording_text_en);
            startRecordingText = getString(R.string.start_recording_text_en);
        }

        updateButtonAppearance();

        String titleText = getString(R.string.activity_main_title_text_en);

        if (languagePreference.equals("de")) {
            titleText = getString(R.string.activity_main_title_text_de);
        } else if (languagePreference.equals("en")) {
            titleText = getString(R.string.activity_main_title_text_en);
        }

        getSupportActionBar().setTitle(titleText);
    }

    private void openResultActivity(String text) {
        Intent intent = new Intent(MainActivity.this, ResultActivity.class);
        intent.putExtra("result", text);
        startActivity(intent);
    }

    public void openSettings(View view) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }
}
