package com.citrix.wrekt.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.citrix.wrekt.R;
import com.citrix.wrekt.data.Channel;
import com.squareup.picasso.Picasso;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ChannelAdapter extends RecyclerView.Adapter<ChannelAdapter.ChannelViewHolder> {

    private Context context;
    private LayoutInflater layoutInflater;
    private List<Channel> channelList;
    private ChannelClickListener channelClickListener;
    private NameComparator nameComparator;

    public ChannelAdapter(Context context, List<Channel> channelList, ChannelClickListener channelClickListener) {
        this.context = context;
        layoutInflater = LayoutInflater.from(context);
        nameComparator = new NameComparator();
        Collections.sort(channelList, nameComparator);
        this.channelList = channelList;
        this.channelClickListener = channelClickListener;
    }

    @Override
    public ChannelViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = layoutInflater.inflate(R.layout.layout_channel_list_row, parent, false);
        return new ChannelViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ChannelViewHolder holder, int position) {
        Channel channel = channelList.get(position);
        holder.channelNameTextView.setText(channel.getName());
        holder.channelDescriptionTextView.setText(channel.getDescription());
        holder.channelMemberCountTextView.setText(String.valueOf(channel.getMemberCount()));
        Picasso.with(context)
                .load(channel.getImageUrl())
                .placeholder(R.drawable.ic_loading)
                .error(R.drawable.ic_no_image)
                .into(holder.channelImageView);
    }

    @Override
    public int getItemCount() {
        return channelList == null ? 0 : channelList.size();
    }

    public void updateChannelList(List<Channel> channelList) {
        if (channelList != null) {
            this.channelList.clear();
            Collections.sort(channelList, nameComparator);
            this.channelList.addAll(channelList);
            notifyDataSetChanged();
        }
    }


    public interface ChannelClickListener {

        void onChannelClicked(String channelId);
    }

    protected class ChannelViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private ImageView channelImageView;
        private TextView channelNameTextView;
        private TextView channelDescriptionTextView;
        private TextView channelMemberCountTextView;

        public ChannelViewHolder(View view) {
            super(view);

            channelImageView = (ImageView) view.findViewById(R.id.channel_image_view);
            channelNameTextView = (TextView) view.findViewById(R.id.channel_name_text_view);
            channelDescriptionTextView = (TextView) view.findViewById(R.id.channel_description_text_view);
            channelMemberCountTextView = (TextView) view.findViewById(R.id.channel_member_count_text_view);

            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();
            Channel channel = channelList.get(position);
            channelClickListener.onChannelClicked(channel.getId());
        }
    }

    private class NameComparator implements Comparator<Channel> {

        @Override
        public int compare(Channel first, Channel second) {
            return first.getName().compareTo(second.getName());
        }
    }
}
