package com.citrix.wrekt.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.citrix.wrekt.R;
import com.citrix.wrekt.data.PendingFriendRequest;

import java.util.Collections;
import java.util.List;

public class PendingFriendRequestAdapter extends RecyclerView.Adapter<PendingFriendRequestAdapter.PendingRequestViewHolder> {

    private LayoutInflater layoutInflater;
    private List<PendingFriendRequest> pendingRequestList;
    private PendingRequestActionListener pendingRequestActionListener;
    private String myUid;
    private int selectedPosition = -1;

    public PendingFriendRequestAdapter(Context context, List<PendingFriendRequest> pendingRequestList,
                                       PendingRequestActionListener pendingRequestActionListener, String myUid) {
        layoutInflater = LayoutInflater.from(context);
        Collections.sort(pendingRequestList);
        this.pendingRequestList = pendingRequestList;
        this.pendingRequestActionListener = pendingRequestActionListener;
        this.myUid = myUid;
    }

    @Override
    public PendingRequestViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = layoutInflater.inflate(R.layout.layout_pending_request_row, parent, false);
        return new PendingRequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(PendingRequestViewHolder holder, int position) {
        PendingFriendRequest request = pendingRequestList.get(position);
        if (request.getFromUid().equals(myUid)) {
            // Outgoing request
            holder.requestTypeImageView.setImageResource(R.drawable.ic_outgoing);
            holder.requestUsernameTextView.setText(request.getToUsername());
            holder.requestEmailTextView.setText(request.getToUserEmail());
            holder.incomingRequestOptionContainer.setVisibility(View.GONE);
            holder.outgoingRequestOptionContainer.setVisibility(View.VISIBLE);
        } else {
            // Incoming request
            holder.requestTypeImageView.setImageResource(R.drawable.ic_incoming);
            holder.requestUsernameTextView.setText(request.getFromUsername());
            holder.requestEmailTextView.setText(request.getFromUserEmail());
            holder.outgoingRequestOptionContainer.setVisibility(View.GONE);
            holder.incomingRequestOptionContainer.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return pendingRequestList == null ? 0 : pendingRequestList.size();
    }

    public void updatePendingRequestList(List<PendingFriendRequest> pendingRequestList) {
        if (pendingRequestList != null) {
            this.pendingRequestList.clear();
            Collections.sort(pendingRequestList);
            this.pendingRequestList.addAll(pendingRequestList);
            notifyDataSetChanged();
        }
    }


    public interface PendingRequestActionListener {

        void onRejectRequestClicked(PendingFriendRequest request);

        void onAcceptRequestClicked(PendingFriendRequest request);

        void onDeleteRequestClicked(PendingFriendRequest request);
    }

    protected class PendingRequestViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private RelativeLayout requestInfoContainer;
        private ImageView requestTypeImageView;
        private TextView requestUsernameTextView;
        private TextView requestEmailTextView;
        private LinearLayout incomingRequestOptionContainer;
        private LinearLayout outgoingRequestOptionContainer;
        private TextView rejectRequestTextView;
        private TextView acceptRequestTextView;
        private TextView deleteRequestTextView;

        public PendingRequestViewHolder(View itemView) {
            super(itemView);
            requestInfoContainer = (RelativeLayout) itemView.findViewById(R.id.request_info_container);
            requestTypeImageView = (ImageView) itemView.findViewById(R.id.request_type_image_view);
            requestUsernameTextView = (TextView) itemView.findViewById(R.id.request_username_text_view);
            requestEmailTextView = (TextView) itemView.findViewById(R.id.request_email_text_view);
            incomingRequestOptionContainer = (LinearLayout) itemView.findViewById(R.id.incoming_request_option_container);
            outgoingRequestOptionContainer = (LinearLayout) itemView.findViewById(R.id.outgoing_request_option_container);
            rejectRequestTextView = (TextView) itemView.findViewById(R.id.reject_request_text_view);
            acceptRequestTextView = (TextView) itemView.findViewById(R.id.accept_request_text_view);
            deleteRequestTextView = (TextView) itemView.findViewById(R.id.delete_request_text_view);

            requestInfoContainer.setOnClickListener(this);
            rejectRequestTextView.setOnClickListener(this);
            acceptRequestTextView.setOnClickListener(this);
            deleteRequestTextView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();
            PendingFriendRequest request = pendingRequestList.get(position);
            switch (v.getId()) {
                case R.id.request_info_container:
                    if (selectedPosition == position) {
                        selectedPosition = -1;
                    } else {
                        selectedPosition = position;
                    }
                    notifyDataSetChanged();
                    break;

                case R.id.reject_request_text_view:
                    pendingRequestActionListener.onRejectRequestClicked(request);
                    break;

                case R.id.accept_request_text_view:
                    pendingRequestActionListener.onAcceptRequestClicked(request);
                    break;

                case R.id.delete_request_text_view:
                    pendingRequestActionListener.onDeleteRequestClicked(request);
                    break;
            }
        }
    }
}
