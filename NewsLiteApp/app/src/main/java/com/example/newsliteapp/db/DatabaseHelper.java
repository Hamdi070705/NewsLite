package com.example.newsliteapp.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.newsliteapp.model.NewsArticle;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "RuangBerita.db";
    // NAIKKAN VERSI DATABASE KARENA ADA PERUBAHAN SKEMA (menghapus kolom source_name)
    private static final int DATABASE_VERSION = 3; // Misalnya, jika versi sebelumnya 2

    public static final String TABLE_SAVED_ARTICLES = "saved_articles";

    public static final String COLUMN_DB_ID = "db_id"; // Primary Key, URL berita
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_URL = "url";
    public static final String COLUMN_IMAGE_URL_MAIN = "image_url_main";
    public static final String COLUMN_PUBLISHED_AT = "published_at"; // isoDate
    // COLUMN_SOURCE_NAME DIHAPUS
    public static final String COLUMN_DESCRIPTION = "description"; // contentSnippet
    public static final String COLUMN_CONTENT_FULL = "content_full";

    // SQL untuk membuat tabel (tanpa COLUMN_SOURCE_NAME)
    private static final String CREATE_TABLE_SAVED_ARTICLES =
            "CREATE TABLE " + TABLE_SAVED_ARTICLES + "(" +
                    COLUMN_DB_ID + " TEXT PRIMARY KEY," +
                    COLUMN_TITLE + " TEXT," +
                    COLUMN_URL + " TEXT UNIQUE," +
                    COLUMN_IMAGE_URL_MAIN + " TEXT," +
                    COLUMN_PUBLISHED_AT + " TEXT," +
                    // COLUMN_SOURCE_NAME + " TEXT," + // Baris ini dihapus
                    COLUMN_DESCRIPTION + " TEXT," +
                    COLUMN_CONTENT_FULL + " TEXT" +
                    ")";

    private static final String TAG = "DatabaseHelper";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_SAVED_ARTICLES);
        Log.d(TAG, "Database tables created");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                + newVersion + ", which will destroy all old data");
        // Cara sederhana untuk upgrade: hapus tabel lama dan buat baru
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SAVED_ARTICLES);
        onCreate(db);
        // Untuk aplikasi produksi, Anda mungkin ingin melakukan migrasi data yang lebih cermat
        // menggunakan ALTER TABLE jika memungkinkan, tapi untuk pengembangan ini cukup.
    }

    public boolean addSavedArticle(NewsArticle article) {
        if (article == null || article.getUrl() == null || article.getUrl().isEmpty()) {
            Log.e(TAG, "Cannot save article with null or empty URL");
            return false;
        }
        if (isArticleSaved(article.getUrl())) {
            Log.d(TAG, "Article already saved: " + article.getTitle());
            return true;
        }

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_DB_ID, article.getUrl());
        values.put(COLUMN_TITLE, article.getTitle());
        values.put(COLUMN_URL, article.getUrl());
        values.put(COLUMN_IMAGE_URL_MAIN, article.getImageUrl());
        values.put(COLUMN_PUBLISHED_AT, article.getPublishedAt());
        // values.put(COLUMN_SOURCE_NAME, article.getSourceName()); // Baris ini dihapus
        values.put(COLUMN_DESCRIPTION, article.getDescription());
        values.put(COLUMN_CONTENT_FULL, article.getContent());

        long result = -1;
        try {
            result = db.insertOrThrow(TABLE_SAVED_ARTICLES, null, values);
        } catch (Exception e) {
            Log.e(TAG, "Error inserting article: " + e.getMessage());
        } finally {
            db.close();
        }
        return result != -1;
    }

    public boolean removeSavedArticle(String articleUrl) {
        if (articleUrl == null || articleUrl.isEmpty()) {
            return false;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        int result = 0;
        try {
            result = db.delete(TABLE_SAVED_ARTICLES, COLUMN_URL + " = ?", new String[]{articleUrl});
        } catch (Exception e) {
            Log.e(TAG, "Error deleting article: " + e.getMessage());
        } finally {
            db.close();
        }
        return result > 0;
    }

    public List<NewsArticle> getAllSavedArticles() {
        List<NewsArticle> articleList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_SAVED_ARTICLES, null, null, null, null, null, COLUMN_PUBLISHED_AT + " DESC");
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    NewsArticle article = new NewsArticle();
                    article.setDbId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DB_ID)));
                    article.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)));
                    article.setUrl(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_URL)));

                    String mainImageUrl = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_URL_MAIN));
                    if (mainImageUrl != null) { // Tambahkan pengecekan null untuk image
                        NewsArticle.Image image = new NewsArticle.Image();
                        image.setLarge(mainImageUrl); // Asumsikan yang disimpan adalah 'large' atau URL utama
                        article.setImageObject(image);
                    }

                    article.setPublishedAt(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PUBLISHED_AT)));
                    // int sourceNameIndex = cursor.getColumnIndex(COLUMN_SOURCE_NAME); // Baris ini tidak lagi diperlukan
                    // if (sourceNameIndex != -1) article.setSourceName(cursor.getString(sourceNameIndex)); // Baris ini dihapus
                    article.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)));
                    article.setContent(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTENT_FULL)));
                    article.setSaved(true);
                    articleList.add(article);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching all saved articles: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return articleList;
    }

    public boolean isArticleSaved(String articleUrl) {
        if (articleUrl == null || articleUrl.isEmpty()) return false;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        boolean isSaved = false;
        try {
            cursor = db.query(TABLE_SAVED_ARTICLES, new String[]{COLUMN_DB_ID}, COLUMN_URL + " = ?",
                    new String[]{articleUrl}, null, null, null);
            if (cursor != null && cursor.getCount() > 0) {
                isSaved = true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking if article is saved: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return isSaved;
    }
}
