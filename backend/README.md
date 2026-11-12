# ⚙️ Project Shop – Backend

Backend de Project Shop, un proyecto personal desarrollado con **Java y Spring Boot**.

Se encarga de recopilar información de productos mediante técnicas de web scraping, procesarla y proporcionar los datos al frontend a través de una API REST.

## ✨ Funcionalidades

- Recopilación de productos de diferentes supermercados.
- Extracción y procesamiento de información.
- Servicios de scraping independientes por supermercado.
- Generación y almacenamiento de archivos JSON.
- Consulta de productos mediante una API REST.
- Búsqueda por nombre.
- Aplicación de múltiples filtros.
- Ordenación por precio.
- Paginación de resultados.

## 🏪 Fuentes de información

El proyecto contempla servicios de recopilación de información de:

- Mercadona.
- Carrefour.
- DIA.

La extracción se realiza mediante técnicas de web scraping, utilizando diferentes métodos según las características de cada plataforma.

## 🛠️ Tecnologías

- **Java:** lenguaje de programación.
- **Spring Boot:** desarrollo de la API REST.
- **Maven:** gestión de dependencias y construcción.
- **Jsoup:** extracción y análisis de contenido HTML.
- **Playwright:** automatización e interacción con páginas web.
- **Jackson:** procesamiento y generación de archivos JSON.

## 🏗️ Arquitectura

El backend separa la lógica de recopilación de datos, los modelos de productos, los servicios de procesamiento y los controladores REST.

```text
Backend
  |
  ├── Controller
  |     └── Endpoints REST
  |
  ├── Service
  |     ├── MercadonaScrapeService
  |     ├── CarrefourScrapService
  |     ├── DiaScrapService
  |     └── ScrapService
  |
  ├── Model
  |     └── Product
  |
  └── Datos
        └── productos.json
```

## 🔗 Enlaces

- Repositorio: [Project Shop](https://github.com/Estrella-BR/project-shop)
- Frontend: [URL](https://shop-project-ebr.netlify.app/)

## ⚠️ Aviso

Este backend forma parte de un proyecto personal y experimental.

Los datos se obtienen mediante web scraping. No existe autorización, afiliación ni colaboración con las empresas mencionadas.

El proyecto no garantiza la disponibilidad ni la exactitud de los datos obtenidos. Su uso debe respetar las condiciones de las plataformas de origen y la normativa aplicable.

Los nombres y marcas comerciales pertenecen a sus respectivos titulares.

## 👩‍💻 Autora

[Estrella Blanco](https://github.com/Estrella-BR)