package org.openhab.speechrecognizer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.openhab.android.rest.client.CRUD;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;
import android.content.res.Configuration;

@SuppressWarnings("ALL")
public class ResultActivity extends AppCompatActivity {
    private CRUD crud;
    private SharedPreferences sharedPreferences;
    private String languagePreference;
    private String titleText;
    private String protocolPreference;
    private String ipAddressPreference;
    private String portPreference;
    private String usernamePreference;
    private String passwordPreference;
    private String sttItemNamePreference;
    private String baseURL;
    private String result;
    private String errorTitle;
    private String updateItemMessage;
    private String updateItemMessageError;
    private String createNewItemMessage;
    private String createNewItemMessageError;
    private String jsonParseError;
    private String urlNotReachableError;
    private String establishingConnectionError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        languagePreference = sharedPreferences.getString("language_preference", "en");
        protocolPreference = sharedPreferences.getString("protocol_preference", "http");
        ipAddressPreference = sharedPreferences.getString("ip_address_preference", "192.168.0.1");
        portPreference = sharedPreferences.getString("port_preference", "8080");
        usernamePreference = sharedPreferences.getString("username_preference", "openhab");
        passwordPreference = sharedPreferences.getString("password_preference", "habopen");
        sttItemNamePreference = sharedPreferences.getString("stt_item_name_preference", "openHAB_SpeechRecognizer_STTBuffer");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TextView tvResult = findViewById(R.id.tv_result);

        this.result = getIntent().getStringExtra("result");
        tvResult.setText(result);

        ImageView settingsIcon = findViewById(R.id.settings_icon);
        settingsIcon.setOnClickListener(this::openSettings);
        settingsIcon.setColorFilter(ContextCompat.getColor(this, android.R.color.white));

        if (checkBaseUrlAndCreateCRUD()) {
            SendCommandAsyncTask sendCommandTask = new SendCommandAsyncTask();
            sendCommandTask.execute();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        languagePreference = sharedPreferences.getString("language_preference", "en");
        protocolPreference = sharedPreferences.getString("protocol_preference", "http");
        ipAddressPreference = sharedPreferences.getString("ip_address_preference", "192.168.0.1");
        portPreference = sharedPreferences.getString("port_preference", "8080");
        usernamePreference = sharedPreferences.getString("username_preference", "openhab");
        passwordPreference = sharedPreferences.getString("password_preference", "habopen");
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


    private boolean checkBaseUrlAndCreateCRUD() {
        setBaseURL();
        return true;
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
            updateItemMessage = getString(R.string.toast_item_updated_de);
            updateItemMessageError = getString(R.string.toast_item_update_error_de);
            createNewItemMessage = getString(R.string.toast_new_item_created_de);
            createNewItemMessageError = getString(R.string.toast_new_item_creation_error_de);
            jsonParseError = getString(R.string.json_parse_error_de);
            urlNotReachableError = getString(R.string.url_not_reachable_de);
            establishingConnectionError = getString(R.string.establishing_connection_error_de);
        } else if (languagePreference.equals("en")) {
            titleText = getString(R.string.activity_result_title_text_en);
            updateItemMessage = getString(R.string.toast_item_updated_en);
            updateItemMessageError = getString(R.string.toast_item_update_error_en);
            createNewItemMessage = getString(R.string.toast_new_item_created_en);
            createNewItemMessageError = getString(R.string.toast_new_item_creation_error_en);
            jsonParseError = getString(R.string.json_parse_error_en);
            urlNotReachableError = getString(R.string.url_not_reachable_en);
            establishingConnectionError = getString(R.string.establishing_connection_error_en);
        }
    }

    private void setBaseURL() {
        baseURL = String.format("%s://%s:%s", protocolPreference, ipAddressPreference, portPreference);
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

    private class SendCommandAsyncTask extends AsyncTask<Void, Void, Boolean> {
        private boolean success;
        private String errorMessage;

        @Override
        protected Boolean doInBackground(Void... voids) {
            if (baseURL != null && !baseURL.isEmpty()) {
                Log.d("doInBackground","baseURL is not null.");
                try {
                    URL urlCheck = new URL(baseURL);
                    HttpURLConnection connection = (HttpURLConnection) urlCheck.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(5000);

                    int responseCode = connection.getResponseCode();
                    if (responseCode == 200) {
                        Log.d("doInBackground","CRUD was created");
                        if (!usernamePreference.isEmpty() && !passwordPreference.isEmpty()) {
                            crud = new CRUD(baseURL, usernamePreference, passwordPreference);
                        } else {
                            crud = new CRUD(baseURL);
                        }
                    } else {
                        errorMessage = urlNotReachableError + responseCode;
                        return false;
                    }
                } catch (IOException e) {
                    errorMessage = establishingConnectionError + e.getMessage();
                    return false;
                }
            }

            if (crud != null) {
                JSONObject item = null;
                JSONObject errorObject = null;
                String message = null;
                int httpCode = 0;

                try {
                    String itemJson = crud.readItem(sttItemNamePreference);
                    Log.d("ReadItem", "Returned JSON: " + itemJson);
                    item = new JSONObject(itemJson);
                } catch (JSONException e) {
                    Log.d("Create new Item", jsonParseError + e.getMessage());
                    if (usernamePreference.isEmpty() && passwordPreference.isEmpty()) {
                        errorMessage = createNewItemMessageError;
                        return false;
                    } else {
                        crud.createItem("String", sttItemNamePreference);
                        errorMessage = createNewItemMessage;
                    }
                }

                if (sttItemNamePreference != null && !sttItemNamePreference.isEmpty() && result != null && !result.isEmpty()) {
                    crud.sendCommand(sttItemNamePreference, result);
                    errorMessage = updateItemMessage;
                    success = true;
                } else {
                    errorMessage = updateItemMessageError;
                    success = false;
                }

                return success;
            }

            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);

            if (result) {
                showToastMessage(errorMessage);
            } else {
                showErrorDialog(errorMessage);
            }
        }
    }
}
