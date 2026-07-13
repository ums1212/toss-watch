package dev.comon.toss_watch.core.network.di

import javax.inject.Qualifier

/**
 * 토큰 갱신 전용 OkHttpClient/Retrofit 식별자.
 * [dev.comon.toss_watch.core.network.TokenAuthenticator]가 부착되지 않은 클라이언트로,
 * 갱신 요청이 다시 Authenticator를 타는 재귀를 방지한다.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RefreshClient
