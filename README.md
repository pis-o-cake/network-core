# Android Network Core 🚀

> 모던 안드로이드 개발을 위한 경량 네트워크 래퍼 라이브러리

[🌐 View in English](#english)

---

반복적인 try-catch 보일러플레이트를 제거하고, API 응답 처리를 표준화
Generic Interface(`BaseResponse`) 접근 방식으로 라이브러리 수정 없이 다양한 서버 응답 형식에 대응

## 주요 기능

| 기능 | 설명 |
|------|------|
| 🛡️ **타입 안전 에러 처리** | Sealed Class (`Success`, `Empty`, `Error`, `NetworkError`, `Exception`) |
| 🔄 **로딩 상태 지원** | UI 바인딩을 위한 `Loading` 상태 내장 |
| 🌐 **네트워크 에러 분류** | Timeout, 연결 실패, 기타 예외 분리 처리 |
| 🧩 **범용 호환성** | 추상화된 `BaseResponse<T>` 인터페이스로 다양한 서버 포맷 지원 |
| ⚡ **Coroutines 기반** | Kotlin Coroutines & Suspend 함수 기반 설계 |
| 🧹 **Clean Architecture 호환** | Data Layer (Repository)에서 바로 사용 |

## ⚠️ 이 라이브러리가 하지 않는 것

이 라이브러리는 **API 응답 래핑**에만 집중합니다. 다음 항목들은 **사용하는 프로젝트에서 직접 구성**해야 함

| 항목 | 설명 |
|------|------|
| Retrofit/OkHttp 인스턴스 | 직접 생성 및 DI 구성 |
| Base URL 설정 | 서버 주소, 환경별(dev/prod) 분기 |
| Interceptor | 인증 토큰, 로깅, 헤더 추가 등 |
| Timeout 설정 | connect/read/write 타임아웃 |
| SSL/인증서 | 인증서 피닝, 커스텀 TrustManager |

**이유:** 프로젝트마다 서버 구성, 인증 방식, 빌드 환경이 다르기 때문에 라이브러리에서 강제하지 않음

<details>
<summary>📋 Retrofit 설정 예시 (Hilt)</summary>

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) 
                    HttpLoggingInterceptor.Level.BODY 
                else 
                    HttpLoggingInterceptor.Level.NONE
            })
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer ${TokenManager.accessToken}")
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)  // 환경별 분기
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideUserApi(retrofit: Retrofit): UserApi {
        return retrofit.create(UserApi::class.java)
    }
}
```

</details>

## 설치 방법

### JitPack

**Step 1.** `settings.gradle.kts`에 JitPack 저장소 추가:

```kotlin
dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://jitpack.io") }
    }
}
```

**Step 2.** 의존성 추가:

```kotlin
dependencies {
    implementation("com.github.pis-o-cake:network-core:1.0.0")
}
```

## 사용 방법

### 1. 서버 응답 형식에 맞게 `BaseResponse` 구현

```kotlin
// 예시: { "code": 200, "message": "성공", "result": { ... } }
data class ApiResponse<T>(
    val code: Int,
    val message: String?,
    val result: T?
) : BaseResponse<T> {
    override fun isSuccess(): Boolean = code == 200
    override fun getData(): T? = result
    override fun getErrorCode(): String? = if (code != 200) code.toString() else null
    override fun getMessage(): String? = message
}
```

```kotlin
// 예시: { "res": true, "msg": "OK", "data": { ... } }
data class LegacyResponse<T>(
    val res: Boolean,
    val msg: String?,
    val data: T?
) : BaseResponse<T> {
    override fun isSuccess(): Boolean = res
    override fun getData(): T? = data
    override fun getErrorCode(): String? = if (!res) "FAIL" else null
    override fun getMessage(): String? = msg
}
```

### 2. Retrofit API 정의

```kotlin
interface UserApi {
    @GET("users/{id}")
    suspend fun getUser(@Path("id") id: Long): Response<ApiResponse<User>>
    
    @DELETE("users/{id}")
    suspend fun deleteUser(@Path("id") id: Long): Response<ApiResponse<Unit>>
}
```

### 3. Repository에서 `safeApiCall` 사용

```kotlin
class UserRepositoryImpl(
    private val api: UserApi
) : UserRepository {

    // 데이터 반환 API
    override suspend fun getUser(id: Long): NetworkResult<User> {
        return safeApiCall { api.getUser(id) }
    }

    // 데이터 없는 API (DELETE 등)
    override suspend fun deleteUser(id: Long): NetworkResult<Unit> {
        return safeApiCallEmpty { api.deleteUser(id) }
    }
}
```

### 4. ViewModel에서 결과 처리

```kotlin
// 방법 1: 체이닝 콜백
viewModelScope.launch {
    _uiState.value = UiState.Loading
    
    repository.getUser(userId)
        .onSuccess { user ->
            _uiState.value = UiState.Success(user)
        }
        .onError { error ->
            _uiState.value = UiState.Error("${error.code}: ${error.message}")
        }
        .onNetworkError { e ->
            _uiState.value = UiState.Error("네트워크 연결 실패")
        }
        .onException { e ->
            _uiState.value = UiState.Error("알 수 없는 오류: ${e.message}")
        }
}

// 방법 2: when 분기
viewModelScope.launch {
    when (val result = repository.getUser(userId)) {
        is NetworkResult.Loading -> { /* 로딩 UI */ }
        is NetworkResult.Success -> { /* result.data 사용 */ }
        is NetworkResult.Empty -> { /* 성공, 데이터 없음 */ }
        is NetworkResult.Error -> { /* result.code, result.message */ }
        is NetworkResult.NetworkError -> { /* 네트워크 에러 */ }
        is NetworkResult.Exception -> { /* 예외 처리 */ }
    }
}
```

## NetworkResult 상태 정리

| 상태 | 설명 |
|------|------|
| `Loading` | 요청 진행 중 |
| `Success<T>` | 성공 (데이터 있음) |
| `Empty` | 성공 (데이터 없음) - DELETE 등 |
| `Error` | HTTP 에러 또는 비즈니스 로직 에러 |
| `NetworkError` | 네트워크 연결 실패, 타임아웃 |
| `Exception` | 예상치 못한 예외 |

## 유틸리티 함수

```kotlin
val result: NetworkResult<User> = repository.getUser(1)

// 성공 여부
result.isSuccess   // true if Success or Empty
result.isFailure   // true if Error, NetworkError, Exception

// 데이터 추출
result.getOrNull()           // User? (실패 시 null)
result.getOrDefault(guest)   // User (실패 시 기본값)
result.getOrThrow()          // User (실패 시 예외 발생)
```

## 프로젝트 구조

```
networkcore/
├── model/
│   ├── BaseResponse.kt      # 서버 응답 추상화 인터페이스
│   └── NetworkResult.kt     # Sealed Class 결과 래퍼
└── util/
    └── safeApiCall.kt       # API 호출 래퍼 함수
```

## License

```
MIT License
Copyright (c) 2024 pis-o-cake
```

---

<a id="english"></a>
# English

> A lightweight, reusable network wrapper designed for Modern Android Development.

Eliminates repetitive try-catch boilerplate and standardizes API response handling.  
Uses a Generic Interface (`BaseResponse`) approach, allowing adaptation to any server JSON structure without modifying the core library.

## Key Features

| Feature | Description |
|---------|-------------|
| 🛡️ **Type-Safe Error Handling** | Sealed Classes (`Success`, `Empty`, `Error`, `NetworkError`, `Exception`) |
| 🔄 **Loading State** | Built-in `Loading` state for UI binding |
| 🌐 **Network Error Classification** | Separates Timeout, Connection failure, and other exceptions |
| 🧩 **Universal Compatibility** | Abstracted `BaseResponse<T>` interface supports various server formats |
| ⚡ **Coroutines First** | Built entirely on Kotlin Coroutines and Suspend functions |
| 🧹 **Clean Architecture Ready** | Designed for Data Layer (Repositories) |

## ⚠️ What This Library Does NOT Do

This library focuses **only on API response wrapping**. The following must be **configured in your project**:

| Item | Description |
|------|-------------|
| Retrofit/OkHttp Instance | Create and configure via DI |
| Base URL | Server address, environment switching (dev/prod) |
| Interceptors | Auth tokens, logging, custom headers |
| Timeout Settings | connect/read/write timeouts |
| SSL/Certificates | Certificate pinning, custom TrustManager |

**Reason:** Server configuration, authentication methods, and build environments vary by project, so the library does not enforce them.

<details>
<summary>📋 Retrofit Setup Example (Hilt)</summary>

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) 
                    HttpLoggingInterceptor.Level.BODY 
                else 
                    HttpLoggingInterceptor.Level.NONE
            })
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer ${TokenManager.accessToken}")
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)  // Environment-based
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideUserApi(retrofit: Retrofit): UserApi {
        return retrofit.create(UserApi::class.java)
    }
}
```

</details>

## Installation

### JitPack

**Step 1.** Add JitPack repository to `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://jitpack.io") }
    }
}
```

**Step 2.** Add dependency:

```kotlin
dependencies {
    implementation("com.github.pis-o-cake:network-core:1.0.0")
}
```

## Usage

### 1. Implement `BaseResponse` for your server format

```kotlin
data class ApiResponse<T>(
    val code: Int,
    val message: String?,
    val result: T?
) : BaseResponse<T> {
    override fun isSuccess(): Boolean = code == 200
    override fun getData(): T? = result
    override fun getErrorCode(): String? = if (code != 200) code.toString() else null
    override fun getMessage(): String? = message
}
```

### 2. Define Retrofit API

```kotlin
interface UserApi {
    @GET("users/{id}")
    suspend fun getUser(@Path("id") id: Long): Response<ApiResponse<User>>
    
    @DELETE("users/{id}")
    suspend fun deleteUser(@Path("id") id: Long): Response<ApiResponse<Unit>>
}
```

### 3. Use `safeApiCall` in Repository

```kotlin
class UserRepositoryImpl(
    private val api: UserApi
) : UserRepository {

    override suspend fun getUser(id: Long): NetworkResult<User> {
        return safeApiCall { api.getUser(id) }
    }

    override suspend fun deleteUser(id: Long): NetworkResult<Unit> {
        return safeApiCallEmpty { api.deleteUser(id) }
    }
}
```

### 4. Handle results in ViewModel

```kotlin
viewModelScope.launch {
    _uiState.value = UiState.Loading
    
    repository.getUser(userId)
        .onSuccess { user ->
            _uiState.value = UiState.Success(user)
        }
        .onError { error ->
            _uiState.value = UiState.Error("${error.code}: ${error.message}")
        }
        .onNetworkError {
            _uiState.value = UiState.Error("Network connection failed")
        }
        .onException { e ->
            _uiState.value = UiState.Error("Unknown error: ${e.message}")
        }
}
```

## NetworkResult States

| State | Description |
|-------|-------------|
| `Loading` | Request in progress |
| `Success<T>` | Success with data |
| `Empty` | Success without data (DELETE, etc.) |
| `Error` | HTTP or business logic error |
| `NetworkError` | Connection failure, timeout |
| `Exception` | Unexpected exception |

## License

```
MIT License
Copyright (c) 2024 pis-o-cake
```
