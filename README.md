👔 My Perfect Outfit

Tu Asesor de Imagen y Estilista Personal Inteligente impulsado por Azure OpenAI y Jetpack Compose
 ![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg)  ![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material3-brightgreen.svg)  ![Android SDK](https://img.shields.io/badge/Android%20SDK-35-blue.svg)  ![Architecture](https://img.shields.io/badge/Architecture-MVVM%20%2B%20Clean-orange.svg)  ![Room Database](https://img.shields.io/badge/Database-Room%20v20-red.svg)  ![Azure OpenAI](https://img.shields.io/badge/AI%20Backend-Azure%20OpenAI%20%2B%20Firebase-lightgrey.svg) 

📌 Descripción General
My Perfect Outfit es una aplicación móvil nativa para Android diseñada para revolucionar la gestión del armario personal y la creación de outfits diarios. Combinando Jetpack Compose para una experiencia visual fluida y moderna, y el poder de Azure OpenAI (vía Firebase Cloud Functions), la aplicación actúa como un estilista personal de lujo que conoce el inventario exacto del usuario, sus hábitos de vestimenta, reglas de estilo y compromisos en el calendario.

✨ Funcionalidades Probadas y Validadas

🛍️ 1. Armario Digital Inteligente
•
Categorización Completa: Organización de Prendas Superiores (Camisas/Tops), Inferiores (Pantalones), Calzado, Chaquetas/Abrigos, Bolsos/Carteras, Vestidos, Faldas, Corbatas, Relojes y Fragancias.
•
Categorías Personalizadas Flexibles: Creación de categorías a medida con campos y atributos ilimitados dinámicos (CustomCategoryEntity y CustomGarmentEntity).
•
Ficha Técnica de Prenda: Registro detallado de marca, tipo/subtipo, color primario y secundario, estampado, material, estilo de corte, nivel de formalidad y fotografía.
•
Control de Estado de Lavandería (LaundryState): Clasificación en tiempo real de prendas disponibles, en uso o en lavandería.
•
Modo de Visualización Configurable: Alterna entre vista de catálogo por pestañas, lista continua o combinada.
🪄 2. Generador Diario de Outfits (Daily Outfit)
•
Recomendaciones con Inteligencia Artificial: Generación de propuestas de outfits armoniosas explicando la teoría del color, ocasión y combinación de texturas.
•
Fijación de Prendas Base: Permite al usuario seleccionar prendas específicas obligatorias alrededor de las cuales la IA debe construir el resto del outfit.
•
Instrucciones y Preferencias Diarias: Soporte para restricciones personalizadas por clima, nivel de formalidad, eventos o prendas a omitir (ej. "hace calor, omitir chaqueta y corbata").
•
Prevención de Repetición: Filtra automáticamente combinaciones históricas sugeridas previamente para garantizar outfits variados.
•
Mapeo Automatizado (SELECCION_IDS): Extracción y vinculación automática de los IDs numéricos del inventario para visualización y confirmación en un toque.
💬 3. Asesoría de Estilo Interactiva (AI Style Chat)
•
Chatbot de Estilo Multimodal: Consulta interactiva con el estilista de IA que mantiene contexto del armario completo, reglas de estilo activas, historial de uso y viajes planificados.
•
Guardrail de Visión con ML Kit: Evaluación automática de imágenes subidas para verificar que muestren ropa o accesorios antes de enviarlas al servidor ([NON_GARMENT_IMAGE]).
•
Historial de Conversación Persistente: Guardado local en Room (chat_messages) con prendas sugeridas vinculadas directamente a la interfaz.
📅 4. Planificador Multi-Día (Calendar Planning)
•
Coordinación de Vestimenta para Múltiples Días: Generación secuencial de outfits para un rango de fechas específicas sin repetir combinaciones.
•
Confirmación e Integración: Permite guardar y confirmar looks futuros en el calendario del usuario.
🧳 5. Asistente Inteligente de Maletas para Viajes (Suitcase Assistant)
•
Listas de Empaque Optimizadas: Creación de maletas según destino, días de estancia, tipo de equipaje (Carry-on vs Documentado), estilo de viaje y eventos especiales.
•
Cálculo de Esenciales y Sugerencias de Faltantes: Identifica prendas faltantes recomendadas para comprar o incluir antes del viaje.
•
Sustitución de Prendas: Sugerencias inteligentes para reemplazar prendas específicas en la maleta manteniendo la armonía del equipaje.
📜 6. Reglas de Estilo Personalizadas
•
Normas Globales de Vestimenta: Permite definir y activar/desactivar reglas globales que la IA debe respetar en todas sus recomendaciones (ej. "Nunca combinar calzado formal con pantalones cargo").
🧺 7. Gestión de Lavandería (Laundry Manager)
•
Módulo de Lavado: Pantalla dedicada para gestionar ropa sucia y reincorporar prendas lavadas al inventario disponible.
☁️ 8. Respaldo y Seguridad
•
Sincronización con Google Drive: Copia de seguridad y restauración de la base de datos a través de Google Drive API y Google Auth.
•
Seguridad de Datos: Almacenamiento cifrado de tokens sensibles mediante EncryptedSharedPreferences.
•
Protección con Firebase App Check: Verificación de integridad mediante Play Integrity API.
🔔 9. Recordatorios en Segundo Plano
•
WorkManager (TripNotificationWorker): Notificaciones locales para viajes planificados y recordatorios de empaque.

📄 Licencia
Este proyecto está distribuido bajo los términos de la licencia privada / comercial de su autor. Todos los derechos reservados.
