package net.anber.testfacebook;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphUser;
import com.google.gson.Gson;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;

import net.anber.testfacebook.model.FacebookPost;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends ActionBarActivity {

    private static final String TAG = "LOG_TAG";

    //    private static final String URL = "https://www.facebook.com/PRhymeOfficial";
    private static final String URL = "https://www.facebook.com/mahala360";

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

    protected ImageLoader imageLoader;

    private String iconUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        uiHelper = new UiLifecycleHelper(this, null);
        uiHelper.onCreate(savedInstanceState);

        imageLoader = ImageLoader.getInstance();

        DisplayImageOptions defaultOptions = new DisplayImageOptions.Builder()
                .showImageOnLoading(R.drawable.placeholder)
                .imageScaleType(ImageScaleType.IN_SAMPLE_INT)
                .resetViewBeforeLoading(true)
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .build();

        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this)
                .diskCacheSize(50 * 1024 * 1024)
                .defaultDisplayImageOptions(defaultOptions)
                .build();
        imageLoader.init(config);

        adapter = new ArrayAdapter<FacebookPost>(this, 0, items) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.list_item, parent, false);
                }
                FacebookPost item = getItem(position);

                TextView text = (TextView) convertView.findViewById(R.id.text);
                text.setText(item.getMessage());

                ImageView icon = (ImageView) convertView.findViewById(R.id.icon);
                if (!TextUtils.isEmpty(iconUrl)) {
                    icon.setVisibility(View.VISIBLE);
                    DisplayImageOptions iconOptions = new DisplayImageOptions.Builder()
                            //  .displayer(new FadeInBitmapDisplayer(500))
                            .displayer(new RoundedBitmapDisplayer(100))
                            .build();
                    imageLoader.displayImage(iconUrl, icon, iconOptions);
                } else {
                    icon.setVisibility(View.INVISIBLE);
                }

                ImageView image = (ImageView) convertView.findViewById(R.id.image);

                if (TextUtils.isEmpty(item.getPicture()) || item.getType().equalsIgnoreCase("link")) {
                    image.setVisibility(View.GONE);
                } else {
                    image.setVisibility(View.VISIBLE);
                    imageLoader.displayImage(item.getPicture(), image);
                }
                return convertView;
            }
        };
        ListView listView = (ListView) findViewById(R.id.list);
        listView.setAdapter(adapter);
    }

    public void btnMakeFacebookActionClick(View view) {
        Session session = Session.getActiveSession();
        if (session != null && session.isOpened() && !session.isClosed()) {
            getDataFromServer();
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
                            getDataFromServer();
                        }
                    }
                }).executeAsync();
            } else {
                getDataFromServer();
            }
        }
    }

    private void getDataFromServer() {
        final Session session = Session.getActiveSession();
        if (session == null || !session.isOpened()) {
            return;
        }

        new AsyncTask<Void, Void, Void>() {

            private ProgressDialog dialog;

            @Override
            protected void onPreExecute() {
                dialog = ProgressDialog.show(MainActivity.this, "","Loading...", true);
            }

            @Override
            protected Void doInBackground(Void... params) {
                sendRequestForUserIcon(session);
                sendRequestForFeeds(session);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                adapter.notifyDataSetChanged();
                dialog.dismiss();
            }
        }.execute();
    }

    private void processResponse(Response response, final Session session) {
        if (response.getGraphObject() == null) {
            return;
        }
        String str = response.getGraphObject().getProperty("data").toString();
        Gson gson = new Gson();
        FacebookPost[] arr = gson.fromJson(str, FacebookPost[].class);
        items.clear();
        for (final FacebookPost post : arr) {
            if (!TextUtils.isEmpty(post.getMessage())) {
                items.add(post);
                if (post.getType().equalsIgnoreCase("photo") && !TextUtils.isEmpty(post.getObject_id())) {
                    sendRequestForBigPicture(session, post);
                }
            }
        }
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

    private void sendRequestForFeeds(final Session session) {
        new Request(
                session,
                facebookId + "/feed",
                null,
                HttpMethod.GET,
                new Request.Callback() {
                    public void onCompleted(Response response) {
                        processResponse(response, session);
                    }
                }
        ).executeAndWait();
    }

    private void sendRequestForUserIcon(Session session) {
        Bundle bundle = new Bundle();
        bundle.putBoolean("redirect", false);
        bundle.putString("height", "200");
        bundle.putString("type", "normal");
        bundle.putString("width", "200");
        new Request(
                session,
                facebookId + "/picture",
                bundle,
                HttpMethod.GET,
                new Request.Callback() {
                    public void onCompleted(Response response) {
                        if (response.getGraphObject() == null) {
                            return;
                        }
                        String str = response.getGraphObject().getProperty("data").toString();
                        try {
                            JSONObject jo = new JSONObject(str);
                            iconUrl = jo.getString("url");
                        } catch (JSONException e) {
                            Log.i(TAG, "can't get icon", e);
                        }
                    }
                }
        ).executeAndWait();
    }

    private void sendRequestForBigPicture(Session session, final FacebookPost post) {
        new Request(
                session,
                post.getObject_id(),
                null,
                HttpMethod.GET,
                new Request.Callback() {
                    public void onCompleted(Response response) {
                        if (response.getGraphObject() == null) {
                            return;
                        }
                        String str = response.getGraphObject().getProperty("images").toString();
                        try {
                            JSONArray ja = new JSONArray(str);
                            JSONObject jo = new JSONObject(ja.get(0).toString());
                            String url = jo.getString("source");
                            post.setPicture(url);
                        } catch (JSONException e) {
                            Log.i(TAG, "can't get icon", e);
                        }
                    }
                }
        ).executeAndWait();
    }


}
