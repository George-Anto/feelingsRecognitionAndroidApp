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

class VideosToWatchAdapter(
    private val context: Context,
    private val videosList: ArrayList<VideoData>):
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var onClickListener: OnClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return MyViewHolder(
            LayoutInflater
                .from(context)
                .inflate(R.layout.video_card, parent, false)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val model = videosList[position]
        if (holder is MyViewHolder) {
            holder.itemView.tv_video_name.text = model.name

            holder.itemView.img_video_thumbnail

            //Load the user image in the ImageView using a third party library
            Glide
                .with(context)
                .load(model.thumbnail)
                .centerCrop()
//                .placeholder(R.drawable.ic_user_place_holder)
                .into(holder.itemView.img_video_thumbnail)
        }

        holder.itemView.setOnClickListener {
            if (onClickListener != null) {
                onClickListener!!.onClick(position, model)
            }
        }
    }

    override fun getItemCount(): Int {
        return videosList.size
    }

    interface OnClickListener {
        fun onClick(position: Int, model: VideoData)
    }

    private class MyViewHolder(view: View): RecyclerView.ViewHolder(view)
}