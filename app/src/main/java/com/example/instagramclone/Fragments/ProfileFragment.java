package com.example.instagramclone.Fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.instagramclone.Adapter.PhotoAdapter;
import com.example.instagramclone.EditProfileActivity;
import com.example.instagramclone.FollowersActivity;
import com.example.instagramclone.Model.Post;
import com.example.instagramclone.Model.User;
import com.example.instagramclone.OptionsActivity;
import com.example.instagramclone.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileFragment extends Fragment {

    private RecyclerView recyclerView;
    private RecyclerView recyclerView_saves;
    private PhotoAdapter photoAdapter;
    private List<Post> myPhotoList;

    private List<String> mySaves;

    private CircleImageView imageProfile;
    private ImageView options;
    private TextView followers;
    private TextView following;
    private TextView posts;
    private TextView fullname;
    private TextView bio;
    private TextView username;

    private ImageButton myPictures;
    private ImageButton savedPictures;

    private Button editProfile;

    private FirebaseUser fUser;

    private List<Post> postList_saves;
    private PhotoAdapter myFotosAdapter_saves;
    String profileId;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        fUser = FirebaseAuth.getInstance().getCurrentUser();

        String data = getContext().getSharedPreferences("PROFILE", Context.MODE_PRIVATE).getString("profileId", "none");

        if (data.equals("none")) {
            profileId = fUser.getUid();
        } else {
            profileId = data;
            getContext().getSharedPreferences("PROFILE", Context.MODE_PRIVATE).edit().clear().apply();
        }

        imageProfile = view.findViewById(R.id.image_profile);
        options = view.findViewById(R.id.options);
        followers = view.findViewById(R.id.followers);
        following = view.findViewById(R.id.following);
        posts = view.findViewById(R.id.posts);
        fullname = view.findViewById(R.id.fullname);
        bio = view.findViewById(R.id.bio);
        username = view.findViewById(R.id.username);
        myPictures = view.findViewById(R.id.my_pictures);
        savedPictures = view.findViewById(R.id.saved_pictures);
        editProfile = view.findViewById(R.id.edit_profile);

        recyclerView = view.findViewById(R.id.recucler_view_pictures);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        myPhotoList = new ArrayList<>();
        photoAdapter = new PhotoAdapter(getContext(), myPhotoList);
        recyclerView.setAdapter(photoAdapter);
        recyclerView.setVisibility(View.VISIBLE);

        recyclerView_saves = view.findViewById(R.id.recucler_view_saved);
        recyclerView_saves.setHasFixedSize(true);
        recyclerView_saves.setLayoutManager(new GridLayoutManager(getContext(), 3));
        postList_saves = new ArrayList<>();
        myFotosAdapter_saves = new PhotoAdapter(getContext(), postList_saves);
        recyclerView_saves.setVisibility(View.GONE);
        recyclerView_saves.setAdapter(myFotosAdapter_saves);

        userInfo();
        getFollowersAndFollowingCount();
        getPostCount();
        myPhotos();
        mySaves();

        if (profileId.equals(fUser.getUid())) {
            editProfile.setText("Edit profile");
        } else {
            checkFollowingStatus();
            savedPictures.setVisibility(View.GONE);
        }
        editProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String btnText = editProfile.getText().toString();

                if (btnText.equals("Edit profile")) {
                    startActivity(new Intent(getContext(), EditProfileActivity.class));
                } else {
                    if (btnText.equals("follow")) {
                        FirebaseDatabase.getInstance().getReference().child("Follow").child(fUser.getUid())
                                .child("following").child(profileId).setValue(true);

                        FirebaseDatabase.getInstance().getReference().child("Follow").child(profileId)
                                .child("followers").child(fUser.getUid()).setValue(true);
                    } else {
                        FirebaseDatabase.getInstance().getReference().child("Follow").child(fUser.getUid())
                                .child("following").child(profileId).removeValue();

                        FirebaseDatabase.getInstance().getReference().child("Follow").child(profileId)
                                .child("followers").child(fUser.getUid()).removeValue();
                    }
                }
            }
        });

        followers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), FollowersActivity.class);
                intent.putExtra("id", profileId);
                intent.putExtra("title", "followers");
                startActivity(intent);

            }
        });

        myPictures.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                recyclerView.setVisibility(View.VISIBLE);
                recyclerView_saves.setVisibility(View.GONE);
            }
        });

        savedPictures.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                recyclerView.setVisibility(View.GONE);
                recyclerView_saves.setVisibility(View.VISIBLE);
            }
        });

        following.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), FollowersActivity.class);
                intent.putExtra("id", profileId);

                intent.putExtra("title", "followings");
                startActivity(intent);

            }
        });

        options.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getContext(), OptionsActivity.class));
            }
        });

        return view;
    }

    private void myPhotos() {

        FirebaseDatabase.getInstance().getReference().child("Posts").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                myPhotoList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Post post = snapshot.getValue(Post.class);

                    if (post.getPublisher().equals(profileId)) {
                        myPhotoList.add(post);
                    }
                }

                Collections.reverse(myPhotoList);
                photoAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }
    private void checkFollowingStatus() {

        FirebaseDatabase.getInstance().getReference().child("Follow").child(fUser.getUid()).child("following").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.child(profileId).exists()) {
                    editProfile.setText("following");
                } else {
                    editProfile.setText("follow");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void getPostCount() {

        FirebaseDatabase.getInstance().getReference().child("Posts").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int counter = 0;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Post post = snapshot.getValue(Post.class);

                    if (post.getPublisher().equals(profileId)) counter ++;
                }

                posts.setText(String.valueOf(counter));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void getFollowersAndFollowingCount() {

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("Follow").child(profileId);

        ref.child("followers").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                followers.setText("" + dataSnapshot.getChildrenCount());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        ref.child("following").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                following.setText("" + dataSnapshot.getChildrenCount());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void userInfo() {

        FirebaseDatabase.getInstance().getReference().child("Users").child(profileId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);

                Picasso.get().load(user.getImageurl()).placeholder(R.mipmap.ic_launcher).into(imageProfile);
//                if (user.getImageurl().equals("default")){
//                    holder.imageProfile.setImageResource(R.mipmap.ic_launcher);
//                } else {
//                    Picasso.get().load(user.getImageurl()).placeholder(R.mipmap.ic_launcher).into(holder.imageProfile);
//                }
                username.setText(user.getUsername());
                fullname.setText(user.getName());
                bio.setText(user.getBio());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    private void mySaves(){
        mySaves = new ArrayList<>();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Saves").child(fUser.getUid());
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()){
                    mySaves.add(snapshot.getKey());
                }
                readSaves();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void readSaves(){
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Posts");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                postList_saves.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()){
                    Post post = snapshot.getValue(Post.class);

                    for (String id : mySaves) {
                        if (post.getPostid().equals(id)) {
                            postList_saves.add(post);
                        }
                    }
                }
                myFotosAdapter_saves.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

}