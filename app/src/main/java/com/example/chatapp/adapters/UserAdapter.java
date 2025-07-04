package com.example.chatapp.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.R;
import com.example.chatapp.databinding.ItemContainerUserBinding;
import com.example.chatapp.listeners.UsersListeners;
import com.example.chatapp.models.User;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.userViewHolder>{

    private final List<User> users;
    private final UsersListeners usersListeners;
    private final Context context; // Context to access resources

    public UserAdapter(List<User> users, UsersListeners usersListeners, Context context) {
        this.users = users;
        this.usersListeners = usersListeners;
        this.context = context;  // Initialize the context
    }


    @NonNull
    @Override
    public UserAdapter.userViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        ItemContainerUserBinding itemContainerUserBinding = ItemContainerUserBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );

        return new userViewHolder(itemContainerUserBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull UserAdapter.userViewHolder holder, int position) {
        holder.setUserData(users.get(position));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    class userViewHolder extends RecyclerView.ViewHolder{

        ItemContainerUserBinding binding;

        userViewHolder(ItemContainerUserBinding itemContainerUserBinding){
            super(itemContainerUserBinding.getRoot());
            binding = itemContainerUserBinding;
        }

        void setUserData(User user){
            binding.textName.setText(user.name);
            binding.textEmail.setText(user.email);
            binding.iamgeProfile.setImageBitmap(getuserImage(user.Image));
            binding.getRoot().setOnClickListener(v -> usersListeners.onUserClicked(user));
        }
    }

    private Bitmap getuserImage(String encodedImage) {
        if (encodedImage == null || encodedImage.isEmpty()) {
            // Fallback to a default image if no image is found
            return BitmapFactory.decodeResource(context.getResources(), R.drawable.bacground_content_top);
        }

        try {
            byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (IllegalArgumentException e) {
            // If decoding fails, fallback to default image
            return BitmapFactory.decodeResource(context.getResources(), R.drawable.bacground_content_top);
        }
    }

}
