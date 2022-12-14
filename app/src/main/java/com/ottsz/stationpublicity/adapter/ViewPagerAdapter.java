package com.ottsz.stationpublicity.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.ottsz.stationpublicity.R;
import com.ottsz.stationpublicity.bean.Resource;
import com.ottsz.stationpublicity.constant.ApkInfo;

import java.io.File;
import java.util.List;

public class ViewPagerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context mContext;
    private final List<Resource> resourceList;
    public static final int VIEW_TYPE_IMAGE = 1;
    public static final int VIEW_TYPE_VIDEO = 2;

    public ViewPagerAdapter(Context mContext, List<Resource> resourceList) {
        this.mContext = mContext;
        this.resourceList = resourceList;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_IMAGE) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image, parent, false);
            ImageViewHolder viewHolder = new ImageViewHolder(view);
            viewHolder.imageView = view.findViewById(R.id.image);
            return viewHolder;
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_video, parent, false);
            VideoViewHolder viewHolder = new VideoViewHolder(view);
            viewHolder.mPlayerContainer = view.findViewById(R.id.container);
            return viewHolder;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (resourceList.size() > 0) {
            Resource resource = resourceList.get(position % resourceList.size());
            File file = new File(ApkInfo.APP_ROOT_PATH + ApkInfo.DOWNLOAD_DIR, resource.getLocalName());
            if (holder instanceof ImageViewHolder) {
                ImageViewHolder viewHolder = (ImageViewHolder) holder;
                Glide.with(mContext)
                        .load(file)
                        .error(R.drawable.error)
                        .override(1920, 1080)
                        .into(viewHolder.imageView);
                viewHolder.mPosition = position % resourceList.size();
            } else {
                VideoViewHolder viewHolder = (VideoViewHolder) holder;
                viewHolder.mPosition = position % resourceList.size();
            }
        }
    }

    @Override
    public int getItemCount() {
        return resourceList != null ? Integer.MAX_VALUE : 0;
    }

    @Override
    public int getItemViewType(int position) {
        if (resourceList.size() > 0) {
            if (resourceList.get(position % resourceList.size()).getType() == 1) {
                return VIEW_TYPE_IMAGE;
            } else {
                return VIEW_TYPE_VIDEO;
            }
        } else {
            return VIEW_TYPE_IMAGE;
        }
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder {

        public int mPosition;
        private AppCompatImageView imageView;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            itemView.setTag(this);
        }
    }

    public static class VideoViewHolder extends RecyclerView.ViewHolder {

        public int mPosition;
        public FrameLayout mPlayerContainer;

        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            itemView.setTag(this);
        }
    }

}