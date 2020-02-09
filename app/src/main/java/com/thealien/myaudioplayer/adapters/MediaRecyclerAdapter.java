package com.thealien.myaudioplayer.adapters;

import android.content.Context;
import android.support.v4.media.MediaMetadataCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.thealien.myaudioplayer.R;

import java.util.List;

public class MediaRecyclerAdapter extends RecyclerView.Adapter<MediaRecyclerAdapter.MediaViewHolder> {

    private List<MediaMetadataCompat> mediaList ;
    private Context context;
    private OnItemClickListener listener;
    private int selectedIndex;

    public interface OnItemClickListener{
        void onItemClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener){
        this.listener = listener;
    }

    public MediaRecyclerAdapter(List<MediaMetadataCompat> mediaList, Context context) {
        this.mediaList = mediaList;
        this.context = context;
        selectedIndex = -1;
    }

    @NonNull
    @Override
    public MediaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.media_list_item,parent,false);
        return new MediaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MediaViewHolder holder, int position) {
        holder.titleTextView.setText(mediaList.get(position).getDescription().getTitle());
        holder.descriptionTextView.setText(mediaList.get(position).getDescription().getSubtitle().toString());
        //TODO make sure of this data type of date
        holder.timeStampTextView.setText(mediaList.get(position).getMediaMetadata().toString());

        if(selectedIndex == position){
            holder.titleTextView.setTextColor(ContextCompat.getColor(context,R.color.colorPrimary));
            holder.descriptionTextView.setTextColor(ContextCompat.getColor(context,R.color.colorPrimary));
        }else{
            holder.titleTextView.setTextColor(ContextCompat.getColor(context,R.color.black));
            holder.descriptionTextView.setTextColor(ContextCompat.getColor(context,R.color.black));
        }
    }

    public void setSelectedIndex(int index){
        selectedIndex = index;
        notifyDataSetChanged();
    }

    public int getSelectedIndex(){
        return selectedIndex;
    }

    public int getIndexOfItem(MediaMetadataCompat mediaItem){
        for(int i=0;i<mediaList.size();i++){
            if(mediaList.get(i).getDescription().getMediaId().equals(mediaItem.getDescription().getMediaId())){
                return i;
            }
        }
        return -1;
    }

    @Override
    public int getItemCount() {
        return mediaList.size();
    }

    public class MediaViewHolder extends RecyclerView.ViewHolder{

        public TextView titleTextView;
        public TextView descriptionTextView;
        public TextView timeStampTextView;

        public MediaViewHolder(@NonNull View itemView) {
            super(itemView);
            timeStampTextView = itemView.findViewById(R.id.media_time_stamp);
            titleTextView  = itemView.findViewById(R.id.media_title);
            descriptionTextView = itemView.findViewById(R.id.media_description);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = getAdapterPosition();
                    if(position != RecyclerView.NO_POSITION && listener != null){
                        listener.onItemClick(position);
                    }
                }
            });
        }
    }
}
