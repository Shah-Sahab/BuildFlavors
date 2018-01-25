package com.udacity.gradle.builditbigger;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.test.espresso.idling.CountingIdlingResource;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.example.JokeSmith;
import com.example.psych.myapplication.backend.myApi.MyApi;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.services.AbstractGoogleClientRequest;
import com.google.api.client.googleapis.services.GoogleClientRequestInitializer;
import com.joketellerlibrary.JokeTellerActivity;

import java.io.IOException;


public class MainActivity extends AppCompatActivity {

    private ProgressDialog mProgressDialog;
    private CountingIdlingResource mIdlingResource;
    private String mResult;

    private final String idlingResourceName = "AsyncTask Idling Resource";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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

    public void tellJoke(View view) {
        showPleaseWaitProgressDialog();
        incrementIdlingResource();
        new EndpointsAsyncTask().execute(getApplicationContext());
    }

    class EndpointsAsyncTask extends AsyncTask<Context, Void, String> {
        private MyApi myApiService = null;
        private Context context;

        @Override
        protected String doInBackground(Context... params) {
            if (myApiService == null) {  // Only do this once
                MyApi.Builder builder = new MyApi.Builder(AndroidHttp.newCompatibleTransport(),
                                new AndroidJsonFactory(), null)
                                // options for running against local devappserver
                                // - 10.0.2.2 is localhost's IP address in Android emulator
                                // - turn off compression when running against local devappserver
                                .setRootUrl("http://10.0.2.2:8080/_ah/api/")
                                .setGoogleClientRequestInitializer(new GoogleClientRequestInitializer() {
                                    @Override
                                    public void initialize(AbstractGoogleClientRequest<?> abstractGoogleClientRequest) throws IOException {
                                        abstractGoogleClientRequest.setDisableGZipContent(true);
                                    }
                                });
                // end options for devappserver

                myApiService = builder.build();
            }

            context = params[0];

            try {
                return myApiService.getJokes().execute().getData();
            } catch (IOException e) {
                return e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            dismissProgressDialog();
            showJokeTellerActivity(result);
        }

    }

    private void showJokeTellerActivity(String result) {
        mResult = result;
        decrementIdlingResource();
        Toast.makeText(getApplicationContext(), mResult, Toast.LENGTH_LONG).show();
        Intent intent = new Intent(MainActivity.this, JokeTellerActivity.class);
        intent.putExtra(Intent.EXTRA_REFERRER, new JokeSmith().getJoke());
        startActivity(intent);
    }

    private void showPleaseWaitProgressDialog() {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle(getString(R.string.loading_string));
        mProgressDialog.setMessage(getString(R.string.please_wait_string));
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
    }

    private void dismissProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    private void incrementIdlingResource() {
        if (mIdlingResource != null) {
            mIdlingResource.increment();
        }
    }

    private void decrementIdlingResource() {
        if (mIdlingResource != null) {
            mIdlingResource.decrement();
        }
    }

    /**
     * Only called from test, creates and returns a new {@link CountingIdlingResource}.
     */
    @VisibleForTesting
    @NonNull
    public CountingIdlingResource getIdlingResource() {
        if (mIdlingResource == null) {
            mIdlingResource = new CountingIdlingResource(idlingResourceName);
        }
        return mIdlingResource;
    }

    @VisibleForTesting
    public String getResult() {
        return mResult;
    }
}
