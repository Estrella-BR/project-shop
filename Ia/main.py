
import chromadb
from chromadb.utils.embedding_functions import (
    SentenceTransformerEmbeddingFunction
)

embedding_function = SentenceTransformerEmbeddingFunction(
    model_name="sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2"
)

client = chromadb.PersistentClient(
    path="./chroma_db"
)

collection = client.get_or_create_collection(
    name="supermarket_products",
    embedding_function=embedding_function
)



def index_products(products):

    ids = []
    documents = []
    metadatas = []

    for index, product in enumerate(products):

        product_id = f"{product['seller']}_{index}"

        description = f"""
        Producto: {product['name']}.
        Cantidad: {product['cuantity']}.
        Supermercado: {product['seller']}.
        """

        ids.append(product_id)
        documents.append(description)

        metadatas.append({
            "name": product["name"],
            "seller": product["seller"],
            "price": float(product["price"])
        })

    collection.upsert(
        ids=ids,
        documents=documents,
        metadatas=metadatas
    )



from fastapi import FastAPI
from pydantic import BaseModel

app = FastAPI()

class SearchRequest(BaseModel):
    query: str
    limit: int = 5

@app.post("/search")
def search_products(request: SearchRequest):

    results = collection.query(
        query_texts=[request.query],
        n_results=request.limit
    )

    return {
        "products": results["metadatas"][0],
        "documents": results["documents"][0],
        "distances": results["distances"][0]
    }
