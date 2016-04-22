package com.citrix.wrekt.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.citrix.wrekt.R;
import com.citrix.wrekt.data.User;
import com.citrix.wrekt.util.EmailUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ChannelMemberAdapter extends RecyclerView.Adapter<ChannelMemberAdapter.ChannelMemberViewHolder> {

    private Context context;
    private LayoutInflater layoutInflater;
    private List<User> channelMemberList;
    private ChannelMemberActionListener channelMemberActionListener;
    private String adminUid;
    private String myUid;
    private Set<String> friendIdSet;
    private String memberNameMeSuffix;
    private String memberNameAdminSuffix;
    private String memberNameFriendSuffix;
    private int selectedPosition = -1;

    public ChannelMemberAdapter(Context context, List<User> channelMemberList, String adminUid, String myUid,
                                Set<String> friendIdSet, ChannelMemberActionListener channelMemberActionListener) {
        this.context = context;
        layoutInflater = LayoutInflater.from(context);
        Collections.sort(channelMemberList);
        this.channelMemberList = channelMemberList;
        this.adminUid = adminUid;
        this.myUid = myUid;
        this.friendIdSet = friendIdSet;
        this.channelMemberActionListener = channelMemberActionListener;
        memberNameMeSuffix = context.getString(R.string.member_name_me_suffix);
        memberNameAdminSuffix = context.getString(R.string.member_name_admin_suffix);
        memberNameFriendSuffix = context.getString(R.string.member_name_friend_suffix);
    }

    @Override
    public ChannelMemberViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = layoutInflater.inflate(R.layout.layout_channel_member_row, parent, false);
        return new ChannelMemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ChannelMemberViewHolder holder, int position) {
        User channelMember = channelMemberList.get(position);
        String uid = channelMember.getUid();
        String username = channelMember.getUsername();
        String email = channelMember.getUserEmail();
        holder.memberUsernameTextView.setText(getMemberRoleString(uid, username));
        if (myUid.equals(uid)) {
            holder.memberEmailTextView.setText("");
            holder.memberEmailImageButton.setVisibility(View.GONE);
            holder.memberOptionContainer.setVisibility(View.GONE);
            holder.friendOptionContainer.setVisibility(View.GONE);
        } else {
            if (TextUtils.isEmpty(email)) {
                holder.memberEmailTextView.setText(R.string.no_email_text);
                holder.memberEmailImageButton.setVisibility(View.GONE);
            } else {
                holder.memberEmailTextView.setText(channelMember.getUserEmail());
                holder.memberEmailImageButton.setVisibility(View.VISIBLE);
            }
            if (friendIdSet.contains(uid)) {
                holder.memberOptionContainer.setVisibility(View.GONE);
                holder.friendOptionContainer.setVisibility(selectedPosition == position ? View.VISIBLE : View.GONE);
            } else {
                holder.friendOptionContainer.setVisibility(View.GONE);
                holder.memberOptionContainer.setVisibility(selectedPosition == position ? View.VISIBLE : View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return channelMemberList == null ? 0 : channelMemberList.size();
    }

    public void addNewChannelMember(User user) {
        channelMemberList.add(user);
        Collections.sort(channelMemberList);
        notifyDataSetChanged();
    }

    public void clear() {
        channelMemberList.clear();
        notifyDataSetChanged();
    }

    public void updateFriendIdSet(Set<String> friendIdSet) {
        this.friendIdSet = friendIdSet;
    }

    private String getMemberRoleString(String uid, String username) {
        if (myUid.equals(uid)) {
            username = username + " " + memberNameMeSuffix;
        }
        if (adminUid.equals(uid)) {
            username = username + " " + memberNameAdminSuffix;
        }
        if (friendIdSet.contains(uid)) {
            username = username + " " + memberNameFriendSuffix;
        }
        return username;
    }


    public interface ChannelMemberActionListener {

        void onAddFriendClicked(User channelMember);

        void onChatWithFriendClicked(User friend);
    }

    protected class ChannelMemberViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private RelativeLayout memberInfoContainer;
        private TextView memberUsernameTextView;
        private TextView memberEmailTextView;
        private ImageButton memberEmailImageButton;
        private LinearLayout memberOptionContainer;
        private TextView addFriendTextView;
        private LinearLayout friendOptionContainer;
        private TextView chatWithFriendTextView;

        public ChannelMemberViewHolder(View itemView) {
            super(itemView);
            memberInfoContainer = (RelativeLayout) itemView.findViewById(R.id.member_info_container);
            memberUsernameTextView = (TextView) itemView.findViewById(R.id.member_username_text_view);
            memberEmailTextView = (TextView) itemView.findViewById(R.id.member_email_text_view);
            memberEmailImageButton = (ImageButton) itemView.findViewById(R.id.member_email_image_button);
            memberOptionContainer = (LinearLayout) itemView.findViewById(R.id.member_option_container);
            addFriendTextView = (TextView) itemView.findViewById(R.id.add_friend_text_view);
            friendOptionContainer = (LinearLayout) itemView.findViewById(R.id.friend_option_container);
            chatWithFriendTextView = (TextView) itemView.findViewById(R.id.chat_with_friend_text_view);

            memberInfoContainer.setOnClickListener(this);
            memberEmailImageButton.setOnClickListener(this);
            addFriendTextView.setOnClickListener(this);
            chatWithFriendTextView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();
            User channelMember = channelMemberList.get(position);
            switch (v.getId()) {
                case R.id.member_info_container:
                    if (selectedPosition == position) {
                        selectedPosition = -1;
                    } else {
                        selectedPosition = position;
                    }
                    notifyDataSetChanged();
                    break;

                case R.id.member_email_image_button:
                    String[] recipientEmails = {channelMember.getUserEmail()};
                    EmailUtils.sendEmailToRecipients(context, recipientEmails);
                    break;

                case R.id.add_friend_text_view:
                    channelMemberActionListener.onAddFriendClicked(channelMember);
                    break;

                case R.id.chat_with_friend_text_view:
                    channelMemberActionListener.onChatWithFriendClicked(channelMember);
                    break;
            }
        }
    }
}
