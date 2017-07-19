package com.example.mapdemo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.parse.FindCallback;
import com.parse.ParseClassName;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.RuntimePermissions;

@ParseClassName("MarkerDetailsActivity")
@RuntimePermissions
public class MarkerDetailsActivity extends AppCompatActivity {

    public final String APP_TAG = "SeenAds";
    public final static int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 1034;
    
    public boolean parseFlag = false;

    // For local storage
    public final String ABSOLUTE_FILE_PATH = "/storage/emulated/0/Android/data/com.example.mapdemo/files/Pictures/SeenAds/";
    public String photoFileName = "photo.jpg";
    // private String finalFileName = "";

    // For parse & compression
    private ByteArrayOutputStream stream;
    private String snip;
    private String location;

    TextView tvTitle;
    TextView tvSnippet;
    ImageButton ibUploadPic;
    ImageView ivMarkerPhoto;

    // Later if we get to it
    RecyclerView rvComments;

    // TODO make this prettier

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.markerdetails_activity);
        // retrieve intent & setup
        String ID = getIntent().getStringExtra("title");
        String snippet = getIntent().getStringExtra("snippet");
        snip = snippet;
        location = getIntent().getStringExtra("location");
        photoFileName = photoFileName + ID;
        tvTitle = (TextView) findViewById(R.id.tvTitle);
        tvSnippet = (TextView) findViewById(R.id.tvSnippet);
        ivMarkerPhoto = (ImageView) findViewById(R.id.ivMarkerPhoto);
        rvComments = (RecyclerView) findViewById(R.id.rvComments);

        // loading photo file based on LOCATION from Parse
        ParseQuery<ParseObject> query  = ParseQuery.getQuery("ParseImageArrays");
        query.whereEqualTo("Location", location);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> parseObjects, com.parse.ParseException e) {
                if (e==null){
                    int size = parseObjects.size();
                    parseFlag = true;
                    // TODO figure out a way to handle duplicate images LATER
                    // if there's something at this location already, load the most recent image (assuming size of list is one, but handling other possibilities)
                    if (size > 0) {
                        ParseObject match = parseObjects.get(size - 1);
                        // get the image file
                        ParseFile imgFile = match.getParseFile("MarkerImage");
                        // get the URL
                        String imgFileUrl = imgFile.getUrl();
                        // load using Glide
                        Glide.with(getApplicationContext())
                                .load(imgFileUrl)
                                .into(ivMarkerPhoto);
                        // load it into the image view
                        String firstItemId = parseObjects.get(0).getObjectId();
                        Toast.makeText(MarkerDetailsActivity.this, "Loading from PARSE: object " + firstItemId, Toast.LENGTH_SHORT).show();
                    }
                    // else don't load any image & wait for the user to upload one
                } else {
                    Log.e("ERROR:", "" + e.getMessage());
                }
            }
        });

        // if there's already a path to the corresponding picture for this marker, load it instead of the placeholder image
        if (!parseFlag) { // TODO figure out how to fix double loading -- local & parse loading happen asynchronously so flag isn't useful
            // possible solution ^: a time delay?
            File imgFile = new  File(ABSOLUTE_FILE_PATH + photoFileName);
            if(imgFile.exists()){
                Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                ivMarkerPhoto.setImageBitmap(myBitmap);
            }
        }
        ibUploadPic = (ImageButton) findViewById(R.id.ibUploadPic);
        ibUploadPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MarkerDetailsActivityPermissionsDispatcher.onLaunchCameraWithCheck(MarkerDetailsActivity.this, v);
            }
        });
        // set information
        tvTitle.setText(ID);
        tvSnippet.setText(snippet);
    }


    @NeedsPermission(Manifest.permission.CAMERA)
    public void onLaunchCamera(View view) {
        // create Intent to take a picture and return control to the calling application
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, getPhotoFileUri(photoFileName)); // set the image file name
        // So as long as the result is not null, it's safe to use the intent.
        if (intent.resolveActivity(getPackageManager()) != null) {
            // Start the image capture intent to take photo
            startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        MarkerDetailsActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Uri takenPhotoUri = getPhotoFileUri(photoFileName);
                // by this point we have the camera photo on disk
                Bitmap takenImage = BitmapFactory.decodeFile(ABSOLUTE_FILE_PATH + photoFileName);
                Bitmap resizedImage = BitmapScaler.scaleToFitWidth(takenImage, 430);
                // Configure byte output stream
                stream = new ByteArrayOutputStream();
                // Compress the image further
                resizedImage.compress(Bitmap.CompressFormat.JPEG, 90, stream);

                // Save image to Parse
                byte[] image = stream.toByteArray();
                final ParseFile parseImage = new ParseFile(image);
                try {
                    parseImage.save();
                } catch (ParseException e) {
                    e.printStackTrace();
                }


                ParseObject testObject = new ParseObject("ParseImageArrays");
                // Put the image file into parse DB under ParseImageArrays class
                testObject.put("MarkerImage", parseImage);
                // For retrieval of images; check if the Parse location matches the current marker's location for loading
                testObject.put("Snippet", snip);
                testObject.put("Location", location);
                testObject.saveInBackground();

                // Create a new file for the resized bitmap (`getPhotoFileUri` defined above)
                Uri resizedUri = getPhotoFileUri(photoFileName + "_resized");
                File resizedFile = new File(resizedUri.getPath());
                FileOutputStream fos = null;
                try {
                    resizedFile.createNewFile();
                    fos = new FileOutputStream(resizedFile);
                    fos.write(stream.toByteArray());
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                // Load the resized image into a preview
                ivMarkerPhoto.setImageBitmap(takenImage);
            } else { // Result was a failure
                Toast.makeText(this, "Picture wasn't taken!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Returns the Uri for a photo stored on disk given the fileName
    public Uri getPhotoFileUri(String fileName) {
        // Only continue if the SD Card is mounted
        if (isExternalStorageAvailable()) {
            // Get safe storage directory for photos
            File mediaStorageDir = new File(
                    getExternalFilesDir(Environment.DIRECTORY_PICTURES), APP_TAG);
            // Create the storage directory if it does not exist
            if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()){
                Log.d(APP_TAG, "failed to create directory");
            }
            // Return the file target for the photo based on filename
            File file = new File(mediaStorageDir.getPath() + File.separator + fileName);
            // wrap File object into a content provider
            return FileProvider.getUriForFile(MarkerDetailsActivity.this, "com.example.mapdemo.fileprovider", file);
        }
        return null;
    }

    // Returns true if external storage for photos is available
    private boolean isExternalStorageAvailable() {
        String state = Environment.getExternalStorageState();
        return state.equals(Environment.MEDIA_MOUNTED);
    }
}
