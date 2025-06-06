
# 📱 Control Parental App

Aplicación Android de control parental desarrollada en **Kotlin**, utilizando **Room**, **WorkManager**, **Dagger Hilt** y servicios de accesibilidad para supervisar, limitar y registrar el uso de aplicaciones.

[Propuesta de valor](https://docs.google.com/document/d/12kFZDpTqzES0-sYFv3g2N5VKG0sBjfh_hx_XMg7oy6Q/edit?usp=sharing)

[SRS - Especificacion de requisitos](https://docs.google.com/document/d/1rpAelZsWywcVWXfYSeQUA5GreiuIhPb2/edit?usp=sharing&ouid=103592374588151306182&rtpof=true&sd=true)

<div style="display: flex;" align="center">
  <img src="https://github.com/user-attachments/assets/b42c6a69-f16a-45b2-9aa8-6462eea6c97a" style="width: 35%;" />
  <img src="https://github.com/user-attachments/assets/6f7884a7-5204-4363-a95a-00d7aaf5a474" style="width: 35%;"/>
</div>

---

## 🧠 Características principales

- 🕒 Registro de estadísticas de uso por aplicación (tiempo total y diario)
- 🚫 Bloqueo de apps por estado: **BLOQUEADA**, **HORARIO**, **DISPONIBLE**
- ⏱️ Límite de uso diario configurable por app
- 📆 Horarios personalizados para permitir acceso a apps
- 🔐 Servicio de accesibilidad para detectar y cerrar apps no permitidas
- 💾 Persistencia local con **Room**
- 🔁 Tareas en segundo plano usando **WorkManager**
- 🗂️ Logging de bloqueos para auditoría
- 🔔 Notificaciones para recordar permisos o activar el servicio

---

## 🛠️ Tecnologías y herramientas

| Herramienta         | Descripción                                   |
|---------------------|-----------------------------------------------|
| Kotlin              | Lenguaje principal                            |
| Room (SQLite)       | Base de datos local                           |
| Dagger Hilt         | Inyección de dependencias                     |
| WorkManager         | Ejecución de tareas periódicas                |
| AccessibilityService| Detección de apps en primer plano             |
| Coroutine / Flow    | Manejo asíncrono y reactivo                   |
| Jsoup               | Scraping opcional para clasificación de apps |

---

## 📐 Arquitectura

El proyecto sigue una arquitectura modular y desacoplada:

```
📦 app
 ┣ 📂 checkers 🟢 (Validadores para bloqueo de apps)
 ┣ 📂 data 🔵 (Bases de datos y repositorios)
 ┃ ┣ 📂 apps 🔹 (Room: DAOs, entidades, DB, proveedores)
 ┃ ┗ 📂 log 🔸 (Registro de bloqueos)
 ┣ 📂 detectors 🟡 (Detectores de eventos específicos)
 ┣ 📂 di 🟣 (Inyección de dependencias con Dagger Hilt)
 ┣ 📂 handlers 🔴 (Manejo de bloqueos y acciones)
 ┣ 📂 receiver 📥 (Recepción de eventos del sistema)
 ┣ 📂 services ⚙️ (Servicios en segundo plano)
 ┣ 📂 UI 🎨 (Interfaz de usuario)
 ┃ ┣ 📂 activities 🖥️ (Pantallas principales)
 ┃ ┣ 📂 adapters 📋 (Adaptadores para listas)
 ┃ ┣ 📂 fragments 🧩 (Fragmentos reutilizables)
 ┃ ┗ 📂 viewmodel 🧠 (ViewModels compartidos)
 ┣ 📂 utils 🛠️ (Funciones y utilidades generales)
 ┣ 📂 workers ⏰ (Trabajos periódicos con WorkManager)
 ┗ 🧠 ControlParentalApp.kt 🚀 (Clase Application principal)

```

---

## 📋 Permisos necesarios

- `PACKAGE_USAGE_STATS`
- `BIND_ACCESSIBILITY_SERVICE`
- `RECEIVE_BOOT_COMPLETED`

---

---

## 📌 Diagrama de casos de uso

[Podés visualizar el diagrama de casos de uso](https://lucid.app/lucidchart/6ba2d302-7073-4598-b272-1eeeb985a417/edit?viewport_loc=-5688%2C-462%2C3647%2C2088%2CsjI~UfAdr-eT&invitationId=inv_18c096de-ea59-49dc-8db0-ff7b3636c7fe)

![Diagrama de casos de uso de Control parental](https://github.com/user-attachments/assets/67477d44-fd85-4142-8f13-0894a2624753)

---

---

## 📌 Diagrama de clases

[Podés visualizar el diagrama de clases UML usando este link](https://lucid.app/lucidchart/6ba2d302-7073-4598-b272-1eeeb985a417/edit?viewport_loc=-1924%2C336%2C4200%2C2404%2CCmMawoI6KhXr&invitationId=inv_18c096de-ea59-49dc-8db0-ff7b3636c7fe)


![Diagrama de clases](https://github.com/user-attachments/assets/5f61b5e0-33c4-4f32-9305-32a41b980c50)

---
---

## 📌 Diagrama de entidad relacion

Base de datos principal:

--

<div align="center">
  <img src="https://github.com/user-attachments/assets/424f72fe-1597-4343-a2e7-af6caab76207" style="width: 70%;" />
</div>

Otra base de datos solo para logs:
--

<div align="center">
  <img src="https://github.com/user-attachments/assets/03c43144-2d6d-4c7f-addc-19994982c2a2" style="width: 50%;" />
</div>

## 🔑 Configuración de la firma

Para compilar la aplicación con firma de lanzamiento necesitás un archivo de
keystore ubicado en `app/keyControl.jks` que **no está incluido en el repositorio**.
Las contraseñas se obtienen desde variables de entorno o desde tu archivo
`gradle.properties` local:

```properties
STORE_PASSWORD=tuStorePassword
KEY_PASSWORD=tuKeyPassword
```

También podés exportar `STORE_PASSWORD` y `KEY_PASSWORD` antes de ejecutar
`./gradlew assembleRelease`.

---

## 🧑‍💻 Autor

**Gleb Ursol**  
📍 Buenos Aires, Argentina  
🎓 Analista de Sistemas | Estudiante de Escuela Da Vinci

---

## 📄 Licencia

Este proyecto se publica bajo la [MIT License](LICENSE).
