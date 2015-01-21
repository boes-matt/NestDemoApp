package com.boes.nestdemoapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.firebase.client.AuthData;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.util.Map;

public class MainActivity extends ActionBarActivity implements ValueEventListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_TOKEN = 1;
    private Firebase mRef;

    private TextView tvCurrentTemp;
    private TextView tvTargetTemp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvCurrentTemp = (TextView) findViewById(R.id.tvCurrentTemp);
        tvTargetTemp = (TextView) findViewById(R.id.tvTargetTemp);

        Firebase.setAndroidContext(this);
        mRef = new Firebase("https://developer-api.nest.com");

        String token = getToken();
        if (token != null) authenticate(token);
        else obtainToken();
    }

    private String getToken() {
        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        return prefs.getString("token", null);
    }

    private void authenticate(String token) {
        mRef.authWithCustomToken(token, new Firebase.AuthResultHandler() {
            @Override
            public void onAuthenticated(AuthData authData) {
                mRef.child("devices/thermostats").addValueEventListener(MainActivity.this);
            }

            @Override
            public void onAuthenticationError(FirebaseError firebaseError) {
                Log.e(TAG, firebaseError.getMessage(), firebaseError.toException());
            }
        });
    }

    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {
        Log.d(TAG, dataSnapshot.getValue().toString());
        for (DataSnapshot child : dataSnapshot.getChildren()) {
            Map<String, Object> thermostat = (Map<String, Object>) child.getValue();
            tvCurrentTemp.setText(thermostat.get("ambient_temperature_f") + "");
            tvTargetTemp.setText("Target is " + thermostat.get("target_temperature_f"));
            break;
        }
    }

    @Override
    public void onCancelled(FirebaseError firebaseError) {
        Log.e(TAG, firebaseError.getMessage(), firebaseError.toException());
    }

    private void obtainToken() {
        Intent i = new Intent(this, AuthActivity.class);
        startActivityForResult(i, REQUEST_TOKEN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK || requestCode != REQUEST_TOKEN) return;

        String token = data.getStringExtra("token");
        getSharedPreferences("prefs", MODE_PRIVATE).edit().putString("token", token).commit();
        authenticate(token);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
