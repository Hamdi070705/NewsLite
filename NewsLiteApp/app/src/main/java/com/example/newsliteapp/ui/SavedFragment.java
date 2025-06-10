package com.example.newsliteapp.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.newsliteapp.R;
import com.example.newsliteapp.activity.DetailActivity;
import com.example.newsliteapp.activity.MainActivity;
import com.example.newsliteapp.adapter.NewsAdapter;
import com.example.newsliteapp.db.DatabaseHelper;
import com.example.newsliteapp.model.NewsArticle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SavedFragment extends Fragment implements NewsAdapter.OnArticleInteractionListener, SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "SavedFragment";
    public static final int DETAIL_ACTIVITY_REQUEST_CODE_SAVED = 1002;

    private RecyclerView recyclerViewSaved;
    private NewsAdapter newsAdapter;
    private List<NewsArticle> savedArticleList = new ArrayList<>();
    private TextView textViewInfoSaved;
    private DatabaseHelper databaseHelper;
    private SwipeRefreshLayout swipeRefreshLayoutSaved;

    private ExecutorService databaseExecutor;
    private Handler uiHandler;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        // Inisialisasi Executor di sini agar siklus hidupnya sama dengan Fragment
        databaseExecutor = Executors.newSingleThreadExecutor();
        uiHandler = new Handler(Looper.getMainLooper());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_saved, container, false);

        Toolbar toolbar = root.findViewById(R.id.toolbar_saved);
        if (getActivity() instanceof AppCompatActivity) {
            ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
            if (((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
                ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.title_saved);
            }
        }

        recyclerViewSaved = root.findViewById(R.id.recycler_view_saved);
        textViewInfoSaved = root.findViewById(R.id.text_view_info_saved);
        swipeRefreshLayoutSaved = root.findViewById(R.id.swipe_refresh_layout_saved);

        Context context = getContext();
        if (context != null) {
            databaseHelper = new DatabaseHelper(context);
        } else {
            Log.e(TAG, "Context is null, DatabaseHelper tidak bisa diinisialisasi.");
        }

        setupRecyclerView();
        setupSwipeRefresh();

        return root;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.toolbar_menu, menu);
        updateDarkModeIcon(menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);
        updateDarkModeIcon(menu);
    }

    private void updateDarkModeIcon(@NonNull Menu menu) {
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            boolean isDarkMode = mainActivity.isDarkMode();

            MenuItem darkModeItem = menu.findItem(R.id.action_dark_mode);
            if (darkModeItem != null) {
                if (isDarkMode) {
                    darkModeItem.setIcon(R.drawable.ic_dark_mode_24);
                    darkModeItem.setTitle("Light Mode");
                } else {
                    darkModeItem.setIcon(R.drawable.ic_light_mode_24);
                    darkModeItem.setTitle("Dark Mode");
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_dark_mode) {
            if (getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();
                mainActivity.toggleDarkMode();

                // Trigger onPrepareOptionsMenu untuk update icon
                getActivity().invalidateOptionsMenu();

                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupRecyclerView() {
        if (getContext() == null) return;
        newsAdapter = new NewsAdapter(getContext(), savedArticleList, this, this);
        recyclerViewSaved.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewSaved.setAdapter(newsAdapter);
        recyclerViewSaved.setHasFixedSize(true);
    }

    private void setupSwipeRefresh() {
        if (swipeRefreshLayoutSaved == null) return;
        swipeRefreshLayoutSaved.setColorSchemeResources(R.color.colorPrimary, R.color.colorAccent, R.color.colorPrimaryDark);
        swipeRefreshLayoutSaved.setOnRefreshListener(this);
    }

    @Override
    public void onRefresh() {
        loadSavedArticles();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadSavedArticles();
    }

    private void loadSavedArticles() {
        // PERBAIKAN: Tambahkan guard clause di awal
        if (databaseExecutor == null || databaseExecutor.isShutdown()) {
            Log.w(TAG, "Database executor tidak tersedia atau sudah dimatikan, membatalkan loadSavedArticles.");
            if (swipeRefreshLayoutSaved != null) swipeRefreshLayoutSaved.setRefreshing(false);
            return;
        }

        if (databaseHelper == null) {
            Log.e(TAG, "DatabaseHelper is null, tidak bisa memuat artikel tersimpan.");
            if (swipeRefreshLayoutSaved != null) swipeRefreshLayoutSaved.setRefreshing(false);
            showError(getString(R.string.error_database_unavailable)); // Anda perlu string ini
            return;
        }

        if (swipeRefreshLayoutSaved != null) swipeRefreshLayoutSaved.setRefreshing(true);
        if (textViewInfoSaved != null) textViewInfoSaved.setVisibility(View.GONE);

        databaseExecutor.execute(() -> {
            if (!isAdded()) return;
            final List<NewsArticle> articlesFromDb = databaseHelper.getAllSavedArticles();
            uiHandler.post(() -> {
                if (!isAdded()) return;
                if (swipeRefreshLayoutSaved != null) swipeRefreshLayoutSaved.setRefreshing(false);

                savedArticleList.clear();
                if (articlesFromDb != null) {
                    savedArticleList.addAll(articlesFromDb);
                }

                if (newsAdapter != null) newsAdapter.updateData(new ArrayList<>(savedArticleList));

                updateEmptyState();
            });
        });
    }

    private void updateEmptyState() {
        if (textViewInfoSaved == null || recyclerViewSaved == null) return;

        if (savedArticleList.isEmpty()) {
            textViewInfoSaved.setText(R.string.no_saved_news);
            textViewInfoSaved.setVisibility(View.VISIBLE);
            recyclerViewSaved.setVisibility(View.GONE);
        } else {
            textViewInfoSaved.setVisibility(View.GONE);
            recyclerViewSaved.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onArticleSaved(NewsArticle article, boolean isNowSaved, int position) {
        if (!isNowSaved) {
            boolean removed = false;
            for (int i = 0; i < savedArticleList.size(); i++) {
                if (savedArticleList.get(i).getUrl() != null && savedArticleList.get(i).getUrl().equals(article.getUrl())) {
                    savedArticleList.remove(i);
                    if (newsAdapter != null) {
                        newsAdapter.notifyItemRemoved(i);
                    }
                    removed = true;
                    break;
                }
            }
            if (removed) {
                updateEmptyState();
            } else {
                loadSavedArticles();
            }
        }
    }

    @Override
    public void onArticleClicked(NewsArticle article) {
        if (getContext() == null) return;
        Intent intent = new Intent(getContext(), DetailActivity.class);
        intent.putExtra(DetailActivity.EXTRA_NEWS_ARTICLE, article);
        startActivityForResult(intent, DETAIL_ACTIVITY_REQUEST_CODE_SAVED);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == DETAIL_ACTIVITY_REQUEST_CODE_SAVED && resultCode == AppCompatActivity.RESULT_OK && data != null) {
            String articleUrl = data.getStringExtra("article_url");
            boolean isSavedAfterDetail = data.getBooleanExtra("is_saved", false);

            if (articleUrl != null) {
                if (!isSavedAfterDetail) {
                    boolean itemRemoved = false;
                    for (int i = 0; i < savedArticleList.size(); i++) {
                        if (savedArticleList.get(i).getUrl() != null && savedArticleList.get(i).getUrl().equals(articleUrl)) {
                            savedArticleList.remove(i);
                            if(newsAdapter != null) newsAdapter.notifyItemRemoved(i);
                            itemRemoved = true;
                            break;
                        }
                    }
                    if (itemRemoved) {
                        updateEmptyState();
                    }
                }
            }
        }
    }

    private void showError(String message) {
        if (textViewInfoSaved == null || recyclerViewSaved == null) return;
        textViewInfoSaved.setText(message);
        textViewInfoSaved.setVisibility(View.VISIBLE);
        recyclerViewSaved.setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (uiHandler != null) {
            uiHandler.removeCallbacksAndMessages(null);
        }
        recyclerViewSaved = null;
        newsAdapter = null;
        textViewInfoSaved = null;
        swipeRefreshLayoutSaved = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (databaseExecutor != null && !databaseExecutor.isShutdown()) {
            databaseExecutor.shutdown();
        }
    }
}