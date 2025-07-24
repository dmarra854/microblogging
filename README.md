# Microblogging

Este proyecto implementa una plataforma simplificada de microblogging inspirada en Twitter (X), que permite a los usuarios publicar, seguir y ver el timeline de tweets, con una arquitectura limpia (Clean Architecture), con separación de responsabilidades, que incluye principios de diseño dirigido por el dominio (DDD) y un enfoque de microservicios.

Los usuarios pueden publicar tweets, seguir/dejar de seguir a otros usuarios y ver sus líneas de tiempo personalizadas.

La aplicación utiliza Spring Boot para los servicios, JPA para la persistencia, Redis para el almacenamiento en caché y Kafka para la comunicación basada en eventos.



## ✨ Características
- Publicación de tweets (máx. 280 caracteres)
- Seguimiento de usuarios
- Visualización de timeline
- Arquitectura hexagonal con casos de uso desacoplados
- Base de datos en memoria (H2) para pruebas

---

## Arquitectura
adapter in (web controllers) ───► application (use cases) ───► domain (models, ports) ◄─── adapter out (repositories)


- **Domain**: Entidades y lógica de negocio (`Tweet`, `User`, etc.)
- **Application**: Casos de uso (`PostTweetUseCase`, `TimelineService`, etc.)
- **Adapters**:
    - **In**: Controladores REST (`TweetController`, `FollowController`, etc.)
    - **Out**: Persistencia con Spring Data JPA, cache con Redis (opcional)
- **Infraestructura**: Configuración de base de datos, seguridad y herramientas.

---
```plaintext
microblogging/
├── src/main/java/com/microblogging/project/
│   ├── adapter/      # Adaptadores de entrada/salida (ej: controladores, persistencia)
│   ├── application/  # Casos de uso de aplicación
│   ├── domain/       # Entidades de dominio y repositorios
│   └── ProjectApplication.java  # Clase principal
└── src/test/java/... # Pruebas unitarias e integración
```


## ⚙️  Tecnologías utilizadas

* **Backend:** Java 21
* **Framework:** Spring Boot 3.x
* **Herramienta de Construcción:** Maven
* **Persistencia:** Spring Data JPA (compatible con varias BBDD relacionales, configurado para H2 en memoria para desarrollo local)
* **Almacenamiento en Caché:** Spring Data Redis
* **Mensajería/Eventos:** Spring Kafka
* **Base de Datos:** H2 (para desarrollo/pruebas local), PostgreSQL (recomendado para producción)
* **Pruebas:** JUnit 5, Mockito
* **Contenedorización:** Docker, Docker Compose
* **Utilidades:** Lombok (para reducción de código repetitivo), SLF4J/Logback (para registro de logs)

---
## Ejecución local


1. Clonar el repositorio:

    ```bash
    git clone https://github.com/dmarra854/microblogging.git
 
    ```

2. Compilar y ejecutar:
    ```bash
    
   ./mvnw spring-boot:run
    ```

3. La API estará disponible en: [http://localhost:9090](http://localhost:9090)

## Despliegue con Docker

Este proyecto incluye un `Dockerfile` para la aplicación y un `docker-compose.yml` para levantar el servicio junto con una base de datos.

### Construir y levantar los contenedores
```bash
docker-compose up --build
```

### Acceder a la aplicación
- La aplicación estará disponible en: [http://localhost:9090](http://localhost:9090)

### Parar los contenedores
```bash
docker-compose down
```



## API principal

### Crear un Tweet
```http
POST /microblogging/tweets
Headers:
  X-User-Id: <UUID>
Body:
{
  "content": "Hola mundo!"
}
Response: 201 Created
```

### Pruebas
Ejecutar las pruebas con:
```bash
./mvnw test
```


### Probar la API con curl
```bash
curl -X POST http://localhost:9090/tweets \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 123e4567-e89b-12d3-a456-426614174000" \
  -d '{"content": "Mi primer tweet desde microblogging!"}'
```


### Autor
- **Darío Marranti** ([@dmarra854](https://github.com/dmarra854))