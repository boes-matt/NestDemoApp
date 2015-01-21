package com.boes.nestdemoapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Random;

import javax.net.ssl.HttpsURLConnection;

public class AuthActivity extends ActionBarActivity {

    private static final String TAG = AuthActivity.class.getSimpleName();

    private static final String BASE_AUTHORIZATION_URL = "https://home.nest.com/";
    private static final String BASE_ACCESS_TOKEN_URL = "https://api.home.nest.com/";

    private static final String CLIENT_CODE_URL = BASE_AUTHORIZATION_URL + "login/oauth2?client_id=%s&state=%s";
    private static final String ACCESS_URL = BASE_ACCESS_TOKEN_URL + "oauth2/access_token?client_id=%s&code=%s&client_secret=%s&grant_type=authorization_code";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WebView webView = new WebView(this);
        setContentView(webView);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // Intercept redirect and capture code; Use code to request token
                if (!url.startsWith(Keys.REDIRECT_URL)) return false;

                String code = parseCode(url);
                if (code == null) {
                    setResult(RESULT_CANCELED);
                    finish();
                } else {
                    requestToken(code);
                }

                return true;
            }
        });

        webView.getSettings().setJavaScriptEnabled(true);
        String state = "app-state" + "-" + System.nanoTime() + "-" + new Random().nextInt();
        String url = String.format(CLIENT_CODE_URL, Keys.CLIENT_ID, state);
        webView.loadUrl(url);
    }

    private String parseCode(String url) {
        Uri uri = Uri.parse(url);
        return uri.getQueryParameter("code");
    }

    private void requestToken(String code) {
        new AsyncTask<String, Void, String>() {
            @Override
            protected String doInBackground(String... params) {
                String urlString = String.format(ACCESS_URL, Keys.CLIENT_ID, params[0], Keys.CLIENT_SECRET);
                HttpsURLConnection conn = null;

                try {
                    URL url = new URL(urlString);
                    conn = (HttpsURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    InputStream in = new BufferedInputStream(conn.getInputStream());
                    JSONObject result = new JSONObject(readStream(in));
                    return result.getString("access_token");
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage(), e);
                } finally {
                    if (conn != null) conn.disconnect();
                }

                return null;
            }

            @Override
            protected void onPostExecute(String token) {
                if (token == null) {
                    setResult(RESULT_CANCELED);
                    finish();
                }

                Intent data = new Intent();
                data.putExtra("token", token);
                setResult(RESULT_OK, data);
                finish();
            }
        }.execute(code);
    }

    private String readStream(InputStream in) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final byte[] buffer = new byte[4096];

        int read;
        while ((read = in.read(buffer, 0, buffer.length)) != -1) {
            out.write(buffer, 0, read);
        }

        final byte[] data = out.toByteArray();
        return new String(data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_auth, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
