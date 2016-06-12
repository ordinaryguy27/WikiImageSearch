package com.darshan.wikiimagesearch;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;

public class MainActivity extends Activity {
    ArrayList<TitleImage> mTitleImages = new ArrayList<>();
    ImageAdapter mImageAdapter;
    ProgressDialog mProgressDialog;
    RecyclerView mRecyclerView;
    int REQUIRED_SIZE = (int)(250 * Resources.getSystem().getDisplayMetrics().density);
    private final String WIKIURL = "https://en.wikipedia.org/w/api.php?" +
            "action=query&prop=pageimages&format=json&piprop=thumbnail&pithumbsize=%d&" +
            "pilimit=50&generator=prefixsearch&gpslimit=50&gpssearch=%s";
    ImageAsync mImageAsync;
    Handler mHandler = new Handler();
    TextView mNoShowTextView;
    final int THIRTY_SEC_TIMEOUT = 30000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        RecyclerView recycleView = (RecyclerView) findViewById(R.id.recycleview);
        recycleView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        mRecyclerView = recycleView;
        mRecyclerView.addItemDecoration(new DividerItemDecorator(this));
        mImageAdapter = new ImageAdapter(this,mTitleImages);
        recycleView.setAdapter(mImageAdapter);
        mNoShowTextView = (TextView) findViewById(R.id.no_result);
        mProgressDialog = new ProgressDialog(MainActivity.this);
        mProgressDialog.setTitle("Please wait");
        mProgressDialog.setMessage("Loading");
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public void buttonpressed(View v){
        EditText searchbox = ((EditText)findViewById(R.id.editText2));
        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(searchbox.getWindowToken(), 0);
        if(!isNetworkAvailable()){
            Toast.makeText(this,R.string.connect_to_internet,Toast.LENGTH_LONG).show();
            setNoshowLayout(true);
            return;
        }
        if(mImageAsync != null){
            mImageAsync.cancel(false);
        }
        mImageAsync = new ImageAsync();
        mImageAsync.execute(searchbox.getText().toString());
    }
    class TitleImage{
        String title;
        String imageUrl;
        TitleImage(String title, String imageUrl){
            this.title = title;
            this.imageUrl = imageUrl;
        }
    }
    void setNoshowLayout(boolean show){
            mRecyclerView.setVisibility(show?View.GONE:View.VISIBLE);
            mNoShowTextView.setVisibility(show?View.VISIBLE:View.GONE);
    }
    Runnable mTimeout = new Runnable() {
        @Override
        public void run() {
            if(mImageAsync != null){
                mImageAsync.cancel(false);
            }
            if(mProgressDialog != null && mProgressDialog.isShowing()){
                mProgressDialog.dismiss();
            }
            setNoshowLayout(true);
            Toast.makeText(MainActivity.this,R.string.timeout,Toast.LENGTH_LONG).show();
        }
    };

    class ImageAsync extends AsyncTask<String,String,ArrayList<TitleImage>>{
        boolean isError = false;
        @Override
        protected void onPreExecute() {
            mHandler.removeCallbacks(mTimeout);
            mHandler.postDelayed(mTimeout,THIRTY_SEC_TIMEOUT);
            mProgressDialog.show();
            mTitleImages.clear();
            mImageAdapter.getImageLoader().cancelAll();
        }

        @Override
        protected ArrayList<TitleImage> doInBackground(String... params) {
            String s = String.format(WIKIURL,REQUIRED_SIZE,params[0]);
            String response ="";
            try {
                URL url = new URL(s);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.connect();
                int responsecode = urlConnection.getResponseCode();
                if(responsecode == HttpURLConnection.HTTP_OK){
                    String line;
                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(urlConnection.getInputStream()));
                    while ((line = br.readLine()) != null) {
                        response += line;
                    }
                }
                else {
                    isError = true;
                    return null;
                }
            } catch (Exception e) {
                isError = true;
                e.printStackTrace();
            }
            return getfromjson(response);

        }

        ArrayList<TitleImage> getfromjson(String jsonstring){
            try {
                JSONObject mainjson = new JSONObject(jsonstring);
                JSONObject query = mainjson.getJSONObject("query");
                JSONObject pages = query.getJSONObject("pages");
                Iterator<String> it = pages.keys();
                ArrayList<TitleImage> ar = new ArrayList<>();
                while(it.hasNext()){
                    JSONObject currentitem = pages.getJSONObject(it.next());
                    String title = currentitem.optString("title");
                    JSONObject thumnail = currentitem.optJSONObject("thumbnail");
                    if(thumnail!= null){
                        String imageurl = thumnail.getString("source");
                        TitleImage ti = new TitleImage(title,imageurl);
                        ar.add(ti);
                    } else {
                        TitleImage ti = new TitleImage(title,null);
                        ar.add(ti);
                    }

                }
                return ar;

            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(ArrayList<TitleImage> s) {
            mHandler.removeCallbacks(mTimeout);
            mProgressDialog.dismiss();
            if(s == null){
                if(isError){
                    Toast.makeText(MainActivity.this,R.string.error_occurred,Toast.LENGTH_LONG)
                            .show();
                }
                setNoshowLayout(true);
                mImageAdapter.notifyDataSetChanged();
                return;
            }
            setNoshowLayout(false);
            mTitleImages.addAll(s);
            mImageAdapter.notifyDataSetChanged();
        }
    }

}
