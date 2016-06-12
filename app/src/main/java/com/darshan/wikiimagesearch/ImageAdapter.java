package com.darshan.wikiimagesearch;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Darshan on 12-06-2016.
 */
public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.MyViewHolder> {

    ArrayList<MainActivity.TitleImage> mTitleImages;
    ImageLoader mImageLoader;

    ImageAdapter(Context context , ArrayList<MainActivity.TitleImage> titleImages){
        mTitleImages = titleImages;
        mImageLoader = new ImageLoader(context);
    }

    ImageLoader getImageLoader() {
        return mImageLoader;
    }


    class MyViewHolder extends RecyclerView.ViewHolder{
        TextView mTitle;
        ImageView mImageView;
        public MyViewHolder(View itemView) {
            super(itemView);
            mTitle = (TextView) itemView.findViewById(R.id.title);
            mImageView = (ImageView) itemView.findViewById(R.id.image);
        }
    }
    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.image_item,parent,false);
        MyViewHolder myViewHolder = new MyViewHolder(v);
        return myViewHolder;
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        String title = mTitleImages.get(position).title;
        String imageUrl = mTitleImages.get(position).imageUrl;
        holder.mTitle.setText(title);
        if(imageUrl != null){
            mImageLoader.loadimage(imageUrl,holder.mImageView);
        } else {
            holder.mImageView.setImageResource(R.drawable.notavailble);
        }
    }



    @Override
    public int getItemCount() {
        return mTitleImages.size();
    }
}
