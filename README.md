# API Gateway

HemenKirala servisleri için merkezi yönlendirme, JWT doğrulama, rate limiting ve izleme katmanı sağlayan Spring Cloud Gateway servisi.

---

## İçindekiler
- [Genel Bakış](#genel-bakış)
- [Mimari](#mimari)
- [Teknolojiler](#teknolojiler)
- [Veritabanı](#veritabanı)
- [API Endpoints](#api-endpoints)
- [Servisler Arası İletişim](#servisler-arası-i̇letişim)
- [Kurulum](#kurulum)
- [Ortam Değişkenleri](#ortam-değişkenleri)
- [Testler](#testler)

---

## Genel Bakış

API Gateway, istemciler ile Lendmate mikroservisleri arasındaki tek giriş noktasıdır. Gelen istekleri Config Server'dan alınan route tanımlarına göre ilgili servise yönlendirir.

Gateway'in sorumlulukları:

- Kimlik doğrulaması gereken isteklerde `Bearer` JWT token'ını doğrulamak.
- JWT içindeki kullanıcı bilgilerini `X-User-Email`, `X-User-Role` ve `X-User-Id` header'larıyla downstream servislere aktarmak.
- Kullanıcı veya anonim istemci anahtarına göre rate limiting uygulamak.
- OpenTelemetry trace context'ini yönlendirilen isteklere taşımak.
- Ortam bazlı ayarları Spring Cloud Config Server üzerinden almak.

JWT güvenliği `gateway.security.enabled` ayarıyla kapatılabilir; varsayılan değer `true`'dur. Güvenlik kapatıldığında istekler token kontrolü yapılmadan route zincirine gönderilir.

---

## Mimari

### Katmanlar

- **Gateway katmanı:** Spring Cloud Gateway, istekleri route tanımlarına göre yönlendirir.
- **Filtre katmanı:** `JwtAuthenticationFilter` kimlik doğrulamasını, `TraceContextPropagationFilter` ise trace bilgisinin aktarımını yönetir.
- **Servis katmanı:** `JwtService`, imzalı JWT token'larından claim okuma ve token geçerlilik kontrolünü yapar.
- **Konfigürasyon katmanı:** Rate limiter anahtar çözümleyicisi ve herkese açık path listesi burada tanımlanır.

### Klasör Yapısı

```text
src/main/java/com/lendmate/apigateway/
├── ApiGatewayApplication.java
├── config/
│   ├── CorsConfig.java
│   ├── RateLimiterConfig.java
│   ├── SecurityPaths.java
│   └── TraceContextPropagationFilter.java
├── filter/
│   └── JwtAuthenticationFilter.java
└── service/
	└── JwtService.java
```

Filtre sırası önemlidir: trace propagation filtresi en yüksek öncelikle çalışır, JWT filtresi ise `-1` sırasıyla kimlik doğrulamasını gerçekleştirir.

---

## Teknolojiler

| Teknoloji | Versiyon | Kullanım Amacı |
|---|---|---|
| Java | 21 | Uygulama çalışma ortamı |
| Spring Boot | 3.5.14 | Uygulama çatısı |
| Spring Cloud Gateway | 2025.0.0 BOM | Reactive API gateway ve route yönetimi |
| Spring Cloud Config | 2025.0.0 BOM | Merkezi konfigürasyon |
| JJWT | 0.11.5 | JWT imza ve claim doğrulama |
| Spring Data Redis Reactive | Spring Boot ile yönetilir | Rate limiter state'i |
| OpenTelemetry Spring Boot Starter | 2.6.0 | Dağıtık izleme ve trace propagation |
| Spring Boot Actuator | Spring Boot ile yönetilir | Sağlık ve operasyon endpoint'leri |
| Springdoc OpenAPI WebFlux UI | 2.3.0 | OpenAPI/Swagger arayüzü |
| Micrometer OTLP Registry | Spring Boot ile yönetilir | Metriklerin OTLP ile aktarımı |

---

## Veritabanı

### Tablolar

Bu servis kendi veritabanı tablolarını kullanmaz. Rate limiting için reactive Redis bağımlılığı bulunur; Redis kalıcı iş verisi veritabanı olarak kullanılmaz.

---

## API Endpoints

### Gateway rotaları

Route tanımları kaynak kodda sabit değildir; aktif profile göre Config Server'dan alınır. Bu nedenle downstream servis endpoint'leri Config Server konfigürasyonuna göre değişebilir.

Gateway'in bildiği herkese açık path'ler:

| Method | Endpoint | Açıklama | Auth |
|---|---|---|---|
| `*` | `/auth/health` | Auth servisinin sağlık endpoint'i | Gerekmez |
| `*` | `/auth/login` | Kullanıcı girişi | Gerekmez |
| `*` | `/auth/register` | Kullanıcı kaydı | Gerekmez |
| `*` | `/auth/refresh` | Access token yenileme | Gerekmez |
| `*` | `/user-service/v3/api-docs` | User Service OpenAPI tanımı | Gerekmez |
| `*` | `/product-service/v3/api-docs` | Product Service OpenAPI tanımı | Gerekmez |
| `*` | `/swagger-ui`, `/webjars`, `/v3/api-docs` | Swagger/OpenAPI kaynakları | Gerekmez |

Yukarıdaki liste dışındaki route'larda `Authorization: Bearer <JWT>` header'ı zorunludur. Eksik, hatalı veya geçersiz token için gateway `401 Unauthorized` döndürür.

---

## Servisler Arası İletişim

### Feign Client (Senkron)

Bu projede Feign Client kullanılmıyor. Gateway, servisler arası çağrı yapmak yerine HTTP isteklerini Spring Cloud Gateway üzerinden yönlendirir.

### Kafka Events (Asenkron)

Bu projede Kafka producer/consumer bulunmuyor. Asenkron event iletişimi bu servisin sorumluluğunda değil.

---

## Kurulum

### Gereksinimler

- Java 21
- Maven veya proje içindeki Maven Wrapper (`./mvnw`)
- Dev ortamında isteğe bağlı Spring Cloud Config Server (`http://localhost:8888`)
- Rate limiting route'ları etkinse Redis
- Docker ile çalıştırmada `lendmate-net` isimli harici Docker ağı

### Çalıştırma

```bash
# Testleri çalıştırır ve paketi oluşturur
./mvnw clean verify

# Varsayılan dev profiliyle başlatır
./mvnw spring-boot:run
```

Uygulama varsayılan olarak `8084` portunda başlar. Docker Compose ile çalıştırmak için önce harici ağı oluşturup servisi başlatın:

```bash
docker network create lendmate-net
docker compose up --build
```

Compose tanımı `prod` profilini, `config-server:8888` Config Server adresini ve `otel-collector:4318` OTLP adresini kullanır.

---

## Ortam Değişkenleri

| Değişken | Açıklama | Örnek |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Aktif Spring profili | `dev`, `stage`, `prod` |
| `CONFIG_SERVER_URL` | Spring Cloud Config Server adresi | `http://localhost:8888` |
| `JWT_SECRET` | Base64 kodlu JWT imzalama anahtarı | `base64-secret` |
| `GATEWAY_SECURITY_ENABLED` | JWT güvenlik filtresini etkinleştirir | `true` |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | OpenTelemetry Collector OTLP adresi | `http://otel-collector:4318` |
| `OTEL_SERVICE_NAME` | OpenTelemetry servis adı | `api-gateway` |

`application.yaml` içinde `server.port` değeri `8084` olarak sabittir. `JWT_SECRET`, uygulamadaki `jwt.secret` ayarına; `GATEWAY_SECURITY_ENABLED` ise `gateway.security.enabled` ayarına karşılık gelecek şekilde Config Server veya ortam konfigürasyonunda sağlanmalıdır. Secret değerleri production ortamında repoya yazılmamalıdır.

---

## Testler

Testleri çalıştırmak için:

```bash
./mvnw test
```

Mevcut test, Spring uygulama context'inin başarıyla ayağa kalktığını doğrulayan bir smoke test'tir. JWT filtresi, rate limiter, route forwarding ve trace propagation için henüz ayrı birim veya entegrasyon testi bulunmamaktadır.
