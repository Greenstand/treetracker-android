package org.greenstand.android.TreeTracker.api

import okhttp3.Interceptor
import okhttp3.Response
import org.greenstand.android.TreeTracker.managers.UserManager
import java.io.IOException

class AuthenticationInterceptor(private val userManager: UserManager) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {

        val original = chain.request()

        if (userManager.authToken != null) {
            val request = original.newBuilder()
                .header("Authorization", "Bearer ${userManager.authToken}")
                .build()
            return chain.proceed(request)
        } else {
            return chain.proceed(original)
        }
    }
}