package com.example.newsliteapp.network; // Pastikan package sesuai dengan proyek Anda

import com.example.newsliteapp.model.NewsApiResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ApiService {

    // Endpoint untuk mendapatkan berita umum dari CNN News (untuk kategori "Terkini")
    // URL lengkap yang akan dipanggil: BASE_URL + "cnn-news"
    // Contoh: https://berita-indo-api-next.vercel.app/api/cnn-news
    // BASE_URL Anda adalah: https://berita-indo-api-next.vercel.app/api/
    @GET("cnn-news")
    Call<NewsApiResponse> getLatestCnnNews(); // Metode untuk "Terkini"

    // Endpoint untuk mendapatkan berita berdasarkan kategori spesifik
    // Contoh URL: https://berita-indo-api-next.vercel.app/api/cnn-news/nasional
    @GET("cnn-news/{category}")
    Call<NewsApiResponse> getNewsByCategory(@Path("category") String category);

    // Jika API Anda mendukung pencarian dengan query parameter terpisah dari kategori:
    // @GET("cnn-news/search") // Atau path pencarian yang sesuai
    // Call<NewsApiResponse> searchNews(@Query("q") String query);
    // Untuk saat ini, kita asumsikan pencarian dilakukan dengan memfilter hasil kategori
    // atau jika API mendukung, mengirimkan query sebagai {category}.
}
