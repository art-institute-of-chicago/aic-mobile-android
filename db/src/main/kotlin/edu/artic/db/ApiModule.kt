package edu.artic.db

import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import javax.inject.Named
import javax.inject.Singleton

@Module
abstract class ApiModule {

    @Module
    companion object {
        @JvmStatic
        @Provides
        @Singleton
        @Named("BlobRetrofit")
        fun provideBlobRetrofit(): Retrofit {
            val builder = Retrofit.Builder()
            return builder.build()
        }
    }
}