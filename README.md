# My Perfect Outfit 👗👔

> **Version:** August 27, 2026  
> **Status:** Stable Release  
> **Database Version:** v13  

**My Perfect Outfit** is a mobile application designed for smart wardrobe management and personalized AI-driven outfit advisory powered by Google Gemini. It empowers users to organize their wardrobe, manage laundry flows, and receive tailored daily outfit recommendations based on occasion, weather, and personal style guidelines.

---

## 🌟 Key Features

### 👤 1. User Profile & Setup
* **User Onboarding:** Initial setup for name, email, profile photo, and core clothing categories (shirts, pants, shoes, etc.).
* **Interactive Profile Picture Cropper:** Photo framing tool during profile setup to ensure proper cropping and positioning.
* **Custom Style Rules:** Define personal dress codes (e.g., *Casual Fridays*, *Casual Weekends*, *Urban/Streetwear*, etc.).

### 👗 2. Wardrobe Management
* **Flexible Categorization:** Manage default clothing categories and create custom categories (scarves, hats, eyewear, etc.).
* **Custom Category Laundry Integration:** Option to enable laundry workflows for custom user-created categories.
* **Detailed Card Zoom:** Interactive zoom view when tapping clothing item cards.
* **Bug Fixes:** *[Resolved]* Fixed an issue where switching categories in the form saved the item under the incorrect category.

### 🤖 3. AI Style Advisor (Gemini AI)
* **Smart Outfit Recommendations:** Automated generation of cohesive outfits adapted to user preferences.
* **Anchor Base Item ("Prenda Base"):** Select a mandatory item (e.g., a specific shirt, pair of pants, or tie) around which the AI designs the full outfit.
* **Daily Focus & Context:** Tailor recommendations based on daily events (important business meetings, social gatherings, weddings, parties) or weather forecasts.
* **Voice Synthesis (Text-to-Speech):** Audio playback for AI-generated recommendations and styling advice.
* **Optimized Prompt Engineering:** Prompts now filter for active categories or categories containing at least one available item.
* **Alternative Recommendations:** Quick "Generate Another Recommendation" option for alternative styling ideas.
* **Daily Tracker:** One-tap action *"Wear this outfit today"* to log daily outfits.

### 🧺 4. Laundry Management & History
* **Laundry Tracker:** Monitor items currently in the laundry workflow (shirts, pants, skirts, dresses, etc.).
* **Outfit History Log:** Complete historical log of previously worn outfit combinations.

---

## 🛠️ Technical Architecture & Security

* **Gemini API Key Security:**
  * Removed API key from `UserEntity`.
  * Secured with encrypted storage via `EncryptedSharedPreferences` backed by the **Android Keystore System**.
* **Database Management (Room):**
  * **Incremental Migration Strategy:** Safe, version-controlled database migrations handling new fields, tables, and relational changes.
  * **Architecture Refactoring:** Cleaned up and removed redundant entities and Data Access Objects (DAOs).
* **Backup & Restore System:**
  * Isolated Backup & Restore management menu (decoupled from the User Profile section).
  * **Directory Selection:** New option to export and save local wardrobe backups into user-selected directories.
* **Codebase Maintenance:** General bug fixes and resolution of compiler warnings.

---

## 🚀 Roadmap & Upcoming Features

* ⏳ **AI Avatar / Virtual Mannequin:** AI-generated mannequin providing a visual approximation of the recommended outfit. *(Pending / In Development)*





# My Perfect Outfit 👗👔

> **Versión:** 27 de agosto de 2026  
> **Estado:** Estabilizada (Versión Estable)  
> **Versión de Base de Datos:** v13  

**My Perfect Outfit** es una aplicación móvil para la gestión inteligente del guardarropa y el asesoramiento de estilo personalizado impulsado por Inteligencia Artificial (Google Gemini). Permite a los usuarios organizar sus prendas, gestionar el flujo de lavandería y recibir combinaciones de outfits adaptadas al clima, contexto y reglas de estilo personales.

---

## 🌟 Características Principales

### 👤 1. Perfil y Configuración de Usuario
* **Registro de usuario:** Configuración inicial de nombre, correo electrónico, foto de perfil y categorías predeterminadas (camisas, pantalones, zapatos, etc.).
* **Encuadre interactivo de foto:** Herramienta de ajuste y recorte al seleccionar la foto de perfil para un encuadre perfecto.
* **Reglas de estilo personalizadas:** Definición de preferencias del usuario (ej. *Viernes sin corbata*, *Fines de semana casuales*, *Estilo urbano*, etc.).

### 👗 2. Gestión del Armario (Closet)
* **Categorización flexible:** Soporte para categorías base y creación de categorías personalizadas (pañuelos, sombreros, lentes, etc.).
* **Integración con lavandería:** Opción para habilitar el flujo de lavandería en categorías personalizadas.
* **Visualización detallada:** Función de zoom interactivo al tocar la tarjeta de una prenda.
* **Corrección de errores:** *[Resuelto]* Se corrigió un error por el cual las prendas se guardaban en la categoría incorrecta al cambiar de categoría durante la edición/registro.

### 🤖 3. Asesor de Estilo con IA (Gemini AI)
* **Recomendaciones inteligentes:** Generación de atuendos completos optimizados según las características del armario.
* **Inclusión de "Prenda Base":** Opción para seleccionar una prenda específica (ej. una camisa, pantalón o corbata) sobre la cual la IA construirá la combinación.
* **Enfoque y contexto diario:** Selección del propósito del día (reuniones importantes, eventos sociales, bodas, fiestas) o condiciones climáticas.
* **Sintetizador de Voz (Text-to-Speech):** Lectura en voz alta de los consejos y respuestas generados por la IA.
* **Optimización de Prompts:** Selección inteligente que incluye únicamente categorías activas o con al menos una prenda disponible para evitar recomendaciones inviables.
* **Sugerencias alternativas:** Opción para solicitar una recomendación diferente (*"Generar otra recomendación"*).
* **Confirmación diaria:** Acción rápida *"Usar este outfit hoy"* para registrar la elección del día.

### 🧺 4. Lavandería y Historial
* **Control de prendas en lavado:** Seguimiento del estado de prendas que requieren lavado (camisas, pantalones, faldas, vestidos, etc.).
* **Historial de Outfits:** Bitácora histórica de atuendos utilizados anteriormente.

---

## 🛠️ Arquitectura Técnica y Seguridad

* **Seguridad de API Key (Gemini):**
  * La clave de API se desvinculó de la entidad `UserEntity`.
  * Se implementó almacenamiento seguro y cifrado mediante `EncryptedSharedPreferences` respaldado por el **Android Keystore System**.
* **Gestión de Base de Datos (Room):**
  * **Estrategia de migración incremental:** Control de versión incremental que agrega campos, tablas y relaciones de forma segura sin pérdida de datos.
  * **Limpieza de arquitectura:** Refactorización y eliminación de entidades y Objetos de Acceso a Datos (DAO) redundantes.
* **Respaldo y Restauración de Armario:**
  * Menú independiente dedicado exclusivamente a opciones de copia de seguridad y restauración (desvinculado de la pantalla de perfil).
  * **Nueva función:** Opción para guardar los respaldos locales directamente en carpetas seleccionables por el usuario.
* **Mantenimiento general:** Corrección de fallos menores y eliminación de advertencias de compilación (*compiler warnings*).

---

## 🚀 Próximas Funcionalidades (Roadmap)

* ⏳ **Maniquí Virtual con IA:** Generación de un avatar/maniquí sintético mediante IA para visualizar una aproximación gráfica del outfit recomendado antes de vestirlo. *(Pendiente / En desarrollo)*

