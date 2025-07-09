package com.example.chatapp.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chatapp.R;
import com.example.chatapp.adapters.RecentConversationAdapter;
import com.example.chatapp.databinding.ActivityMainBinding;
import com.example.chatapp.listeners.ConversionListener;
import com.example.chatapp.models.ChatMessage;
import com.example.chatapp.models.User;
import com.example.chatapp.utilities.Constants;
import com.example.chatapp.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends BaseActivity implements ConversionListener {
    private ActivityMainBinding binding;
    private PreferenceManager preferenceManager;
    private List<ChatMessage> conversations;
    private RecentConversationAdapter conversationAdapter;
    FirebaseFirestore database;
    private static final int REQUEST_CODE_SELECT_IMAGE = 101;
    private Uri selectedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        preferenceManager = new PreferenceManager(getApplicationContext());
        init();
        loadUserDetails();
        getToken();
        setListeners();
        listenConversations();

    }

    private void init(){
        conversations = new ArrayList<>();
        conversationAdapter = new RecentConversationAdapter(conversations, this, chatMessage -> {
            // Long click logic: show confirmation dialog and delete
            showDeleteDialog(chatMessage);
        });
        binding.conversationRecycleview.setAdapter(conversationAdapter);
        database = FirebaseFirestore.getInstance();
    }

    private void setListeners() {
        binding.imageSignout.setOnClickListener(v -> showCustomSignOutDialog());
        binding.fabNewChat.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(), UserActivity.class)));
        binding.imageProfile.setOnClickListener(v -> showProfileDialog());

    }

    private void loadUserDetails() {
        binding.textName.setText(preferenceManager.getString(Constants.Key_NAME));
        byte[] bytes = Base64.decode(preferenceManager.getString(Constants.KEY_IMAGE), Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        binding.imageProfile.setImageBitmap(bitmap);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void listenConversations(){
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID,preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_RECEIVER_ID,preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }

    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if (error != null) {
            return;
        }
        if (value != null){
            for(DocumentChange documentChange : value.getDocumentChanges()){
                if(documentChange.getType() == DocumentChange.Type.ADDED){
                    String senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    String receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.senderId = senderId;
                    chatMessage.receiverId = receiverId;
                    if (preferenceManager.getString(Constants.KEY_USER_ID).equals(senderId)){
                        chatMessage.conversionImage = documentChange.getDocument().getString(Constants.KEY_RECEIVER_IMAGE);
                        chatMessage.conversionName = documentChange.getDocument().getString(Constants.KEY_RECEIVER_NAME);
                        chatMessage.conversionId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    }else {
                        chatMessage.conversionImage = documentChange.getDocument().getString(Constants.KEY_SENDER_IMAGE);
                        chatMessage.conversionName = documentChange.getDocument().getString(Constants.KEY_SENDER_NAME);
                        chatMessage.conversionId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    }
                    chatMessage.message = documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE);
                    chatMessage.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    conversations.add(chatMessage);
                } else if (documentChange.getType() == DocumentChange.Type.MODIFIED) {
                    for (int i=0 ; i <conversations.size();i++){
                        String senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                        String receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                        if(conversations.get(i).senderId.equals(senderId) && conversations.get(i).receiverId.equals(receiverId)){
                            conversations.get(i).message = documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE);
                            conversations.get(i).dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                            break;
                        }
                    }
                }
            }
            Collections.sort(conversations, (obj1, obj2) -> obj2.dateObject.compareTo(obj1.dateObject));
            conversationAdapter.notifyDataSetChanged();
            binding.conversationRecycleview.smoothScrollToPosition(0);
            binding.conversationRecycleview.setVisibility(View.VISIBLE);
            binding.progressbar.setVisibility(View.GONE);
        }
    };

    private void getToken() {
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(this::updateToken);
    }

    private void updateToken(String token) {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference = database.collection(Constants.Key_COLLECTION_USERS)
                .document(preferenceManager.getString(Constants.KEY_USER_ID));

        documentReference.update(Constants.KEY_FCM_TOKEN, token)
                .addOnFailureListener(e -> showToast("Unable to update token"));
    }
    private void showCustomSignOutDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_confirm_sign_out, null);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(false)
                .create();

        Button buttonCancel = view.findViewById(R.id.buttonCancel);
        Button buttonConfirm = view.findViewById(R.id.buttonConfirm);

        buttonCancel.setOnClickListener(v -> dialog.dismiss());

        buttonConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            signOut();
        });

        dialog.show();
    }



    private void signOut() {
        showToast("Signing out...");
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference = database.collection(Constants.Key_COLLECTION_USERS)
                .document(preferenceManager.getString(Constants.KEY_USER_ID));

        HashMap<String, Object> updates = new HashMap<>();
        updates.put(Constants.KEY_FCM_TOKEN, FieldValue.delete());

        documentReference.update(updates)
                .addOnSuccessListener(unused -> {
                    preferenceManager.clear();
                    startActivity(new Intent(getApplicationContext(), SignInActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> showToast("Unable to sign out"));
    }

    @Override
    public void onConversionClicked(User user) {
        Intent intent = new Intent(getApplicationContext(),chatActivity.class);
        intent.putExtra(Constants.KEY_USER,user);
        startActivity(intent);
    }

    private void showDeleteDialog(ChatMessage chatMessage) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_delete_chat, null);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(false)
                .create();

        Button buttonCancel = view.findViewById(R.id.buttonCancel);
        Button buttonConfirm = view.findViewById(R.id.buttonConfirm);

        // Customize button text for delete action
        buttonConfirm.setText("Delete");

        buttonCancel.setOnClickListener(v -> dialog.dismiss());

        buttonConfirm.setOnClickListener(v -> {
            deleteConversation(chatMessage);  // Your logic to delete chat
            dialog.dismiss();
        });

        dialog.show();
    }
    private void deleteConversation(ChatMessage chatMessage) {
        FirebaseFirestore.getInstance()
                .collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID, chatMessage.senderId)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, chatMessage.receiverId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        doc.getReference().delete();
                    }
                    conversations.remove(chatMessage);
                    conversationAdapter.notifyDataSetChanged(); // or use notifyItemRemoved
                    showToast("Chat deleted");
                })
                .addOnFailureListener(e -> showToast("Failed to delete chat"));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && resultCode == RESULT_OK && data != null) {
            try {
                Uri imageUri = data.getData();
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                binding.imageProfile.setImageBitmap(bitmap);  // Update UI instantly

                // Save in shared pref
                String encodedImage = encodeImage(bitmap);
                preferenceManager.putString(Constants.KEY_IMAGE, encodedImage);

                // Save in Firestore
                FirebaseFirestore.getInstance().collection(Constants.Key_COLLECTION_USERS)
                        .document(preferenceManager.getString(Constants.KEY_USER_ID))
                        .update(Constants.KEY_IMAGE, encodedImage)
                        .addOnSuccessListener(unused -> showToast("Profile updated"))
                        .addOnFailureListener(e -> showToast("Failed to update profile"));

            } catch (Exception e) {
                showToast("Error loading image");
            }
        }
    }

    private void showProfileDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_profile_image, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        ImageView imageView = view.findViewById(R.id.profileImageView);
        Button buttonChange = view.findViewById(R.id.buttonChange);
        Button buttonClose = view.findViewById(R.id.buttonClose);

        // Set current profile image
        byte[] bytes = Base64.decode(preferenceManager.getString(Constants.KEY_IMAGE), Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        imageView.setImageBitmap(bitmap);

        buttonClose.setOnClickListener(v -> dialog.dismiss());

        buttonChange.setOnClickListener(v -> {
            dialog.dismiss();
            // Start gallery intent
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, 101);
        });

        dialog.show();
    }

    private String encodeImage(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, stream);
        byte[] bytes = stream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }


}
