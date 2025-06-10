package com.example.newsliteapp.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.newsliteapp.R;
import com.example.newsliteapp.activity.DetailActivity;
import com.example.newsliteapp.db.DatabaseHelper;
import com.example.newsliteapp.model.NewsArticle;
import com.example.newsliteapp.ui.HomeFragment; // Import HomeFragment
import com.example.newsliteapp.ui.SavedFragment; // Import SavedFragment
import com.google.android.material.button.MaterialButton;
import com.squareup.picasso.Picasso;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;
import android.os.Looper;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.NewsViewHolder> {

    private static final String ADAPTER_TAG = "NewsAdapter";
    private Context context;
    private List<NewsArticle> articleList;
    private DatabaseHelper databaseHelper;
    private OnArticleInteractionListener interactionListener;
    private Fragment fragmentHost;

    private final ExecutorService databaseExecutor;
    private final Handler uiHandler;

    public interface OnArticleInteractionListener {
        void onArticleSaved(NewsArticle article, boolean isSaved, int position);
        void onArticleClicked(NewsArticle article);
    }

    public NewsAdapter(Context context, List<NewsArticle> articleList, OnArticleInteractionListener listener, Fragment fragment) {
        this.context = context;
        this.articleList = articleList;
        this.databaseHelper = new DatabaseHelper(context);
        this.interactionListener = listener;
        this.fragmentHost = fragment;
        this.databaseExecutor = Executors.newSingleThreadExecutor();
        this.uiHandler = new Handler(Looper.getMainLooper());
    }
    public NewsAdapter(Context context, List<NewsArticle> articleList, OnArticleInteractionListener listener) {
        this(context, articleList, listener, null);
    }


    @NonNull
    @Override
    public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_news, parent, false);
        return new NewsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NewsViewHolder holder, int position) {
        NewsArticle article = articleList.get(position);

        holder.textViewTitle.setText(article.getTitle());

        if (!TextUtils.isEmpty(article.getDescription())) {
            holder.textViewDescription.setText(article.getDescription());
            holder.textViewDescription.setVisibility(View.VISIBLE);
        } else {
            holder.textViewDescription.setVisibility(View.GONE);
        }

        holder.textViewSource.setText(context.getString(R.string.text_source_cnn_label));
        holder.textViewDate.setText(formatDate(article.getPublishedAt()));

        String imageUrl = article.getImageUrl();
        if (!TextUtils.isEmpty(imageUrl)) {
            Picasso.get()
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_image_placeholder_24)
                    .error(R.drawable.ic_broken_image_24)
                    .fit()
                    .centerCrop()
                    .into(holder.imageViewBanner);
        } else {
            holder.imageViewBanner.setImageResource(R.drawable.ic_image_placeholder_24);
        }

        if (article.getUrl() != null) {
            databaseExecutor.execute(() -> {
                final boolean isSaved = databaseHelper.isArticleSaved(article.getUrl());
                uiHandler.post(() -> {
                    if (holder.getAdapterPosition() == position) {
                        article.setSaved(isSaved);
                        if (holder.buttonSaveToggle instanceof MaterialButton) {
                            updateSaveButtonUI((MaterialButton) holder.buttonSaveToggle, article.isSaved());
                        } else {
                            updateSaveButtonUI(holder.buttonSaveToggle, article.isSaved());
                        }
                    }
                });
            });
        } else {
            article.setSaved(false);
            if (holder.buttonSaveToggle instanceof MaterialButton) {
                updateSaveButtonUI((MaterialButton) holder.buttonSaveToggle, article.isSaved());
            } else {
                updateSaveButtonUI(holder.buttonSaveToggle, article.isSaved());
            }
        }

        holder.buttonSaveToggle.setOnClickListener(v -> {
            if (article.getUrl() == null || article.getUrl().isEmpty()) {
                Toast.makeText(context, "URL Berita tidak valid.", Toast.LENGTH_SHORT).show();
                return;
            }
            databaseExecutor.execute(() -> {
                boolean wasSaved = article.isSaved();
                boolean successOperation;
                String messageToast;

                if (wasSaved) {
                    successOperation = databaseHelper.removeSavedArticle(article.getUrl());
                    if (successOperation) {
                        article.setSaved(false);
                        messageToast = context.getString(R.string.news_removed_from_saved);
                    } else {
                        messageToast = context.getString(R.string.failed_to_remove_news);
                    }
                } else {
                    if (article.getDbId() == null || article.getDbId().isEmpty()){
                        article.setDbId(article.getUrl());
                    }
                    successOperation = databaseHelper.addSavedArticle(article);
                    if (successOperation) {
                        article.setSaved(true);
                        messageToast = context.getString(R.string.news_saved_successfully);
                    } else {
                        messageToast = context.getString(R.string.failed_to_save_news);
                    }
                }

                final boolean finalSuccessOperation = successOperation;
                final String finalMessageToast = messageToast;
                final boolean finalIsNowSaved = article.isSaved();

                uiHandler.post(() -> {
                    Toast.makeText(context, finalMessageToast, Toast.LENGTH_SHORT).show();
                    if (finalSuccessOperation) {
                        if (holder.getAdapterPosition() != RecyclerView.NO_POSITION) {
                            if (holder.buttonSaveToggle instanceof MaterialButton) {
                                updateSaveButtonUI((MaterialButton) holder.buttonSaveToggle, finalIsNowSaved);
                            } else {
                                updateSaveButtonUI(holder.buttonSaveToggle, finalIsNowSaved);
                            }
                        }
                        if (interactionListener != null) {
                            interactionListener.onArticleSaved(article, finalIsNowSaved, holder.getAdapterPosition());
                        }
                    }
                });
            });
        });

        holder.buttonShare.setOnClickListener(v -> {
            if (article.getUrl() != null && !article.getUrl().isEmpty()) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                String shareBody = article.getTitle() + "\n" + article.getUrl();
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, article.getTitle());
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
                context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.action_share)));
            } else {
                Toast.makeText(context, "Tidak ada URL untuk dibagikan.", Toast.LENGTH_SHORT).show();
            }
        });

        holder.itemView.setOnClickListener(v -> {
            openDetailActivity(article);
        });
    }

    private void openDetailActivity(NewsArticle article) {
        Intent intent = new Intent(context, DetailActivity.class);
        intent.putExtra(DetailActivity.EXTRA_NEWS_ARTICLE, article);

        if (fragmentHost != null) {
            if (fragmentHost instanceof HomeFragment) {
                fragmentHost.startActivityForResult(intent, HomeFragment.DETAIL_ACTIVITY_REQUEST_CODE);
            } else if (fragmentHost instanceof SavedFragment) {
                fragmentHost.startActivityForResult(intent, SavedFragment.DETAIL_ACTIVITY_REQUEST_CODE_SAVED);
            } else {
                // Fallback jika fragmentHost bukan tipe yang dikenal, atau gunakan request code default
                Log.w(ADAPTER_TAG, "fragmentHost bukan HomeFragment atau SavedFragment, menggunakan startActivity biasa.");
                fragmentHost.startActivity(intent);
            }
        } else if (context instanceof Activity) {
            // Jika konteks adalah Activity, kita mungkin tidak tahu request code yang tepat
            // atau apakah activity ini mengharapkan hasil. Lebih aman startActivity biasa.
            Log.w(ADAPTER_TAG, "Konteks adalah Activity, menggunakan startActivity biasa untuk DetailActivity.");
            ((Activity) context).startActivity(intent);
            // Jika Anda yakin Activity ini juga mengharapkan hasil dengan request code tertentu:
            // ((Activity) context).startActivityForResult(intent, SOME_DEFAULT_REQUEST_CODE);
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }


    private void updateSaveButtonUI(MaterialButton button, boolean isSaved) {
        Drawable icon = null;
        int iconResId = isSaved ? R.drawable.ic_save_filled_24 : R.drawable.ic_save_outline_24;
        String buttonText = isSaved ? context.getString(R.string.action_unsave) : context.getString(R.string.action_save);
        button.setText(buttonText);
        try {
            icon = ContextCompat.getDrawable(context, iconResId);
        } catch (Exception e) {
            Log.e(ADAPTER_TAG, "Error saat memuat drawable resource: " + iconResId, e);
        }
        button.setIcon(icon);
        ColorStateList colorStateList = null;
        try {
            colorStateList = ContextCompat.getColorStateList(context, R.color.colorPrimary);
        } catch (Exception e) {
            Log.e(ADAPTER_TAG, "Error saat memuat ColorStateList untuk R.color.colorPrimary", e);
        }
        button.setIconTint(colorStateList);
    }

    private void updateSaveButtonUI(Button button, boolean isSaved) {
        Drawable icon;
        int iconResId = isSaved ? R.drawable.ic_save_filled_24 : R.drawable.ic_save_outline_24;
        String buttonText = isSaved ? context.getString(R.string.action_unsave) : context.getString(R.string.action_save);
        button.setText(buttonText);
        try {
            icon = ContextCompat.getDrawable(context, iconResId);
            if (icon != null) {
                button.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, null, null, null);
            } else {
                button.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, null, null);
            }
        } catch (Exception e) {
            Log.e(ADAPTER_TAG, "Error saat memuat drawable (Button biasa) untuk resource: " + iconResId, e);
            button.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, null, null);
        }
    }

    private String formatDate(String isoDateString) {
        if (isoDateString == null || isoDateString.isEmpty()) return "N/A";
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
        inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yy, HH:mm", Locale.forLanguageTag("id-ID"));
        outputFormat.setTimeZone(TimeZone.getDefault());
        try {
            Date date = inputFormat.parse(isoDateString);
            return outputFormat.format(date);
        } catch (ParseException e) {
            Log.e(ADAPTER_TAG, "ParseException for date: " + isoDateString, e);
            try {
                SimpleDateFormat fallbackInputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
                fallbackInputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date date = fallbackInputFormat.parse(isoDateString);
                return outputFormat.format(date);
            } catch (ParseException ex) {
                Log.e(ADAPTER_TAG, "ParseException fallback for date: " + isoDateString, ex);
                if (isoDateString.contains("T")) return isoDateString.substring(0, isoDateString.indexOf("T"));
                return isoDateString;
            }
        }
    }

    @Override
    public int getItemCount() {
        return articleList != null ? articleList.size() : 0;
    }

    public void updateData(List<NewsArticle> newArticles) {
        this.articleList.clear();
        if (newArticles != null) {
            this.articleList.addAll(newArticles);
        }
        notifyDataSetChanged();
    }

    public void filterList(List<NewsArticle> filteredList) {
        this.articleList.clear();
        if (filteredList != null) {
            this.articleList.addAll(filteredList);
        }
        notifyDataSetChanged();
    }

    static class NewsViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewBanner;
        TextView textViewTitle, textViewDescription, textViewSource, textViewDate;
        Button buttonSaveToggle, buttonShare;
        public NewsViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewBanner = itemView.findViewById(R.id.image_view_news_banner);
            textViewTitle = itemView.findViewById(R.id.text_view_news_title);
            textViewDescription = itemView.findViewById(R.id.text_view_news_description);
            textViewSource = itemView.findViewById(R.id.text_view_news_source);
            textViewDate = itemView.findViewById(R.id.text_view_news_date);
            buttonSaveToggle = itemView.findViewById(R.id.button_save_toggle);
            buttonShare = itemView.findViewById(R.id.button_share);
        }
    }
}
