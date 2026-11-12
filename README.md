# 🛒 Project Shop

**Project Shop** es un proyecto personal desarrollado con el objetivo de explorar la recopilación, organización y consulta de información sobre productos de diferentes supermercados.

La aplicación permite centralizar información de productos obtenida mediante técnicas de **web scraping**, ofreciendo funcionalidades de búsqueda, filtrado, ordenación y paginación.

El proyecto tiene fines personales, educativos y experimentales, y no está afiliado ni cuenta con autorización o colaboración de las empresas cuyos sitios web se utilizan como fuentes de información.

## ✨ Funcionalidades

- Recopilación automática de información de productos.
- Consulta de productos de diferentes supermercados.
- Búsqueda de productos por nombre.
- Aplicación de múltiples filtros.
- Ordenación de productos por precio.
- Visualización paginada de resultados.
- Centralización de información procedente de diferentes fuentes.

## 🏪 Fuentes de información

Actualmente, el proyecto contempla la recopilación de información de los siguientes supermercados:

- Mercadona.
- Carrefour.
- DIA.

Los datos se obtienen mediante técnicas de web scraping a partir de información disponible en sus sitios web.

La disponibilidad y actualización de los datos dependen de las fuentes originales y de los cambios que estas puedan realizar en sus plataformas.

## 🛠️ Tecnologías utilizadas

**Frontend**
- Angular
- TypeScript
- HTML5
- CSS3

**Backend**
- Java
- Spring Boot
- Maven
- Jsoup
- Playwright
- Jackson

## 🏗️ Arquitectura

El proyecto utiliza una arquitectura cliente-servidor:

```text
        Angular
        Frontend
           |
           | HTTP / JSON
           |
       Spring Boot
        Backend
           |
      -------------
      |           |
   Scraping    Productos
      |           |
      -------------
           |
       Archivos JSON
```

El backend se encarga de obtener y procesar la información de los productos, mientras que el frontend permite visualizarla e interactuar con ella.

## 🔗 Enlaces del proyecto

| Recurso | Enlace |
|---|---|
| Repositorio | [GitHub](https://github.com/Estrella-BR/project-shop) |
| Frontend | [URL](https://shop-project-ebr.netlify.app/) |

## ⚠️ Aviso legal y limitaciones

Project Shop es un proyecto personal e independiente, desarrollado exclusivamente con fines educativos y experimentales.

- No existe afiliación, patrocinio, colaboración ni autorización por parte de Mercadona, Carrefour, DIA u otras empresas mencionadas.
- Los datos se recopilan mediante técnicas de web scraping a partir de sus sitios web.
- No se garantiza la exactitud, integridad ni actualización de la información obtenida.
- Los nombres comerciales y marcas pertenecen a sus respectivos titulares.
- El proyecto no representa ni sustituye a las plataformas oficiales de los supermercados.

El acceso a las fuentes de información y el uso de los datos deben respetar las condiciones de uso, las restricciones técnicas y la normativa aplicable.

## 🎯 Objetivo del proyecto

Explorar el desarrollo de una aplicación web capaz de recopilar, procesar y presentar información de diferentes fuentes, poniendo en práctica tecnologías de desarrollo frontend, backend y técnicas de extracción de datos.

## 👩‍💻 Autora

Desarrollado como proyecto personal por [Estrella Blanco](https://github.com/Estrella-BR).