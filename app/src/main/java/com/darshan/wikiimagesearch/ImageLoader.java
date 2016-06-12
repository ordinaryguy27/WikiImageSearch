package com.darshan.wikiimagesearch;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.util.LruCache;
import android.util.TypedValue;
import android.widget.ImageView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Darshan on 11-06-2016.
 */
public class ImageLoader {

    ExecutorService ex = Executors.newFixedThreadPool(10);

    ArrayList<BitmapGetter> bitmapGetters = new ArrayList<>();
    LruCache<String,Bitmap> memoryCache;
    private Map<ImageView, String> imageViews = Collections
            .synchronizedMap(new WeakHashMap<ImageView, String>());
    DiskLruImageCache mDiskLruImageCache;
    final String THUMBNAIL = "thumbnail";

    ImageLoader(Context context){
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 8;
        memoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // The cache size will be measured in kilobytes rather than
                // number of items.
                return bitmap.getByteCount() / 1024;
            }
        };
        mDiskLruImageCache = new DiskLruImageCache(context,THUMBNAIL,10*1024*1024,
                Bitmap.CompressFormat.JPEG, 100 );
    }
    boolean imageViewReused(ImageView im,String url) {
        String tag = imageViews.get(im);
        if (tag == null || !tag.equals(url))
            return true;
        return false;
    }

    class BitmapGetter extends AsyncTask<String,String,Bitmap> {
        WeakReference<ImageView> weakImageView;
        String mUrl;

        BitmapGetter(WeakReference<ImageView> im){
            weakImageView = im;
        }
        String replaceSpecialchar(String key){
            return key.replaceAll("[^a-zA-Z0-9 ]", "");
        }
        @Override
        protected Bitmap doInBackground(String... param) {
            try {
                mUrl = param[0];
                if(mDiskLruImageCache.containsKey(replaceSpecialchar(mUrl))){
                    Bitmap b = mDiskLruImageCache.getBitmap(replaceSpecialchar(mUrl));
                    if(b!=null)
                        return b;
                }
                URL url = new URL(mUrl);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setDoInput(true);
                con.setConnectTimeout(30000);
                con.setReadTimeout(30000);
                con.setInstanceFollowRedirects(true);
                int response = con.getResponseCode();
                if(response == HttpURLConnection.HTTP_OK) {
                    InputStream in = con.getInputStream();
                    Bitmap b = decode(in);
                    if(b != null){
                        mDiskLruImageCache.put(replaceSpecialchar(mUrl),b);
                    }
                    return b;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        Bitmap decode(InputStream in){
            /* No need to scale bitmap as bitmap given by wikipedia is always les than or equal to 250dp
                as set in wiki url params */
            return BitmapFactory.decodeStream(in);
        }
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            ImageView im = weakImageView.get();
            if(im == null)
                return;
            if(bitmap == null){
                im.setImageResource(R.drawable.notavailble);
            } else if(!imageViewReused(im,mUrl)) {
                memoryCache.put(mUrl,bitmap);
                im.setImageBitmap(bitmap);
            }
            bitmapGetters.remove(this);
        }
    }


    void loadimage(String url, ImageView img){
        imageViews.put(img, url);
        Bitmap b = memoryCache.get(url);
        if(b!= null){
            img.setImageBitmap(b);
        }
        else {
            img.setImageResource(R.drawable.loading);
            WeakReference<ImageView> wimg = new WeakReference<ImageView>(img);
            BitmapGetter bg = new BitmapGetter(wimg);
            bg.executeOnExecutor(ex,url);
            bitmapGetters.add(bg);
        }
    }
    void cancelAll(){
        for(int i = 0;i<bitmapGetters.size();i++) {
            bitmapGetters.get(i).cancel(false);
        }
        bitmapGetters.clear();
    }
}
