package com.ottsz.stationpublicity.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.ottsz.stationpublicity.R;
import com.ottsz.stationpublicity.bean.EventMsg;
import com.ottsz.stationpublicity.bean.Resource;
import com.ottsz.stationpublicity.constant.ApkInfo;
import com.ottsz.stationpublicity.constant.EventTag;
import com.ottsz.stationpublicity.util.LogUtils;
import com.ottsz.stationpublicity.util.Utils;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.util.List;

import xyz.doikki.videoplayer.player.BaseVideoView;
import xyz.doikki.videoplayer.player.VideoView;
import xyz.doikki.videoplayer.util.L;

public class ViewPagerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final String TAG = "ViewPagerAdapter";
    private final Context mContext;
    private final List<Resource> resourceList;
    public static final int VIEW_TYPE_IMAGE = 1;
    public static final int VIEW_TYPE_VIDEO = 2;
    private VideoView mVideoView, lastVideoView;

    public ViewPagerAdapter(Context mContext, List<Resource> resourceList) {
        this.mContext = mContext;
        this.resourceList = resourceList;
        mVideoView = new VideoView(mContext);
        mVideoView.addOnStateChangeListener(onStateChangeListener);
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
            viewHolder.container = view.findViewById(R.id.container);
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

    public void startPlay(ViewPager2 viewPager, int position) {
        if (resourceList == null || resourceList.size() == 0) {
            return;
        }
        //ViewPage2内部是通过RecyclerView去实现的，它位于ViewPager2的第0个位置
        RecyclerView mViewPagerImpl = (RecyclerView) viewPager.getChildAt(0);
        int count = mViewPagerImpl.getChildCount();
        LogUtils.d(TAG, "展示资源：" + resourceList.get(position).getLocalName());
        LogUtils.d(TAG, "mViewPagerImpl的item个数：" + count);
//        if (count == 0) {
//            if (resourceList.get(position).getType() == 1) {
//                // 当资源数量大于1个时，发送播放图片的通知
//                if (resourceList.size() > 1) {
//                    EventMsg msg = new EventMsg();
//                    msg.setTag(EventTag.START_IMAGE);
//                    EventBus.getDefault().post(msg);
//                }
//            } else {
//                // 发送播放视频的通知
//                EventMsg msg = new EventMsg();
//                msg.setTag(EventTag.START_VIDEO);
//                EventBus.getDefault().post(msg);
//            }
//        } else {
        // 循环获取ViewHolder并设置
        for (int i = 0; i < resourceList.size(); i++) {
            View itemView = mViewPagerImpl.getChildAt(i);
            if (itemView != null && itemView.getTag() instanceof ViewPagerAdapter.VideoViewHolder) {
                ViewPagerAdapter.VideoViewHolder viewHolder = (ViewPagerAdapter.VideoViewHolder) itemView.getTag();
                // 先移除VideoView
                mVideoView.release();
                Utils.removeViewFormParent(mVideoView);
                // 如果是当前显示的ViewHolder
                if (viewHolder.mPosition == position % resourceList.size()) {
                    Resource resource = resourceList.get(position);
                    File file = new File(ApkInfo.APP_ROOT_PATH + ApkInfo.DOWNLOAD_DIR, resource.getLocalName());
                    LogUtils.d(TAG, "当前是VideoViewHolder，position匹配，展示视频：" + resourceList.get(position).getLocalName());
                    mVideoView.setLooping(resourceList.size() == 1);
                    mVideoView.setUrl(file.getAbsolutePath());
                    viewHolder.container.addView(mVideoView, 0);
                    mVideoView.start();
                    // 发送播放视频的通知
                    EventMsg msg = new EventMsg();
                    msg.setTag(EventTag.START_VIDEO);
                    EventBus.getDefault().post(msg);
                    break;
                } else {
                    LogUtils.d(TAG, "当前是VideoViewHolder，position不匹配");
                }
            } else if (itemView != null && itemView.getTag() instanceof ViewPagerAdapter.ImageViewHolder) {
                ViewPagerAdapter.ImageViewHolder viewHolder = (ViewPagerAdapter.ImageViewHolder) itemView.getTag();
                if (viewHolder.mPosition == position % resourceList.size()) {
                    LogUtils.d(TAG, "当前是ImageViewHolder，position匹配，展示图片：" + resourceList.get(position).getLocalName());
                    // 当资源数量大于1个时，发送播放图片的通知
                    if (resourceList.size() > 1) {
                        EventMsg msg = new EventMsg();
                        msg.setTag(EventTag.START_IMAGE);
                        EventBus.getDefault().post(msg);
                    }
                } else {
                    LogUtils.d(TAG, "当前是ImageViewHolder，position不匹配");
                }
            } else {
                LogUtils.d(TAG, "itemView为null");
            }
//            }
        }
    }

    private final BaseVideoView.OnStateChangeListener onStateChangeListener = new BaseVideoView.OnStateChangeListener() {
        @Override
        public void onPlayerStateChanged(int playerState) {

        }

        @Override
        public void onPlayStateChanged(int playState) {
            if (playState == VideoView.STATE_PLAYBACK_COMPLETED) {
                if (getItemCount() > 1) {
                    EventMsg msg = new EventMsg();
                    msg.setTag(EventTag.STOP_VIDEO);
                    EventBus.getDefault().post(msg);
                }
//                mVideoView.release();
//                // 视频播放结束
//                // 当视频没有暂停，切资源大于1个时，滚动到下一页
//                if (!timeTaskService.isPause) {
//                    if (resourceList.size() > 1) {
//                        // 如果当前没有暂停，则播放完毕后，播放下一个资源
//                        currentPosition++;
//                        viewPager.setCurrentItem(currentPosition, true);
//                    } else {
//                        viewPager.setCurrentItem(currentPosition, true);
//                    }
//                }
            }
        }
    };

    @Override
    public int getItemCount() {
        if (resourceList == null || resourceList.size() == 0) {
            return 0;
        }
        if (resourceList.size() == 1) {
            return 1;
        }
        return Integer.MAX_VALUE;
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
        public FrameLayout container;

        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            itemView.setTag(this);
        }
    }

}