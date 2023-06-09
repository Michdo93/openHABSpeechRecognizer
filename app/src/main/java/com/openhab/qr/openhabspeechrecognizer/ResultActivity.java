package com.openhab.qr.openhabspeechrecognizer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

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

        String result = getIntent().getStringExtra("result");
        tvResult.setText(result);

        ImageView settingsIcon = findViewById(R.id.settings_icon);
        settingsIcon.setOnClickListener(this::openSettings);
        settingsIcon.setColorFilter(ContextCompat.getColor(this, android.R.color.white));
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
        } else if (languagePreference.equals("en")) {
            titleText = getString(R.string.activity_result_title_text_en);
        }
    }

    private void setBaseURL() {
        baseURL = protocolPreference + "://" + ipAddressPreference + ":" + portPreference + "/rest";
    }

    private void showToastMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void handleItemExistence(String itemURL) {
        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest getRequest = new JsonObjectRequest(Request.Method.GET, itemURL, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if (response.has("name") && response.getString("name").equals(sttItemNamePreference)) {
                                updateItemValue(response.getString("link"));
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            showToastMessage("Error: Failed to parse JSON response");
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        showToastMessage("Error: Failed to check item existence");
                    }
                }
        );

        queue.add(getRequest);
    }

    private void updateItemValue(String itemLink) {
        RequestQueue queue = Volley.newRequestQueue(this);
        String itemURL = baseURL + "/items/" + sttItemNamePreference;
        String result = getIntent().getStringExtra("result");

        StringRequest postRequest = new StringRequest(Request.Method.POST, itemURL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        showToastMessage("Item value updated successfully");
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        showToastMessage("Error: Failed to update item value");
                    }
                }
        ) {
            @Override
            public byte[] getBody() throws AuthFailureError {
                return result.getBytes();
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-type", CONTENT_TYPE_TEXT);
                headers.put("Accept", ACCEPT_TEXT);
                return headers;
            }
        };

        queue.add(postRequest);
    }

    private void createNewItem() {
        RequestQueue queue = Volley.newRequestQueue(this);
        String itemURL = baseURL + "/items/" + sttItemNamePreference;
        String result = getIntent().getStringExtra("result");

        JSONObject itemData = new JSONObject();
        try {
            itemData.put("type", "String");
            itemData.put("name", sttItemNamePreference);
            itemData.put("value", result);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest putRequest = new JsonObjectRequest(Request.Method.PUT, itemURL, itemData,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        showToastMessage("New item created successfully");
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        showToastMessage("Error: Failed to create new item");
                    }
                }
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
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if (response.has("name") && response.getString("name").equals(sttItemNamePreference)) {
                                updateItemValue(response.getString("link"));
                            } else {
                                createNewItem();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            showToastMessage("Error: Failed to parse JSON response");
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        createNewItem();
                    }
                }
        );

        queue.add(getRequest);
    }

    public void sendCommand(View view) {
        String itemURL = baseURL + "/items/" + sttItemNamePreference;

        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest getRequest = new JsonObjectRequest(Request.Method.GET, itemURL, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if (response.has("name") && response.getString("name").equals(sttItemNamePreference)) {
                                updateItemValue(response.getString("link"));
                            } else {
                                checkItemExistence();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            showToastMessage("Error: Failed to parse JSON response");
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        showToastMessage("Error: Failed to check item existence");
                    }
                }
        );

        queue.add(getRequest);
    }
}
