
# 📱 Control Parental App

Aplicación Android de control parental desarrollada en **Kotlin**, utilizando **Room**, **WorkManager**, **Dagger Hilt** y servicios de accesibilidad para supervisar, limitar y registrar el uso de aplicaciones.

[Propuesta de valor](https://docs.google.com/document/d/12kFZDpTqzES0-sYFv3g2N5VKG0sBjfh_hx_XMg7oy6Q/edit?usp=sharing)

[SRS](https://docs.google.com/document/d/12kFZDpTqzES0-sYFv3g2N5VKG0sBjfh_hx_XMg7oy6Q/edit?usp=sharing)

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

[Podés visualizar el diagrama de arquitectura UML usando este link](https://www.mermaidchart.com/raw/a014271e-0d37-4536-9adc-ee1455d9f9c9?theme=light&version=v0.1&format=svg)

![Diagrama de clases-contr-parent](https://github.com/user-attachments/assets/cd262fee-0fb2-4a6f-935f-26451eb0a8ef)

---
## 📌 Diagrama de casos de uso

![Diagrama de casos de uso de Control parental - Página 2](https://github.com/user-attachments/assets/16ae9efe-2349-4186-a0fb-17119e9f6e97)



---
## 📌 Diagrama de entidad relacion

![er_diagram-cont-parent](https://github.com/user-attachments/assets/424f72fe-1597-4343-a2e7-af6caab76207)

![er lod](https://github.com/user-attachments/assets/03c43144-2d6d-4c7f-addc-19994982c2a2)



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
