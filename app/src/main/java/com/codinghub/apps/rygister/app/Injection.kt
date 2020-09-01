package com.codinghub.apps.rygister.app

import com.codinghub.apps.rygister.BuildConfig
import com.codinghub.apps.rygister.model.AppPrefs
import com.codinghub.apps.rygister.repository.CDHDemoApi
import com.codinghub.apps.rygister.repository.RemoteRepository
import com.codinghub.apps.rygister.repository.Repository
import com.codinghub.apps.rygister.repository.RygisterApi
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object Injection {

    fun provideRepository(): Repository = RemoteRepository

    //Login Section
    private fun provideRetrofitLogin(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(AppPrefs.getServiceURL().toString())
            .addConverterFactory(GsonConverterFactory.create())
            .client(provideOkHttpClientForLogin())
            .build()
    }

    private fun provideOkHttpClientForLogin(): OkHttpClient {

        val x509TrustManager = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {

            }

            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {

            }

            override fun getAcceptedIssuers(): Array<X509Certificate> {
                return arrayOf()
            }
        }

        val trustManagers = arrayOf<TrustManager>(x509TrustManager)

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustManagers, null)

        val httpClient = OkHttpClient.Builder()
        httpClient.sslSocketFactory(sslContext.socketFactory, x509TrustManager)
        httpClient.hostnameVerifier {_, _ -> true}
        httpClient.addInterceptor(provideLoggingInterceptor())

        httpClient.addInterceptor { chain ->
            val request = chain.request()
                .newBuilder()
                .addHeader("content-type", "application/json;charset=UTF-8")
                .build()

            chain.proceed(request)
        }
        return httpClient.build()
    }

    fun provideLoginAPI(): RygisterApi {
        return provideRetrofitLogin().create(RygisterApi::class.java)
    }

    //General API Section
    private fun provideRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(AppPrefs.getServiceURL().toString())
            .addConverterFactory(GsonConverterFactory.create())
            .client(provideOkHttpClient())
            .build()
    }

    private fun provideLoggingInterceptor(): HttpLoggingInterceptor {

        val logging = HttpLoggingInterceptor()
        logging.level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
        return logging
    }

    private fun provideOkHttpClient(): OkHttpClient {

        val x509TrustManager = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {

            }

            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {

            }

            override fun getAcceptedIssuers(): Array<X509Certificate> {
                return arrayOf()
            }
        }

        val trustManagers = arrayOf<TrustManager>(x509TrustManager)

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustManagers, null)

        val httpClient = OkHttpClient.Builder()
        httpClient.sslSocketFactory(sslContext.socketFactory, x509TrustManager)
        httpClient.hostnameVerifier {_, _ -> true}
        httpClient.addInterceptor(provideLoggingInterceptor())

        httpClient.addInterceptor { chain ->
            val request = chain.request()
                .newBuilder()
                .addHeader("Authorization", "Bearer ${AppPrefs.getToken()}")
                .addHeader("Backend-Authorization", "${AppPrefs.getBackendToken()}")
                .build()

            chain.proceed(request)
        }
        return httpClient.build()
    }

    fun provideRygisterApi(): RygisterApi {
        return provideRetrofit().create(RygisterApi::class.java)
    }

    //CDH API

    private fun provideFaceCompareRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(AppPrefs.getCDHServiceURL().toString())
            .addConverterFactory(GsonConverterFactory.create())
            .client(provideFaceCompareOkHttpClient())
            .build()
    }

    private fun provideFaceCompareOkHttpClient(): OkHttpClient {

        val httpClient = OkHttpClient.Builder()
        httpClient.addInterceptor(provideLoggingInterceptor())
        val username = AppPrefs.getCDHUserName().toString()
        val password = AppPrefs.getCDHPassword().toString()

        httpClient.addInterceptor { chain ->
            val request = chain.request()
                .newBuilder()
                .addHeader("Authorization", Credentials.basic(username, password))
                .addHeader("apikey", AppPrefs.getApiKey().toString())
                .build()

            chain.proceed(request)
        }
        return httpClient.build()
    }

    fun provideCDHDemoApi(): CDHDemoApi {
        return provideFaceCompareRetrofit().create(CDHDemoApi::class.java)
    }




}