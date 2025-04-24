
# 📱 Control Parental App

Aplicación Android de control parental desarrollada en **Kotlin**, utilizando **Room**, **WorkManager**, **Dagger Hilt** y servicios de accesibilidad para supervisar, limitar y registrar el uso de aplicaciones.

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
 ┣ 📂 data
 ┃ ┣ 📂 local (Room DAOs, entidades, DB)
 ┃ ┗ 📂 log   (Base de datos de logs)
 ┣ 📂 di      (Módulos Dagger Hilt)
 ┣ 📂 receiver
 ┣ 📂 services
 ┣ 📂 utils   (helpers, permisos, logging, etc.)
 ┣ 📂 workers
 ┗ 🧠 repositories (AppDataRepository, LogDataRepository)
```

---

## 📋 Permisos necesarios

- `PACKAGE_USAGE_STATS`
- `BIND_ACCESSIBILITY_SERVICE`
- `RECEIVE_BOOT_COMPLETED`

---

## 📌 Diagrama de clases

Podés visualizar el diagrama de arquitectura UML en [MermaidChart](https://www.mermaidchart.com/) usando este archivo:

📄 [`control_parental_all_methods_members_full.mmd`](./control_parental_all_methods_members_full.mmd)

---

## 🤝 Contribuciones

¡Se aceptan mejoras! Abrí un `issue` o mandá un `pull request` con tus sugerencias o mejoras.

---

## 🧑‍💻 Autor

**Gleb Ursol**  
📍 Buenos Aires, Argentina  
🎓 Analista de Sistemas | Estudiante de Escuela Da Vinci

---

## 📄 Licencia

Este proyecto se publica bajo la [MIT License](LICENSE).
