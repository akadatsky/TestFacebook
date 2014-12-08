package net.anber.testfacebook;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphUser;
import com.google.gson.Gson;

import net.anber.testfacebook.model.FacebookPost;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class MainActivity extends ActionBarActivity {

    private static final String TAG = "LOG_TAG";
    private static final String URL = "https://www.facebook.com/PRhymeOfficial";
    private String facebookId = URL.substring(URL.lastIndexOf('/') + 1);

    private UiLifecycleHelper uiHelper;
    private String facebookUserName;

    private Session.StatusCallback callback = new Session.StatusCallback() {
        @Override
        public void call(Session session, SessionState state, Exception exception) {
            onSessionStateChange(session, state, exception);
        }
    };
    private List<FacebookPost> items = new ArrayList<>();
    private ArrayAdapter<FacebookPost> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        uiHelper = new UiLifecycleHelper(this, null);
        uiHelper.onCreate(savedInstanceState);

        adapter = new ArrayAdapter<FacebookPost>(this, android.R.layout.simple_list_item_1, items);
        ListView listView = (ListView) findViewById(R.id.list);
        listView.setAdapter(adapter);

    }

    public void btnLoginClick(View view) {
        Session session = Session.getActiveSession();
        if (session != null && session.isOpened() && !session.isClosed()) {
            publishStory();
        } else {
            Session.openActiveSession(this, true, callback);
        }

    }

    private void onSessionStateChange(Session session, SessionState state, Exception exception) {

        if (exception != null) {
            if (session != null) {
                try {
                    session.closeAndClearTokenInformation();
                } catch (Exception e) {
                    Log.e(TAG, "onSessionFBStateChange", e);
                }
            }
            return;
        }

        if (state.isOpened()) {
            if (facebookUserName == null) {
                Request.newMeRequest(session, new Request.GraphUserCallback() {
                    @Override
                    public void onCompleted(GraphUser user, Response response) {
                        if (user != null) {
                            facebookUserName = user.getName();
                            publishStory();
                        }
                    }
                }).executeAsync();
            } else {
                publishStory();
            }
        }
    }

    private void publishStory() {
        Session session = Session.getActiveSession();
        if (session == null || !session.isOpened()) {
            return;
        }
        new Request(
                session,
                facebookId + "/links",
                null,
                HttpMethod.GET,
                new Request.Callback() {
                    public void onCompleted(Response response) {
                        processResponse(response);
                    }
                }
        ).executeAsync();
    }

    private void processResponse(Response response) {
        String str = response.getGraphObject().getProperty("data").toString();
        Gson gson = new Gson();
        FacebookPost[] arr = gson.fromJson(str, FacebookPost[].class);
        Log.i(TAG, "Size: " + arr.length);
        items.clear();
        items.addAll(Arrays.asList(arr));
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
        uiHelper.onResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        uiHelper.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onPause() {
        super.onPause();
        uiHelper.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        uiHelper.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        uiHelper.onSaveInstanceState(outState);
    }


}
