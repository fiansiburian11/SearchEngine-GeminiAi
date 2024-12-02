package com.kel3.geminiai.fragment;

import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.aigemini.R;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.karumi.dexter.BuildConfig;

import java.io.File;
import java.io.IOException;

@RequiresApi(api = Build.VERSION_CODES.P)
public class FragmentImageRecognition extends Fragment {

    ImageView ivSearch;
    MaterialButton btnCapture, btnSubmit;
    TextView tvResult;
    EditText etUserInput;
    String imageFilePath;
    Bitmap selectedBitmap;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_image_recognition, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ivSearch = view.findViewById(R.id.ivSearch);
        btnCapture = view.findViewById(R.id.btnCapture);
        btnSubmit = view.findViewById(R.id.btnSubmit);
        tvResult = view.findViewById(R.id.tvResult);
        etUserInput = view.findViewById(R.id.etUserInput);

        btnCapture.setOnClickListener(v -> showPictureDialog());

        btnSubmit.setOnClickListener(v -> {
            if (selectedBitmap == null) {
                Toast.makeText(getContext(), "Pilih atau ambil gambar terlebih dahulu!", Toast.LENGTH_SHORT).show();
                return;
            }
            String userInput = etUserInput.getText().toString().trim();
            if (userInput.isEmpty()) {
                Toast.makeText(getContext(), "Masukkan pertanyaan terlebih dahulu", Toast.LENGTH_SHORT).show();
                return;
            }
            buttonImageRecognitionGemini(selectedBitmap, userInput);
        });
    }

    private void showPictureDialog() {
        AlertDialog.Builder pictureDialog = new AlertDialog.Builder(getContext());
        pictureDialog.setTitle("Upload Foto");
        String[] pictureDialogItems = {"Pilih Foto", "Ambil Foto Sekarang"};
        pictureDialog.setItems(pictureDialogItems, (dialog, which) -> {
            if (which == 0) {
                openGallery();
            } else {
                takePhoto();
            }
        });
        pictureDialog.show();
    }

    private void takePhoto() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = createImageFile();
        if (photoFile != null) {
            Uri photoURI = FileProvider.getUriForFile(getContext(), BuildConfig.APPLICATION_ID + ".provider", photoFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            startActivityForResult(intent, 1);
        }
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, 2);
    }

    private File createImageFile() {
        File storageDir = getActivity().getExternalFilesDir(null);
        File image = null;
        try {
            image = File.createTempFile("temp_image", ".jpg", storageDir);
            imageFilePath = image.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return image;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == 1) {
                selectedBitmap = BitmapFactory.decodeFile(imageFilePath);
            } else if (requestCode == 2) {
                Uri selectedImageUri = data.getData();
                try {
                    selectedBitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), selectedImageUri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            ivSearch.setImageBitmap(selectedBitmap);
        }
    }

    public void buttonImageRecognitionGemini(Bitmap bitmap, String userInput) {
        GenerativeModel generativeModel = new GenerativeModel("gemini-1.5-flash", "AIzaSyDu4nJvPWvE4RuITZco_tnP0_1GR7kdZ_M");
        GenerativeModelFutures modelFutures = GenerativeModelFutures.from(generativeModel);
        Content content = new Content.Builder().addText(userInput).addImage(bitmap).build();
        ListenableFuture<GenerateContentResponse> responseFuture = modelFutures.generateContent(content);

        Futures.addCallback(responseFuture, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                tvResult.setText(result.getText());
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                tvResult.setText("Gagal: " + t.getMessage());
            }
        }, getContext().getMainExecutor());
    }
}
