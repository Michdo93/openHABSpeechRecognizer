package com.openhab.qr.openhabspeechrecognizer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import android.content.res.Configuration;

public class ResultActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;
    private String languagePreference;
    private String titleText;
    private String protocolPreference;
    private String ipAddressPreference;
    private String portPreference;
    private String sttItemNamePreference;
    private String baseURL;

    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String CONTENT_TYPE_TEXT = "text/plain; charset=utf-8";
    private static final String ACCEPT_TEXT = "text/plain";
    private static final String ACCEPT_JSON = "application/json";
    private String result;
    private String errorTitle;
    private String updateItemMessage;
    private String updateItemMessageError;
    private String createNewItemMessage;
    private String createNewItemMessageError;
    private String checkItemExistenceMessage;
    private String jsonParseError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        languagePreference = sharedPreferences.getString("language_preference", "en");
        protocolPreference = sharedPreferences.getString("protocol_preference", "http");
        ipAddressPreference = sharedPreferences.getString("ip_address_preference", "192.168.0.1");
        portPreference = sharedPreferences.getString("port_preference", "8080");
        sttItemNamePreference = sharedPreferences.getString("stt_item_name_preference", "openHAB_SpeechRecognizer_STTBuffer");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TextView tvResult = findViewById(R.id.tv_result);

        this.result = getIntent().getStringExtra("result");
        tvResult.setText(result);

        ImageView settingsIcon = findViewById(R.id.settings_icon);
        settingsIcon.setOnClickListener(this::openSettings);
        settingsIcon.setColorFilter(ContextCompat.getColor(this, android.R.color.white));

        SendCommandAsyncTask sendCommandTask = new SendCommandAsyncTask();
        sendCommandTask.execute();
    }

    @Override
    protected void onResume() {
        super.onResume();

        languagePreference = sharedPreferences.getString("language_preference", "en");
        protocolPreference = sharedPreferences.getString("protocol_preference", "http");
        ipAddressPreference = sharedPreferences.getString("ip_address_preference", "192.168.0.1");
        portPreference = sharedPreferences.getString("port_preference", "8080");
        sttItemNamePreference = sharedPreferences.getString("stt_item_name_preference", "openHAB_SpeechRecognizer_STTBuffer");

        setLanguage();
        setTitleText();
        setBaseURL();

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(titleText);
        toolbar.setTitleTextColor(ContextCompat.getColor(this, android.R.color.white));
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void openSettings(View view) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    private void setLanguage() {
        Locale locale = new Locale(languagePreference);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }

    private void setTitleText() {
        if (languagePreference.equals("de")) {
            titleText = getString(R.string.activity_result_title_text_de);
            jsonParseError = getString(R.string.json_parse_error_de);
        } else if (languagePreference.equals("en")) {
            titleText = getString(R.string.activity_result_title_text_en);
            jsonParseError = getString(R.string.json_parse_error_en);
        }
    }

    private void setBaseURL() {
        baseURL = String.format("%s://%s:%s/rest", protocolPreference, ipAddressPreference, portPreference);
    }

    private void showToastMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void showErrorDialog(String message) {
        if (languagePreference.equals("de")) {
            errorTitle = getString(R.string.error_title_de);
        } else if (languagePreference.equals("en")) {
            errorTitle = getString(R.string.error_title_en);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(errorTitle)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void updateItemValue() {
        if (languagePreference.equals("de")) {
            updateItemMessage = getString(R.string.toast_item_updated_de);
            updateItemMessageError = getString(R.string.toast_item_update_error_de);
        } else if (languagePreference.equals("en")) {
            updateItemMessage = getString(R.string.toast_item_updated_en);
            updateItemMessageError = getString(R.string.toast_item_update_error_en);
        }

        RequestQueue queue = Volley.newRequestQueue(this);
        String itemURL = baseURL + "/items/" + sttItemNamePreference;

        JsonArrayRequest postRequest = new JsonArrayRequest(Request.Method.POST, itemURL,
                new JSONArray().put(result),
                response -> showToastMessage(updateItemMessage),
                error -> showErrorDialog(updateItemMessageError)
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-type", CONTENT_TYPE_JSON);
                headers.put("Accept", ACCEPT_JSON);
                return headers;
            }
        };

        queue.add(postRequest);
    }

    private void createNewItem() {
        if (languagePreference.equals("de")) {
            createNewItemMessage = getString(R.string.toast_new_item_created_de);
            createNewItemMessageError = getString(R.string.toast_new_item_creation_error_de);
        } else if (languagePreference.equals("en")) {
            createNewItemMessage = getString(R.string.toast_new_item_created_en);
            createNewItemMessageError = getString(R.string.toast_new_item_creation_error_en);
        }

        RequestQueue queue = Volley.newRequestQueue(this);
        String itemURL = baseURL + "/items/" + sttItemNamePreference;

        JSONObject itemData = new JSONObject();
        try {
            itemData.put("type", "String");
            itemData.put("name", sttItemNamePreference);
            itemData.put("value", result);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest putRequest = new JsonObjectRequest(Request.Method.PUT, itemURL, itemData,
                response -> showToastMessage(createNewItemMessage),
                error -> showErrorDialog(createNewItemMessageError)
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-type", CONTENT_TYPE_JSON);
                headers.put("Accept", ACCEPT_JSON);
                return headers;
            }
        };

        queue.add(putRequest);
    }

    private void checkItemExistence() {
        String itemURL = baseURL + "/items/" + sttItemNamePreference;
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest getRequest = new JsonObjectRequest(Request.Method.GET, itemURL, null,
                response -> {
                    try {
                        if (response.has("name") && response.getString("name").equals(sttItemNamePreference)) {
                            updateItemValue();
                        } else {
                            createNewItem();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        showErrorDialog(jsonParseError);
                    }
                },
                error -> createNewItem()
        );

        queue.add(getRequest);
    }

    private class SendCommandAsyncTask extends AsyncTask<Void, Void, Void> {

        public SendCommandAsyncTask() {
        }

        @Override
        protected Void doInBackground(Void... voids) {
            if (languagePreference.equals("de")) {
                checkItemExistenceMessage = getString(R.string.toast_item_existence_error_de);
            } else if (languagePreference.equals("en")) {
                checkItemExistenceMessage = getString(R.string.toast_item_existence_error_en);
            }

            String itemURL = baseURL + "/items/" + sttItemNamePreference;

            RequestQueue queue = Volley.newRequestQueue(ResultActivity.this);
            JsonObjectRequest getRequest = new JsonObjectRequest(Request.Method.GET, itemURL, null,
                    response -> {
                        try {
                            if (response.has("name") && response.getString("name").equals(sttItemNamePreference)) {
                                updateItemValue();
                            } else {
                                checkItemExistence();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            showErrorDialog(jsonParseError);
                        }
                    },
                    error -> showErrorDialog(checkItemExistenceMessage)
            );

            queue.add(getRequest);

            return null;
        }
    }
}
