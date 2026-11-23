import { Product } from "../../common/model/Product";

export interface ChatMessage {
  id: number;
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
  loading?: boolean;
  products?: Product[];
}


export interface Ingredient {
  nombre: string;
  cantidad: string;
}

export interface CatalogProduct {
  nombre?: string;
  formato?: string;
  vendedor?: string;
  precio?: string | number;
  imagen?: string;
  url?: string;
}

export interface CatalogCheck {
  ingrediente: string;
  cantidad_necesaria: string;
  encontrado: boolean;
  producto: CatalogProduct | null;
}

export interface AteneaAnswer {
  plato: string;
  comensales: number;
  ingredientes_necesarios: Ingredient[];
  comprobacion_catalogo: CatalogCheck[];
}

export interface ChatApiResponse {
  ok: boolean;
  answer: AteneaAnswer | string;
}

export interface CatalogCheck {
  ingrediente: string;
  cantidad_necesaria: string;
  encontrado: boolean;
  product: {
    nombre?: string;
    formato?: string;
    vendedor?: string;
    precio?: string | number;
    imagen?: string;
    url?: string;
  } | null;
}