package com.example.newsliteapp.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.example.newsliteapp.R;
import com.example.newsliteapp.db.DatabaseHelper;
import com.example.newsliteapp.model.NewsArticle;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.button.MaterialButton;
import com.squareup.picasso.Picasso;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DetailActivity extends AppCompatActivity {

    public static final String EXTRA_NEWS_ARTICLE = "extra_news_article";
    private static final String TAG = "DetailActivity";

    private ImageView imageViewBanner;
    private TextView textViewTitle, textViewDate, textViewDescription;
    private MaterialButton buttonSaveToggle, buttonShare, buttonReadFullStory;
    private CollapsingToolbarLayout collapsingToolbarLayout;

    private NewsArticle currentArticle;
    private DatabaseHelper databaseHelper;
    private boolean isArticleCurrentlySavedUi;

    private ExecutorService databaseExecutor;
    private Handler uiHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        Toolbar toolbar = findViewById(R.id.toolbar_detail);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        databaseHelper = new DatabaseHelper(this);
        databaseExecutor = Executors.newSingleThreadExecutor();
        uiHandler = new Handler(Looper.getMainLooper());

        initViews();

        currentArticle = getIntent().getParcelableExtra(EXTRA_NEWS_ARTICLE);

        if (currentArticle != null) {
            populateUI();
            checkIfArticleIsSavedBackground();
            setupButtonListeners();
        } else {
            Toast.makeText(this, "Gagal memuat detail berita.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "NewsArticle tidak diterima melalui Intent.");
            finish();
        }
    }

    private void initViews() {
        imageViewBanner = findViewById(R.id.image_view_detail_banner);
        collapsingToolbarLayout = findViewById(R.id.collapsing_toolbar_detail);
        textViewTitle = findViewById(R.id.text_view_detail_title);
        textViewDate = findViewById(R.id.text_view_detail_date);
        textViewDescription = findViewById(R.id.text_view_detail_description);
        buttonSaveToggle = findViewById(R.id.button_detail_save_toggle);
        buttonShare = findViewById(R.id.button_detail_share);
        buttonReadFullStory = findViewById(R.id.button_read_full_story);
    }

    private void populateUI() {
        if (currentArticle == null) return;

        collapsingToolbarLayout.setTitle(currentArticle.getTitle());
        textViewTitle.setText(currentArticle.getTitle());
        textViewDate.setText(formatDate(currentArticle.getPublishedAt()));
        textViewDescription.setText(currentArticle.getDescription());

        String imageUrl = currentArticle.getImageUrl();
        if (!TextUtils.isEmpty(imageUrl)) {
            Picasso.get()
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_image_placeholder_24)
                    .error(R.drawable.ic_broken_image_24)
                    .into(imageViewBanner);
        } else {
            imageViewBanner.setImageResource(R.drawable.ic_image_placeholder_24);
        }
    }

    private void checkIfArticleIsSavedBackground() {
        if (currentArticle != null && currentArticle.getUrl() != null) {
            if(databaseExecutor.isShutdown()) databaseExecutor = Executors.newSingleThreadExecutor();
            databaseExecutor.execute(() -> {
                final boolean isSavedDb = databaseHelper.isArticleSaved(currentArticle.getUrl());
                uiHandler.post(() -> {
                    isArticleCurrentlySavedUi = isSavedDb;
                    if(currentArticle != null) currentArticle.setSaved(isArticleCurrentlySavedUi);
                    updateSaveButtonUI();
                });
            });
        } else {
            isArticleCurrentlySavedUi = false;
            updateSaveButtonUI();
        }
    }

    private void setupButtonListeners() {
        buttonSaveToggle.setOnClickListener(v -> toggleSaveArticleBackground());
        buttonShare.setOnClickListener(v -> shareArticle());
        buttonReadFullStory.setOnClickListener(v -> readFullStory());
    }

    private void toggleSaveArticleBackground() {
        if (currentArticle == null || currentArticle.getUrl() == null) {
            Toast.makeText(this, "URL Berita tidak valid.", Toast.LENGTH_SHORT).show();
            return;
        }
        if(databaseExecutor.isShutdown()) databaseExecutor = Executors.newSingleThreadExecutor();
        databaseExecutor.execute(() -> {
            final boolean wasSaved = isArticleCurrentlySavedUi;
            boolean successOperation;
            String messageToast;

            if (wasSaved) {
                successOperation = databaseHelper.removeSavedArticle(currentArticle.getUrl());
                messageToast = successOperation ? getString(R.string.news_removed_from_saved) : getString(R.string.failed_to_remove_news);
            } else {
                if (currentArticle.getDbId() == null || currentArticle.getDbId().isEmpty()) {
                    currentArticle.setDbId(currentArticle.getUrl());
                }
                successOperation = databaseHelper.addSavedArticle(currentArticle);
                messageToast = successOperation ? getString(R.string.news_saved_successfully) : getString(R.string.failed_to_save_news);
            }

            final boolean newSavedStatusFromDb = successOperation ? !wasSaved : wasSaved;

            uiHandler.post(() -> {
                Toast.makeText(DetailActivity.this, messageToast, Toast.LENGTH_SHORT).show();
                if (successOperation) {
                    isArticleCurrentlySavedUi = newSavedStatusFromDb;
                    if(currentArticle != null) currentArticle.setSaved(isArticleCurrentlySavedUi);
                    updateSaveButtonUI();

                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("article_url", currentArticle.getUrl());
                    resultIntent.putExtra("is_saved", isArticleCurrentlySavedUi);
                    setResult(RESULT_OK, resultIntent);
                }
            });
        });
    }

    private void updateSaveButtonUI() {
        if (buttonSaveToggle == null) return;
        if (isArticleCurrentlySavedUi) {
            buttonSaveToggle.setText(R.string.action_unsave);
            buttonSaveToggle.setIconResource(R.drawable.ic_save_filled_24);
        } else {
            buttonSaveToggle.setText(R.string.action_save);
            buttonSaveToggle.setIconResource(R.drawable.ic_save_outline_24);
        }
    }

    private void shareArticle() {
        if (currentArticle != null && currentArticle.getUrl() != null && !currentArticle.getUrl().isEmpty()) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            String shareBody = currentArticle.getTitle() + "\n" + currentArticle.getUrl();
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, currentArticle.getTitle());
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
            startActivity(Intent.createChooser(shareIntent, getString(R.string.action_share)));
        } else {
            Toast.makeText(this, "Tidak ada URL untuk dibagikan.", Toast.LENGTH_SHORT).show();
        }
    }

    private void readFullStory() {
        if (currentArticle != null && currentArticle.getUrl() != null && !currentArticle.getUrl().isEmpty()) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(currentArticle.getUrl()));
            try {
                startActivity(browserIntent);
            } catch (Exception e) {
                Toast.makeText(this, "Tidak dapat membuka link berita.", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error membuka URL: " + currentArticle.getUrl(), e);
            }
        } else {
            Toast.makeText(this, "URL berita tidak tersedia.", Toast.LENGTH_SHORT).show();
        }
    }

    private String formatDate(String isoDateString) {
        if (isoDateString == null || isoDateString.isEmpty()) return "N/A";
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
        inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.forLanguageTag("id-ID"));
        outputFormat.setTimeZone(TimeZone.getDefault());
        try {
            Date date = inputFormat.parse(isoDateString);
            return outputFormat.format(date);
        } catch (ParseException e) {
            return isoDateString.substring(0, Math.min(isoDateString.length(), 10)); // Fallback
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (currentArticle != null && currentArticle.getUrl() != null) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("article_url", currentArticle.getUrl());
            resultIntent.putExtra("is_saved", isArticleCurrentlySavedUi);
            setResult(RESULT_OK, resultIntent);
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (databaseExecutor != null && !databaseExecutor.isShutdown()) {
            databaseExecutor.shutdown();
        }
        if (uiHandler != null) {
            uiHandler.removeCallbacksAndMessages(null);
        }
    }
}
