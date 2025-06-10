package com.example.newsliteapp.model; // Pastikan package sesuai dengan proyek Anda

import com.google.gson.annotations.SerializedName;
import java.util.List;

// Model ini disesuaikan dengan struktur JSON dari API:
// https://berita-indo-api-next.vercel.app/api/cnn-news/TERBARU
public class NewsApiResponse {

    @SerializedName("message") // Pesan status dari API
    private String message;

    @SerializedName("total") // Jumlah total hasil
    private int total;

    @SerializedName("data") // Array/List dari artikel berita
    private List<NewsArticle> data;

    // Getter dan Setter
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public List<NewsArticle> getArticles() { // Mengubah nama getter agar konsisten dengan penggunaan sebelumnya
        return data;
    }

    public void setArticles(List<NewsArticle> data) { // Mengubah nama setter
        this.data = data;
    }

    // Anda bisa menambahkan field untuk error jika API mengembalikannya dalam struktur yang berbeda
    // Untuk saat ini, kita asumsikan error ditangani melalui kode HTTP atau parsing errorBody Retrofit
}
