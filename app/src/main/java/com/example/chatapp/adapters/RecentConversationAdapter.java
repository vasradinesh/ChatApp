package com.example.chatapp.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.databinding.IconContainerReciveMessageBinding;
import com.example.chatapp.databinding.ItemContainerRecentConversionBinding;
import com.example.chatapp.listeners.ConversionListener;
import com.example.chatapp.models.ChatMessage;
import com.example.chatapp.models.User;

import java.util.List;

public class RecentConversationAdapter extends  RecyclerView.Adapter<RecentConversationAdapter.ConversionViewHolder>{

    private final List<ChatMessage> chatMessage;
    private final ConversionListener conversionListener;


    public RecentConversationAdapter(List<ChatMessage> chatMessage, ConversionListener conversionListener) {
        this.chatMessage = chatMessage;
        this.conversionListener = conversionListener;
    }

    @NonNull
    @Override
    public ConversionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ConversionViewHolder(
                ItemContainerRecentConversionBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                )

        );
    }

    @Override
    public void onBindViewHolder(@NonNull ConversionViewHolder holder, int position) {
        holder.setdata(chatMessage.get(position));
    }

    @Override
    public int getItemCount() {
        return chatMessage.size();
    }

    class  ConversionViewHolder extends RecyclerView.ViewHolder{

       ItemContainerRecentConversionBinding binding;

       ConversionViewHolder(ItemContainerRecentConversionBinding itemContainerRecentConversionBinding){
           super(itemContainerRecentConversionBinding.getRoot());
           binding= itemContainerRecentConversionBinding;
       }
       void setdata(ChatMessage chatMessage){
           binding.iamgeProfile.setImageBitmap(getConversionImage(chatMessage.conversionImage));
           binding.textName.setText(chatMessage.conversionName);
           binding.textRecentMessage.setText(chatMessage.message);
           binding.getRoot().setOnClickListener(v -> {
               User user = new User();
               user.id = chatMessage.conversionId;
               user.name = chatMessage.conversionName;
               user.Image = chatMessage.conversionImage;
               conversionListener.onConversionClicked(user);
           });
       }
    }

    private Bitmap getConversionImage(String encodedImage) {
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }
}
