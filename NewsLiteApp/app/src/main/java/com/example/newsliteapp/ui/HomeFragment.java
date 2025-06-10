package com.example.newsliteapp.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.view.ContextThemeWrapper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.newsliteapp.R;
import com.example.newsliteapp.activity.DetailActivity;
import com.example.newsliteapp.activity.MainActivity;
import com.example.newsliteapp.adapter.NewsAdapter;
import com.example.newsliteapp.db.DatabaseHelper;
import com.example.newsliteapp.model.NewsApiResponse;
import com.example.newsliteapp.model.NewsArticle;
import com.example.newsliteapp.network.ApiService;
import com.example.newsliteapp.network.RetrofitClient;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment implements NewsAdapter.OnArticleInteractionListener, SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "HomeFragment";
    public static final int DETAIL_ACTIVITY_REQUEST_CODE = 1001;

    private RecyclerView recyclerViewHome;
    private NewsAdapter newsAdapter;
    private List<NewsArticle> articleList = new ArrayList<>();
    private List<NewsArticle> originalArticleList = new ArrayList<>();
    private ProgressBar progressBarHome;
    private TextView textViewInfoHome;
    private LinearLayout loadingIndicatorLayout;
    private SearchView searchView;
    private DatabaseHelper databaseHelper;
    private ApiService apiService;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ChipGroup chipGroupCategories;

    private final List<String> categories = Arrays.asList(
            "Terkini", "Nasional", "Internasional", "Ekonomi", "Olahraga", "Teknologi", "Hiburan", "Gaya-Hidup"
    );
    private String currentSelectedCategory = "Terkini";

    private ExecutorService databaseExecutor;
    private Handler uiHandler;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        // Inisialisasi Executor di sini (dipanggil sekali per instance fragment)
        databaseExecutor = Executors.newSingleThreadExecutor();
        uiHandler = new Handler(Looper.getMainLooper());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        Toolbar toolbar = root.findViewById(R.id.toolbar_home);
        if (getActivity() instanceof AppCompatActivity) {
            ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
            if (((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
                ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.app_name);
            }
        }

        recyclerViewHome = root.findViewById(R.id.recycler_view_home);
        loadingIndicatorLayout = root.findViewById(R.id.loading_indicator_layout);
        progressBarHome = root.findViewById(R.id.progress_bar_home);
        textViewInfoHome = root.findViewById(R.id.text_view_info_home);
        searchView = root.findViewById(R.id.search_view);
        swipeRefreshLayout = root.findViewById(R.id.swipe_refresh_layout_home);
        chipGroupCategories = root.findViewById(R.id.chip_group_categories);

        databaseHelper = new DatabaseHelper(getContext());
        apiService = RetrofitClient.getClient().create(ApiService.class);

        // ExecutorService dan Handler sudah diinisialisasi di onCreate

        setupRecyclerView();
        setupSearchView();
        setupSwipeRefresh();
        setupCategoryChips();

        if (isNetworkAvailable()) {
            loadNewsDataForCategory(currentSelectedCategory);
        } else {
            showError(getString(R.string.error_no_internet));
        }

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
                    darkModeItem.setIcon(R.drawable.ic_light_mode_24);
                    darkModeItem.setTitle("Light Mode");
                } else {
                    darkModeItem.setIcon(R.drawable.ic_dark_mode_24);
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

                // This will trigger onPrepareOptionsMenu to update the icon
                getActivity().invalidateOptionsMenu();

                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupRecyclerView() {
        newsAdapter = new NewsAdapter(getContext(), articleList, this, this);
        recyclerViewHome.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewHome.setAdapter(newsAdapter);
        recyclerViewHome.setHasFixedSize(true);
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterNews(query);
                searchView.clearFocus();
                return true;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                filterNews(newText);
                return true;
            }
        });
        ImageView closeButton = searchView.findViewById(androidx.appcompat.R.id.search_close_btn);
        if (closeButton != null) {
            closeButton.setOnClickListener(v -> {
                searchView.setQuery("", false);
                searchView.clearFocus();
                filterNews("");
            });
        }
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary, R.color.colorAccent, R.color.colorPrimaryDark);
        swipeRefreshLayout.setOnRefreshListener(this);
    }

    private void setupCategoryChips() {
        Context context = getContext();
        if (context == null || chipGroupCategories == null) {
            Log.e(TAG, "Context atau ChipGroup null di setupCategoryChips.");
            return;
        }
        chipGroupCategories.removeAllViews();
        ColorStateList chipBackgroundColorStateList = ContextCompat.getColorStateList(context, R.color.chip_background_color_selector);
        ColorStateList chipTextColorStateList = ContextCompat.getColorStateList(context, R.color.chip_text_color_selector);
        for (String categoryName : categories) {
            Chip chip = new Chip(new ContextThemeWrapper(context, com.google.android.material.R.style.Widget_MaterialComponents_Chip_Choice));
            chip.setText(categoryName);
            chip.setCheckable(true);
            chip.setClickable(true);
            chip.setChipBackgroundColor(chipBackgroundColorStateList);
            chip.setTextColor(chipTextColorStateList);
            chip.setChipStrokeWidth(0);
            if (categoryName.equals(currentSelectedCategory)) {
                chip.setChecked(true);
            }
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    currentSelectedCategory = buttonView.getText().toString();
                    originalArticleList.clear();
                    articleList.clear();
                    if(newsAdapter != null) newsAdapter.notifyDataSetChanged();
                    searchView.setQuery("", false);
                    searchView.clearFocus();
                    if (isNetworkAvailable()) {
                        loadNewsDataForCategory(currentSelectedCategory);
                    } else {
                        showError(getString(R.string.error_no_internet));
                    }
                }
            });
            chipGroupCategories.addView(chip);
        }
    }

    @Override
    public void onRefresh() {
        if (isNetworkAvailable()) {
            loadNewsDataForCategory(currentSelectedCategory);
        } else {
            showError(getString(R.string.error_no_internet));
            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
        }
    }

    private void loadNewsDataForCategory(String category) {
        if (databaseExecutor == null || databaseExecutor.isShutdown()) {
            Log.w(TAG, "Database executor tidak tersedia, membatalkan loadNewsDataForCategory.");
            return;
        }

        showLoading(true, category);
        Call<NewsApiResponse> call;
        if (category.equalsIgnoreCase("Terkini")) {
            call = apiService.getLatestCnnNews();
        } else {
            call = apiService.getNewsByCategory(category.toLowerCase(Locale.ROOT));
        }
        call.enqueue(new Callback<NewsApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<NewsApiResponse> call, @NonNull Response<NewsApiResponse> response) {
                if (!isAdded()) return;
                showLoading(false, null);
                if (response.isSuccessful()) {
                    NewsApiResponse apiResponse = response.body();
                    if (apiResponse != null && apiResponse.getArticles() != null) {
                        List<NewsArticle> fetchedArticles = apiResponse.getArticles();

                        databaseExecutor.execute(() -> {
                            if (!isAdded()) return;
                            for (NewsArticle article : fetchedArticles) {
                                if (article.getUrl() != null) {
                                    article.setSaved(databaseHelper.isArticleSaved(article.getUrl()));
                                    if (article.getDbId() == null || article.getDbId().isEmpty()) {
                                        article.setDbId(article.getUrl());
                                    }
                                }
                            }
                            uiHandler.post(() -> {
                                if (!isAdded()) return;
                                originalArticleList.clear();
                                originalArticleList.addAll(fetchedArticles);
                                String currentSearchQuery = searchView.getQuery().toString();
                                filterNews(currentSearchQuery);
                                if (articleList.isEmpty() && !TextUtils.isEmpty(currentSearchQuery)) {
                                    showError(getString(R.string.no_news_found) + " untuk '" + currentSearchQuery + "'");
                                } else if (articleList.isEmpty() && fetchedArticles.isEmpty()) {
                                    showError(getString(R.string.no_news_found));
                                }
                            });
                        });
                    } else {
                        String msg = getString(R.string.error_failed_to_load_news) + " (Respons tidak valid dari server)";
                        showError(msg);
                    }
                } else {
                    String errorMessage = getString(R.string.error_failed_to_load_news) + " (Error: " + response.code() + ")";
                    showError(errorMessage);
                }
            }
            @Override
            public void onFailure(@NonNull Call<NewsApiResponse> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                showLoading(false, null);
                showError(getString(R.string.error_failed_to_load_news));
                Log.e(TAG, "API call failed: ", t);
            }
        });
    }

    private void filterNews(String text) {
        List<NewsArticle> filteredList = new ArrayList<>();
        if (TextUtils.isEmpty(text)) {
            filteredList.addAll(originalArticleList);
        } else {
            String filterPattern = text.toLowerCase(Locale.getDefault()).trim();
            for (NewsArticle item : originalArticleList) {
                boolean matches = false;
                if (item.getTitle() != null && item.getTitle().toLowerCase(Locale.getDefault()).contains(filterPattern)) {
                    matches = true;
                }
                if (!matches && item.getDescription() != null && item.getDescription().toLowerCase(Locale.getDefault()).contains(filterPattern)) {
                    matches = true;
                }
                if (matches) {
                    filteredList.add(item);
                }
            }
        }
        articleList.clear();
        articleList.addAll(filteredList);
        if(newsAdapter != null) newsAdapter.updateData(new ArrayList<>(articleList));
        if (loadingIndicatorLayout == null || recyclerViewHome == null || textViewInfoHome == null) return;
        if (articleList.isEmpty()) {
            if (!TextUtils.isEmpty(text)) {
                textViewInfoHome.setText(getString(R.string.no_news_found) + " untuk '" + text + "'");
            } else if (originalArticleList.isEmpty()) {
                textViewInfoHome.setText(getString(R.string.no_news_found));
            } else {
                textViewInfoHome.setText(getString(R.string.no_news_found));
            }
            loadingIndicatorLayout.setVisibility(View.VISIBLE);
            progressBarHome.setVisibility(View.GONE);
            textViewInfoHome.setVisibility(View.VISIBLE);
            recyclerViewHome.setVisibility(View.GONE);
        } else {
            loadingIndicatorLayout.setVisibility(View.GONE);
            recyclerViewHome.setVisibility(View.VISIBLE);
        }
    }

    private void showLoading(boolean isLoading, @Nullable String categoryName) {
        if (loadingIndicatorLayout == null || progressBarHome == null || textViewInfoHome == null || recyclerViewHome == null) return;
        if (isLoading) {
            loadingIndicatorLayout.setVisibility(View.VISIBLE);
            progressBarHome.setVisibility(View.VISIBLE);
            recyclerViewHome.setVisibility(View.GONE);
            if (categoryName != null) {
                textViewInfoHome.setText(getString(R.string.loading_news_category, categoryName));
            } else {
                textViewInfoHome.setText(getString(R.string.loading_news));
            }
            textViewInfoHome.setVisibility(View.VISIBLE);
        } else {
            loadingIndicatorLayout.setVisibility(View.GONE);
            if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
        }
    }

    private void showError(String message) {
        if (loadingIndicatorLayout == null || progressBarHome == null || textViewInfoHome == null || recyclerViewHome == null) return;
        loadingIndicatorLayout.setVisibility(View.VISIBLE);
        progressBarHome.setVisibility(View.GONE);
        recyclerViewHome.setVisibility(View.GONE);
        textViewInfoHome.setText(message);
        textViewInfoHome.setVisibility(View.VISIBLE);
        if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private boolean isNetworkAvailable() {
        if (getContext() == null) return false;
        ConnectivityManager connectivityManager = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            try {
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                return activeNetworkInfo != null && activeNetworkInfo.isConnected();
            } catch (Exception e) {
                Log.e(TAG, "Error saat memeriksa status jaringan: " + e.getMessage());
                return false;
            }
        }
        return false;
    }

    @Override
    public void onArticleSaved(NewsArticle article, boolean isNowSaved, int position) {
        if (databaseExecutor.isShutdown()) return;
        databaseExecutor.execute(() -> {
            boolean found = false;
            for (NewsArticle originalArticle : originalArticleList) {
                if (originalArticle.getUrl() != null && originalArticle.getUrl().equals(article.getUrl())) {
                    originalArticle.setSaved(isNowSaved);
                    found = true;
                    break;
                }
            }
            if(found) Log.d(TAG, "Status simpan artikel di original list diperbarui: " + article.getTitle());
        });
    }

    @Override
    public void onArticleClicked(NewsArticle article) {
        Intent intent = new Intent(getContext(), DetailActivity.class);
        intent.putExtra(DetailActivity.EXTRA_NEWS_ARTICLE, article);
        startActivityForResult(intent, DETAIL_ACTIVITY_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == DETAIL_ACTIVITY_REQUEST_CODE && resultCode == AppCompatActivity.RESULT_OK && data != null) {
            String articleUrl = data.getStringExtra("article_url");
            boolean isSaved = data.getBooleanExtra("is_saved", false);
            if (articleUrl != null) {
                if (databaseExecutor.isShutdown()) return;
                databaseExecutor.execute(() -> {
                    boolean listChanged = false;
                    for (NewsArticle originalArticle : originalArticleList) {
                        if (originalArticle.getUrl() != null && originalArticle.getUrl().equals(articleUrl)) {
                            if (originalArticle.isSaved() != isSaved) {
                                originalArticle.setSaved(isSaved);
                                listChanged = true;
                            }
                            break;
                        }
                    }
                    final boolean finalHasListChanged = listChanged;
                    uiHandler.post(() -> {
                        boolean adapterItemUpdated = false;
                        for (int i = 0; i < articleList.size(); i++) {
                            NewsArticle articleInCurrentList = articleList.get(i);
                            if (articleInCurrentList.getUrl() != null && articleInCurrentList.getUrl().equals(articleUrl)) {
                                if (articleInCurrentList.isSaved() != isSaved) {
                                    articleInCurrentList.setSaved(isSaved);
                                    if(newsAdapter != null) newsAdapter.notifyItemChanged(i);
                                    adapterItemUpdated = true;
                                }
                                break;
                            }
                        }
                        if (finalHasListChanged || adapterItemUpdated) {
                            Log.d(TAG, "Status simpan artikel diperbarui dari DetailActivity untuk URL: " + articleUrl);
                        }
                    });
                });
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (newsAdapter != null && !articleList.isEmpty()) {
            if (databaseExecutor.isShutdown()) {
                databaseExecutor = Executors.newSingleThreadExecutor();
            }
            databaseExecutor.execute(() -> {
                final List<NewsArticle> tempArticleList = new ArrayList<>(articleList);
                boolean listChangedOverall = false;
                List<Integer> changedPositions = new ArrayList<>();
                for (int i = 0; i < tempArticleList.size(); i++) {
                    NewsArticle article = tempArticleList.get(i);
                    if (article.getUrl() != null) {
                        boolean currentDbStatus = databaseHelper.isArticleSaved(article.getUrl());
                        if (article.isSaved() != currentDbStatus) {
                            article.setSaved(currentDbStatus);
                            changedPositions.add(i);
                            listChangedOverall = true;
                        }
                    }
                }
                for (NewsArticle originalArticle : originalArticleList) {
                    if (originalArticle.getUrl() != null) {
                        boolean currentDbStatus = databaseHelper.isArticleSaved(originalArticle.getUrl());
                        if (originalArticle.isSaved() != currentDbStatus) {
                            originalArticle.setSaved(currentDbStatus);
                        }
                    }
                }
                if (listChangedOverall) {
                    uiHandler.post(() -> {
                        articleList.clear();
                        articleList.addAll(tempArticleList);
                        for (Integer position : changedPositions) {
                            if(newsAdapter != null) newsAdapter.notifyItemChanged(position);
                        }
                        Log.d(TAG, "Status simpan artikel di-refresh pada onResume.");
                    });
                }
            });
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (databaseExecutor != null && !databaseExecutor.isShutdown()) {
            databaseExecutor.shutdown();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (uiHandler != null) {
            uiHandler.removeCallbacksAndMessages(null);
        }
        recyclerViewHome = null;
        newsAdapter = null;
        progressBarHome = null;
        textViewInfoHome = null;
        loadingIndicatorLayout = null;
        searchView = null;
        swipeRefreshLayout = null;
        chipGroupCategories = null;
    }
}