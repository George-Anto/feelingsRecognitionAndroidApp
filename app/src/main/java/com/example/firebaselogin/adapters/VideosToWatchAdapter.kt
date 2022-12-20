package com.example.firebaselogin.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.firebaselogin.R
import com.example.firebaselogin.model.VideoData
import kotlinx.android.synthetic.main.video_card.view.*

//The adapter that we use to fill the videos recycler view in the VideoChooserActivity
//It needs a context and an ArrayList of VideosData to populate each ViewHolder
class VideosToWatchAdapter(
    private val context: Context,
    private val videosList: ArrayList<VideoData>):
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    //Listener property
    private var onClickListener: OnClickListener? = null

    //The ViewHolders will be created using the video_card layout and
    //our custom ViewHolder class VideoViewHolder, the context
    //we will provide will be the VideoChooserActivity
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return VideoViewHolder(
            LayoutInflater
                .from(context)
                .inflate(R.layout.video_card, parent, false)
        )
    }

    //This is where the most of the work is done
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        //Each time this function is called (for every item in the ArrayList provided)
        //We create a model out of the current item of the ArrayList
        val model = videosList[position]
        //If the holder provided is our custom VideoViewHolder
        if (holder is VideoViewHolder) {
            //Set the name of the card to the UI according to the name of the model
            holder.itemView.tv_video_name.text = model.name

            //Set the thumbnail of the card to the UI according to the thumbnail of the model
            Glide
                .with(context)
                .load(model.thumbnail)
                .centerCrop()
//                .placeholder(R.drawable.ic_user_place_holder)
                .into(holder.itemView.img_video_thumbnail)
        }

        //Call the onClick() function of the interface if the onClickListener field is initialized
        holder.itemView.setOnClickListener {
            if (onClickListener != null) {
                onClickListener!!.onClick(position, model)
            }
        }
    }

    //Set the item count equal to the size of the ArrayList provided
    override fun getItemCount(): Int {
        return videosList.size
    }

    //Bind the onClick event with the field of the adapter
    fun setOnClickListener(onClickListener: OnClickListener) {
        this.onClickListener = onClickListener
    }

    //Listener interface that is used to set an onClick listener event in each card in the UI
    interface OnClickListener {
        fun onClick(position: Int, model: VideoData)
    }

    //Our custom ViewHolder class that inherits from RecyclerView.ViewHolder
    private class VideoViewHolder(view: View): RecyclerView.ViewHolder(view)
}