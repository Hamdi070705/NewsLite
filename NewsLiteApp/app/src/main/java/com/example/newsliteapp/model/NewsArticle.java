package com.example.newsliteapp.model; // Pastikan package sesuai dengan proyek Anda

import android.os.Parcel;
import android.os.Parcelable;
import com.google.gson.annotations.SerializedName;

public class NewsArticle implements Parcelable {

    // Nested class untuk menangani objek 'image' dari API
    public static class Image implements Parcelable {
        @SerializedName("small")
        private String small;

        @SerializedName("large")
        private String large;

        public Image() {}

        protected Image(Parcel in) {
            small = in.readString();
            large = in.readString();
        }

        public static final Creator<Image> CREATOR = new Creator<Image>() {
            @Override
            public Image createFromParcel(Parcel in) {
                return new Image(in);
            }

            @Override
            public Image[] newArray(int size) {
                return new Image[size];
            }
        };

        public String getSmall() { return small; }
        public void setSmall(String small) { this.small = small; }
        public String getLarge() { return large; }
        public void setLarge(String large) { this.large = large; }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(small);
            dest.writeString(large);
        }
    }

    // dbId adalah untuk penggunaan internal database lokal, TIDAK diharapkan dari JSON API.
    // Kita akan mengisinya secara manual (misalnya dengan URL berita) sebelum menyimpan ke DB.
    private String dbId;

    @SerializedName("title")
    private String title;

    @SerializedName("link") // Sesuai dengan API di PDF
    private String url; // Nama variabel di model tetap 'url' untuk konsistensi

    @SerializedName("isoDate") // Sesuai dengan API di PDF
    private String publishedAt; // Nama variabel di model tetap 'publishedAt'

    @SerializedName("image") // Objek Image yang berisi 'small' dan 'large'
    private Image imageObject;

    @SerializedName("contentSnippet") // Sesuai dengan API di PDF
    private String description; // Nama variabel di model tetap 'description'

    // Field 'content' (konten lengkap) tidak ada di contoh API artikel pada PDF.
    // Dibiarkan ada untuk potensi penggunaan lain, tapi tidak akan diisi oleh Gson dari API ini.
    private String content;

    // isSaved adalah untuk status database lokal, TIDAK dari JSON API.
    private boolean isSaved;

    public NewsArticle() {
    }

    // Konstruktor utama (tanpa dbId dan isSaved, karena itu diatur terpisah)
    public NewsArticle(String title, String url, String publishedAt, Image imageObject, String description, String content) {
        this.title = title;
        this.url = url;
        this.publishedAt = publishedAt;
        this.imageObject = imageObject;
        this.description = description;
        this.content = content;
        this.isSaved = false; // Default
        // dbId akan di-set sebelum disimpan ke database, misal: article.setDbId(article.getUrl());
    }

    protected NewsArticle(Parcel in) {
        dbId = in.readString();
        title = in.readString();
        url = in.readString();
        publishedAt = in.readString();
        imageObject = in.readParcelable(Image.class.getClassLoader());
        description = in.readString();
        content = in.readString();
        isSaved = in.readByte() != 0;
    }

    public static final Creator<NewsArticle> CREATOR = new Creator<NewsArticle>() {
        @Override
        public NewsArticle createFromParcel(Parcel in) {
            return new NewsArticle(in);
        }

        @Override
        public NewsArticle[] newArray(int size) {
            return new NewsArticle[size];
        }
    };

    // Getters
    public String getDbId() { return dbId; } // Untuk keperluan database
    public String getTitle() { return title; }
    public String getUrl() { return url; }
    public String getPublishedAt() { return publishedAt; }
    public Image getImageObject() { return imageObject; }
    public String getDescription() { return description; }
    public String getContent() { return content; }
    public boolean isSaved() { return isSaved; } // Untuk status simpan

    // Helper getter untuk URL gambar (mengambil yang 'large' atau 'small')
    public String getImageUrl() {
        if (imageObject != null) {
            if (imageObject.getLarge() != null && !imageObject.getLarge().isEmpty()) {
                return imageObject.getLarge();
            } else if (imageObject.getSmall() != null && !imageObject.getSmall().isEmpty()) {
                return imageObject.getSmall();
            }
        }
        return null;
    }

    // Setters
    public void setDbId(String dbId) { this.dbId = dbId; } // Untuk keperluan database
    public void setTitle(String title) { this.title = title; }
    public void setUrl(String url) { this.url = url; }
    public void setPublishedAt(String publishedAt) { this.publishedAt = publishedAt; }
    public void setImageObject(Image imageObject) { this.imageObject = imageObject; }
    public void setDescription(String description) { this.description = description; }
    public void setContent(String content) { this.content = content; }
    public void setSaved(boolean saved) { this.isSaved = saved; } // Untuk status simpan


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(dbId);
        dest.writeString(title);
        dest.writeString(url);
        dest.writeString(publishedAt);
        dest.writeParcelable(imageObject, flags);
        dest.writeString(description);
        dest.writeString(content);
        dest.writeByte((byte) (isSaved ? 1 : 0));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NewsArticle article = (NewsArticle) o;
        return url != null ? url.equals(article.url) : article.url == null;
    }

    @Override
    public int hashCode() {
        return url != null ? url.hashCode() : 0;
    }
}
