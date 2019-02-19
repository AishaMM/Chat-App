package com.koddev.chatapp;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.koddev.chatapp.Fragments.APIService;
import com.koddev.chatapp.Model.Chat;
import com.koddev.chatapp.Model.Upload;
import com.koddev.chatapp.Model.User;
import com.koddev.chatapp.Notifications.Client;
import com.koddev.chatapp.Notifications.Data;
import com.koddev.chatapp.Notifications.MyResponse;
import com.koddev.chatapp.Notifications.Sender;
import com.koddev.chatapp.Notifications.Token;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ImageActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    FloatingActionButton send_image;

    EditText text_send;
    ImageView imageView;

    String userid;
    Intent intent;

    boolean notify = false;
    APIService apiService;

    ProgressDialog progressDialog;
    ProgressBar progressBar;

    FirebaseUser fuser;
    DatabaseReference reference, imageRef, rootRef;


    StorageReference imageStorage;
    StorageTask uploads;


    private final String IMAGE_DIRECTORY = "/IMAGES";
    private Uri outputFileUri;
    private static final int CAMERA = 1;
    private static final int GALLERY_PICK = 2;
    public static final int READ_EXTERNAL_STORAGE = 0, WRITE_EXTERNAL_STORAGE = 1 , MULTIPLE_PERMISSIONS = 10;
    private Uri imageUri ;

    private String imagePath = "";
    final CharSequence[] options = {"Camera", "Gallery", "Cancel"};
    String[] permissions = new String[] {
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };
/*
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);
/*
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);


        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // and this
                startActivity(new Intent(ImageActivity.this, MessageActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            }
        });

        send_image = findViewById(R.id.send_image);
        text_send = findViewById(R.id.text_send);
        imageView = findViewById(R.id.imageView);

//Intent receivedIntent = getIntent();
        if(getIntent().hasExtra("byte"))
        {
            Bitmap bitmap = BitmapFactory.decodeByteArray(getIntent().getByteArrayExtra("byte"),0,getIntent().getByteArrayExtra("byte").length);
            imageView.setImageBitmap(bitmap);
        }

        rootRef = FirebaseDatabase.getInstance().getReference();
        imageStorage = FirebaseStorage.getInstance().getReference("Images");
        imageRef = FirebaseDatabase.getInstance().getReference("Images");
        progressDialog = new ProgressDialog(ImageActivity.this);

        apiService = Client.getClient("https://fcm.googleapis.com/").create(APIService.class);


        intent = getIntent();
        userid = intent.getStringExtra("userid");
        fuser = FirebaseAuth.getInstance().getCurrentUser();

        Choice();
        //getImage();
        final Bitmap bitmap = (BitmapFactory.decodeFile(imagePath));
        imageView.setImageBitmap(bitmap);
        send_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                notify = true;
                if (uploads != null && uploads.isInProgress()) {
                    Toast.makeText(ImageActivity.this, "Upload in progress", Toast.LENGTH_SHORT).show();
                } else {

                    sendImage();

                    Intent intent = new Intent(ImageActivity.this, MessageActivity.class);
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG,50,byteArrayOutputStream);
                    intent.putExtra("byte", byteArrayOutputStream.toByteArray());
                    startActivity(intent);
                }
               // Picasso.with(getApplicationContext()).load(imageUri).into(imageView);
                //startActivity(new Intent(ImageActivity.this, MessageActivity.class));

            }
        });
    }

    private String getFileExtension(Uri uri) {
        ContentResolver contentResolver = getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));
    }
    private void sendImage() {
        if(imageUri != null) {
            reference = FirebaseDatabase.getInstance().getReference("Chats");
            DatabaseReference push_message = reference.child(fuser.getUid()).child(userid).push();

            String push_id = push_message.getKey();

            final StorageReference filepath = imageStorage.child("GALLERY").child(push_id + ".jpg");
            final UploadTask uploadTask = filepath.putFile(imageUri);


             //Bitmap thumbnail = (BitmapFactory.decodeFile(imagePath));
             //imageView.setImageBitmap(thumbnail);
             //imageView.setVisibility(View.VISIBLE);

            Picasso.with(ImageActivity.this)
                    .load(imageUri)
                    .into(imageView);

             uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setProgress(0);
                            }

                        }, 500);


                        progressDialog.dismiss();
                        String downloadUri = taskSnapshot.getStorage().getDownloadUrl().toString();
                        Toast.makeText(ImageActivity.this, "Image uploaded", Toast.LENGTH_LONG).show();
                        //Chat chat = new Chat(fuser.getUid(), userid, downloadUri, notify);
                        Upload upload = new Upload(downloadUri);
                        String uploadId = reference.push().getKey();
                        reference.child(uploadId).setValue(upload);
                        //sendImage(fuser.getUid(), userid, downloadUri);

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(ImageActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }

                }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                        double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                        progressBar.setProgress((int) progress);
                    }
                });

        } else {
            Toast.makeText(this, "No file selected", Toast.LENGTH_SHORT).show();

        }

    }

    private void getImage() {
        rootRef.child("Chats").child(fuser.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        if (dataSnapshot.exists() && (dataSnapshot.hasChild("sender"))
                                && (dataSnapshot.hasChild("receiver")) && (dataSnapshot.hasChild("sender")))
                        {
                            String sender = dataSnapshot.child("sender").getValue().toString();
                            String receiver = dataSnapshot.child("receiver").getValue().toString();
                            String image = dataSnapshot.child("image").getValue().toString();

                            Picasso.with(getApplicationContext()).load(image).into(imageView);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }
    private void Choice () {
        AlertDialog.Builder builder = new AlertDialog.Builder(ImageActivity.this);
        builder.setTitle("Choose Source");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (options[item].equals("Camera")) {
                    //if (checkPermissions()) {
                    if (ContextCompat.checkSelfPermission(ImageActivity.this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(ImageActivity.this,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_STORAGE);
                    } else {
                        callCamera();
                    }
                }
                if (options[item].equals("Gallery")) {
                    if (ContextCompat.checkSelfPermission(ImageActivity.this,
                            Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(ImageActivity.this,
                                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EXTERNAL_STORAGE);
                    } else {
                        callGallery();
                    }
                }
                if (options[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            switch (requestCode) {
                case READ_EXTERNAL_STORAGE:
                    if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                        callGallery();
                    return;
                case WRITE_EXTERNAL_STORAGE:
                    if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    {
                        callCamera();
                    }
                case MULTIPLE_PERMISSIONS: {
                    if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    {
                        callCamera();
                    }
                }

            }
        }

        private void callCamera() {

           /* Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            // Ensure that there's a camera activity to handle the intent
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                // Create the File where the photo should go
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                } catch (IOException ex) {
                    // Error occurred while creating the File
                }
                // Continue only if the File was successfully created
                if (photoFile != null) {
                    outputFileUri = FileProvider.getUriForFile(this,
                            "com.example.android.fileprovider",
                            photoFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
                    startActivityForResult(takePictureIntent, CAMERA);
                }
            }
String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    String imageFileName = timeStamp + ".jpg";
    File storageDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES);
            Log.d("LOGGED", "imageFileName : " + imageFileName);
    imagePath = imageFileName + "/" + storageDir.getAbsolutePath() ;

    File file = new File(imagePath);
    outputFileUri = FileProvider.getUriForFile(getApplicationContext(),
    getApplicationContext().getPackageName() + ".provider", file);

    Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
            cameraIntent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
            cameraIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            cameraIntent.setData(outputFileUri);

            Log.d("LOGGED", "pictureImagePath : "+ imagePath);
            Log.d("LOGGED", "outputFileUri : "+ outputFileUri);

           /*if(cameraIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(cameraIntent, CAMERA);

        }

            super.startActivityForResult(cameraIntent, CAMERA);
}

    private void callGallery() {

        Intent intent = new Intent(Intent.ACTION_PICK_ACTIVITY);
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);

        super.startActivityForResult(Intent.createChooser(intent, "SELECT IMAGE"), GALLERY_PICK);
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );

        // Save a file: path for use with ACTION_VIEW intents
        imagePath = image.getAbsolutePath();
        return image;
    }


    public void onActivityResult(int requestcode, int resultcode, final Intent data) {
        super.onActivityResult(requestcode, resultcode, data);
        if (requestcode  == GALLERY_PICK && resultcode == Activity.RESULT_OK && data != null && data.getData() != null) {

            imageUri = data.getData();

            Picasso.with(ImageActivity.this)
                    .load(imageUri)
                    .into(imageView);

/*
            reference = FirebaseDatabase.getInstance().getReference("Chats");
            DatabaseReference push_message = reference.child(fuser.getUid()).child(userid).push();

            String push_id = push_message.getKey();


            //progressDialog.setMessage("Uploading...");
            // progressDialog.show();
            imageUri = data.getData();

            if (imageUri != null) {

                final StorageReference filepath = imageStorage.child("GALLERY").child(push_id + ".jpg");
                final UploadTask uploadTask = filepath.putFile(imageUri);


               // Bitmap thumbnail = (BitmapFactory.decodeFile(imagePath));
                //imageView.setImageBitmap(thumbnail);
               // imageView.setVisibility(View.VISIBLE);

                Picasso.with(ImageActivity.this)
                        .load(imageUri)
                        .into(imageView);

                if (uploads != null && uploads.isInProgress()) {
                    Toast.makeText(ImageActivity.this, "Upload in progress", Toast.LENGTH_SHORT).show();
                } else {
                    uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                            Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    progressBar.setProgress(0);
                                }

                            }, 500);


                            progressDialog.dismiss();
                            String downloadUri = taskSnapshot.getStorage().getDownloadUrl().toString();
                            Toast.makeText(ImageActivity.this, "Image uploaded", Toast.LENGTH_LONG).show();
                            Chat chat = new Chat(fuser.getUid(), userid, downloadUri, notify);
                            reference.push().setValue(chat);
                            //sendImage(fuser.getUid(), userid, downloadUri);

                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(ImageActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }

                    }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                            progressBar.setProgress((int) progress);
                        }
                    });
                }
            } else {
                Toast.makeText(this, "No file selected", Toast.LENGTH_SHORT).show();

            }




        } else if (requestcode == CAMERA) {


            //Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            File imgFile = new File(imagePath);
            if (imgFile.exists()) {
                Log.d("LOGGED", "imgFile : " + imgFile);

                Uri fileUri = Uri.fromFile(imgFile);
                Log.d("LOGGED", "fileUri : " + fileUri);
                //mediaScanIntent.setData(fileUri);
                //this.sendBroadcast(mediaScanIntent);

                final StorageReference filepath = imageStorage.child("CAMERA");
                final UploadTask uploadTask = filepath.putFile(fileUri);

                Bitmap thumbnail = (Bitmap) data.getExtras().get("data");
                imageView.setImageBitmap(thumbnail);
                Picasso.with(ImageActivity.this)
                        .load(imageUri)
                        .into(imageView);
                //imageView.setVisibility(View.VISIBLE);
                Toast.makeText(getApplicationContext(), "Image Saved!", Toast.LENGTH_SHORT).show();

                if (uploads != null && uploads.isInProgress()) {
                    Toast.makeText(ImageActivity.this, "Upload in progress", Toast.LENGTH_SHORT).show();
                } else {
                    uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    progressBar.setProgress(0);
                                }

                            }, 500);

                            progressDialog.dismiss();
                            //String downloadUri = filepath.getDownloadUrl().toString();
                            Toast.makeText(ImageActivity.this, "Image uploaded", Toast.LENGTH_LONG).show();

                            //sendImage(fuser.getUid(), userid, downloadUri);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(ImageActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                            progressBar.setProgress((int) progress);
                        }
                    });

                }


            }
            else if (requestcode == CAMERA) {
                Toast.makeText(this, "resultCode : " + resultcode, Toast.LENGTH_SHORT).show();
            }


        }
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item){

        switch (item.getItemId()) {
            case android.R.id.home:
                this.onBackPressed();//takes user back
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    public boolean onSupportNavigateUp () {
        onBackPressed();
        return true;
    }
    private void sendImage(String sender, final String receiver, String downloadUrl) {

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("sender", sender);
        hashMap.put("receiver", receiver);
        hashMap.put("message", downloadUrl);
        hashMap.put("isseen", false);

        reference.child("Chats").push().setValue(hashMap);


        // add user to chat fragment
        final DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference("Chatlist")
                .child(fuser.getUid())
                .child(userid);

        chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    chatRef.child("id").setValue(userid);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        final DatabaseReference chatRefReceiver = FirebaseDatabase.getInstance().getReference("Chatlist")
                .child(userid)
                .child(fuser.getUid());
        chatRefReceiver.child("id").setValue(fuser.getUid());

        final String msg = downloadUrl;

        reference = FirebaseDatabase.getInstance().getReference("Users").child(fuser.getUid());
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                if (notify) {
                    sendNotifiaction(receiver, user.getUsername(), msg);
                }
                notify = false;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void sendNotifiaction(String receiver, final String username, final String message) {
        DatabaseReference tokens = FirebaseDatabase.getInstance().getReference("Tokens");
        Query query = tokens.orderByKey().equalTo(receiver);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Token token = snapshot.getValue(Token.class);
                    Data data = new Data(fuser.getUid(), R.mipmap.ic_launcher, username + ": " + message, "New Message",
                            userid);

                    Sender sender = new Sender(data, token.getToken());

                    apiService.sendNotification(sender)
                            .enqueue(new Callback<MyResponse>() {
                                @Override
                                public void onResponse(Call<MyResponse> call, Response<MyResponse> response) {
                                    if (response.code() == 200) {
                                        if (response.body().success != 1) {
                                            Toast.makeText(ImageActivity.this, "Failed!", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }

                                @Override
                                public void onFailure(Call<MyResponse> call, Throwable t) {

                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
*/
}