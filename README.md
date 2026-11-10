# Fresk - Sumativa 2 - PRO403

Fresk es una aplicación Android para registrar alimentos, controlar cantidades y consultar fechas estimadas de caducidad. Funciona de manera local, sin conexión a internet.

## Tecnologías

- Kotlin, Jetpack Compose y Material 3.
- Room y SQLite para guardar los datos en el dispositivo.
- Gradle 8.9 y Android Gradle Plugin (AGP) 8.7.2.

## Requisitos

- Android Studio y Android SDK 35.
- JDK 21 para ejecutar Gradle.
- Celular o emulador con Android 8.0 (API 26) o superior.
- Internet durante la primera sincronización para descargar las dependencias.

**Importante:** Al abrir el proyecto en Android Studio indica que Gradle debe usar Java 21 en `gradle/gradle-daemon-jvm.properties`. Si Java 21 no está instalado, es necesario instalarlo. No selecciones Java 25 ni actualices Gradle o AGP solo por un aviso de Android Studio.

## Cómo abrir y ejecutar

1. Descarga o clona este repositorio desde GitHub.
2. En Android Studio, selecciona **Open** y abre la carpeta del proyecto (la que contiene `settings.gradle.kts`).
3. Espera a que termine **Gradle Sync**. Si se solicita el JDK, configura **JDK 21** en **Settings -> Build, Execution, Deployment -> Build Tools -> Gradle -> Gradle JDK**.
4. Instala **Android SDK 35** si Android Studio lo solicita.
5. Selecciona **app** y un celular físico conectado con depuración USB o un emulador.
6. Presiona **Run** para instalar y abrir Fresk.

Para comprobar la compilación: **Build -> Assemble Project**.

## Funciones principales

- Registrar y editar productos, cantidades y unidades de medida.
- Buscar productos y organizarlos por categorías y tipo de almacenamiento.
- Registrar consumos, descartes y eliminaciones con motivos predefinidos o agregar motivos distintos.
- Consultar productos próximos a vencer y con fecha estimada cumplida.
- Guardar los datos localmente, incluso al cerrar la aplicación.

## Base de datos

La base se llama `fresk_local.db` y utiliza las tablas `products` y `movements`.
Se agregaron productos en un catálogo de prueba para esta primera versión de la aplicación. Esto para categorizar automáticamente los productos y asignarle una fecha de caducidad estimada automáticamente.
Las fechas del catálogo son estimaciones para el piloto y no garantizan la seguridad de los alimentos.
