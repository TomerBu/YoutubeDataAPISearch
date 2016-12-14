package tomerbu.edu.youtubedataapisearch;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.Scopes;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTubeScopes;
import com.google.api.services.youtube.model.ResourceId;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.Subscription;
import com.google.api.services.youtube.model.SubscriptionSnippet;
import com.google.api.services.youtube.model.Thumbnail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int REQUEST_AUTHORIZATION = 1;
    private static final int REQUEST_ACCOUNT_PICKER = 2;
    private static final int REQUEST_PERMISSION = 3;

    final HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
    public static final String TAG = "TomerBu";
    GoogleAccountCredential credential;
    final JsonFactory jsonFactory = GsonFactory.getDefaultInstance();
    private static final String PREF_ACCOUNT_NAME = "accountName";
    Button btnSearch;
    EditText etSearch;
    String API_KEY = "AIzaSyAZ-IQ3_jlb6cCQ2Bkzre1uBZhRKYxh7sg";
    private String mChosenAccountName = "tomer.bu@gmail.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ArrayList<String> scopes = new ArrayList<>();
        scopes.add(YouTubeScopes.YOUTUBE);//scopes.add("https://www.googleapis.com/auth/youtube");
        scopes.add(YouTubeScopes.YOUTUBE_UPLOAD);
        scopes.add(YouTubeScopes.YOUTUBEPARTNER_CHANNEL_AUDIT);
        scopes.add(Scopes.PROFILE);


        credential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), scopes);
        // set exponential backoff policy
        credential.setBackOff(new ExponentialBackOff());
        credential.setSelectedAccountName(mChosenAccountName);
        btnSearch = (Button) findViewById(R.id.btnSearch);
        btnSearch.setOnClickListener(this);
        etSearch = (EditText) findViewById(R.id.etSearch);


// YouTube client
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                youtube();
            }
        });
        thread.start();


    }

    private void youtube() {
        YouTube youtube = new YouTube.Builder(httpTransport, jsonFactory, new HttpRequestInitializer() {
            public void initialize(HttpRequest request) throws IOException {
            }
        }).setApplicationName("YoutubeDemo").build();

        // Prompt the user to enter a query term.
        String queryTerm = "Hello";

        // Define the API request for retrieving search results.
        try {
            YouTube.Search.List search = youtube.search().list("id,snippet");

            // Set your developer key from the {{ Google Cloud Console }} for
            // non-authenticated requests. See:
            // {{ https://cloud.google.com/console }}
            String apiKey = "AIzaSyBcXsITjWAQI4iNnkxc56nn_wCubSaFoco";
            search.setKey(apiKey);

            search.setQ(queryTerm);

            // Restrict the search results to only include videos. See:
            // https://developers.google.com/youtube/v3/docs/search/list#type
            search.setType("video");

            // To increase efficiency, only retrieve the fields that the
            // application uses.
            search.setFields("items(id/kind,id/videoId,snippet/title,snippet/thumbnails/default/url)");
            search.setMaxResults(25L);

            // Call the API and print results.
            SearchListResponse searchResponse = search.execute();
            List<SearchResult> searchResultList = searchResponse.getItems();
            if (searchResultList != null) {
                prettyPrint(searchResultList.iterator(), queryTerm);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void prettyPrint(Iterator<SearchResult> iteratorSearchResults, String query) {

        System.out.println("\n=============================================================");
        System.out.println(
                "   First " + 25 + " videos for search on \"" + query + "\".");
        System.out.println("=============================================================\n");

        if (!iteratorSearchResults.hasNext()) {
            System.out.println(" There aren't any results for your query.");
        }

        while (iteratorSearchResults.hasNext()) {

            SearchResult singleVideo = iteratorSearchResults.next();
            ResourceId rId = singleVideo.getId();

            // Confirm that the result represents a video. Otherwise, the
            // item will not contain a video ID.
            if (rId.getKind().equals("youtube#video")) {
                Thumbnail thumbnail = singleVideo.getSnippet().getThumbnails().getDefault();

                System.out.println(" Video Id" + rId.getVideoId());
                System.out.println(" Title: " + singleVideo.getSnippet().getTitle());
                System.out.println(" Thumbnail: " + thumbnail.getUrl());
                System.out.println("\n-------------------------------------------------------------\n");
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    protected void onResume() {
        super.onResume();


        new AsyncTask<Void, Void, Void>() {


            @Override
            protected Void doInBackground(Void... voids) {
                addSubscription();
                return null;
            }
        }.execute();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (permissions[0].equals(Manifest.permission.GET_ACCOUNTS) && requestCode == REQUEST_PERMISSION && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            addSubscription();
        }
    }

    void addSubscription() {
// This OAuth 2.0 access scope allows for full read/write access to the
        // authenticated user's account.

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.GET_ACCOUNTS}, REQUEST_PERMISSION);
            return;
        }
        try {
            // Authorize the request.
            //  Credential credential = Auth.authorize(scopes, "addsubscription");
            // This object is used to make YouTube Data API requests.
            YouTube youtube = new YouTube.Builder(httpTransport, jsonFactory, credential).setApplicationName(
                    "YoutubeDemo").build();

            // We get the user selected channel to subscribe.
            // Retrieve the channel ID that the user is subscribing to.
            String channelId = getChannelId();
            System.out.println("You chose " + channelId + " to subscribe.");

            // Create a resourceId that identifies the channel ID.
            ResourceId resourceId = new ResourceId();
            resourceId.setChannelId(channelId);
            resourceId.setKind("youtube#channel");

            // Create a snippet that contains the resourceId.
            SubscriptionSnippet snippet = new SubscriptionSnippet();
            snippet.setResourceId(resourceId);
            snippet.setTitle("Tomer");


            // Create a request to add the subscription and send the request.
            // The request identifies subscription metadata to insert as well
            // as information that the API server should return in its response.
            Subscription subscription = new Subscription();
            subscription.setSnippet(snippet);
            YouTube.Subscriptions.Insert subscriptionInsert =
                    youtube.subscriptions().insert("snippet,contentDetails", subscription);
            Subscription returnedSubscription = subscriptionInsert.execute();

            // Print information from the API response.
            System.out.println("\n================== Returned Subscription ==================\n");
            System.out.println("  - Id: " + returnedSubscription.getId());
            System.out.println("  - Title: " + returnedSubscription.getSnippet().getTitle());

        } catch (GoogleJsonResponseException e) {
            System.err.println("GoogleJsonResponseException code: " + e.getDetails().getCode() + " : "
                    + e.getDetails().getMessage());
            e.printStackTrace();
        } catch (UserRecoverableAuthIOException e) {
            startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
            e.printStackTrace();
        } catch (Throwable t) {
            System.err.println("Throwable: " + t.getMessage());
            t.printStackTrace();
        }
    }

    void auth() {
        startActivityForResult(credential.newChooseAccountIntent(),
                REQUEST_ACCOUNT_PICKER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == Activity.RESULT_OK && data != null
                        && data.getExtras() != null) {
                    String accountName = data.getExtras().getString(
                            AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        mChosenAccountName = accountName;
                        credential.setSelectedAccountName(accountName);
                        Toast.makeText(this, mChosenAccountName, Toast.LENGTH_SHORT).show();
                    }
                }
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.auth:
                auth();
                return true;
            case R.id.action_settings:
                return true;
        }
        //noinspection SimplifiableIfStatement
        return super.onOptionsItemSelected(item);
    }

    //btnSearch Clicked
    @Override
    public void onClick(View view) {
        Editable query = etSearch.getText();
        performYoutubeSearch(query);
    }

    private void performYoutubeSearch(Editable query) {

    }

    public String getChannelId() {
        return "UCtVd0c0tGXuTSbU5d8cSBUg";
    }
}
