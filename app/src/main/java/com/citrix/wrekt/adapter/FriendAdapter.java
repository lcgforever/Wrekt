package com.citrix.wrekt.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.citrix.wrekt.R;
import com.citrix.wrekt.data.User;
import com.citrix.wrekt.util.EmailUtils;

import java.util.Collections;
import java.util.List;

public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.FriendViewHolder> {

    private Context context;
    private LayoutInflater layoutInflater;
    private List<User> friendList;
    private FriendActionListener friendActionListener;

    public FriendAdapter(Context context, List<User> friendList, FriendActionListener friendActionListener) {
        this.context = context;
        layoutInflater = LayoutInflater.from(context);
        Collections.sort(friendList);
        this.friendList = friendList;
        this.friendActionListener = friendActionListener;
    }

    @Override
    public FriendViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = layoutInflater.inflate(R.layout.layout_friend_row, parent, false);
        return new FriendViewHolder(view);
    }

    @Override
    public void onBindViewHolder(FriendViewHolder holder, int position) {
        User friend = friendList.get(position);
        holder.friendUsernameTextView.setText(friend.getUsername());
        String email = friend.getUserEmail();
        if (TextUtils.isEmpty(email)) {
            holder.friendEmailTextView.setText(R.string.no_email_text);
            holder.friendEmailImageButton.setVisibility(View.GONE);
        } else {
            holder.friendEmailTextView.setText(friend.getUserEmail());
            holder.friendEmailImageButton.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return friendList == null ? 0 : friendList.size();
    }

    public void updateFriendList(List<User> friendList) {
        if (friendList != null) {
            this.friendList.clear();
            Collections.sort(friendList);
            this.friendList.addAll(friendList);
            notifyDataSetChanged();
        }
    }


    public interface FriendActionListener {

        void onFriendClicked(User friend);
    }

    protected class FriendViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private RelativeLayout friendInfoContainer;
        private TextView friendUsernameTextView;
        private TextView friendEmailTextView;
        private ImageButton friendEmailImageButton;

        public FriendViewHolder(View itemView) {
            super(itemView);
            friendInfoContainer = (RelativeLayout) itemView.findViewById(R.id.friend_info_container);
            friendUsernameTextView = (TextView) itemView.findViewById(R.id.friend_username_text_view);
            friendEmailTextView = (TextView) itemView.findViewById(R.id.friend_email_text_view);
            friendEmailImageButton = (ImageButton) itemView.findViewById(R.id.friend_email_image_button);

            friendInfoContainer.setOnClickListener(this);
            friendEmailImageButton.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();
            User friend = friendList.get(position);
            switch (v.getId()) {
                case R.id.friend_info_container:
                    friendActionListener.onFriendClicked(friend);
                    break;

                case R.id.friend_email_image_button:
                    String[] recipientEmails = {friend.getUserEmail()};
                    EmailUtils.sendEmailToRecipients(context, recipientEmails);
                    break;
            }
        }
    }
}
