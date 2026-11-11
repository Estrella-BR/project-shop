from PIL import Image
import numpy as np
import cv2
import sys
import io
import argparse
import os
import urllib.request
import cloudinary
import cloudinary.uploader

# --- 1. CONFIGURACIÓN INFALIBLE DE CLOUDINARY ---
def configurar_cloudinary():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    env_path = os.path.join(script_dir, '.env')

    url = None
    if os.path.exists(env_path):
        with open(env_path, 'r', encoding='utf-8') as f:
            for line in f:
                line = line.strip()
                if line.startswith('CLOUDINARY_URL='):
                    url = line.split('=', 1)[1].strip().strip('"').strip("'")
                    break

    if not url:
        print("❌ ERROR: No se encontró CLOUDINARY_URL en el .env", file=sys.stderr)
        sys.exit(1)

    try:
        clean_url = url.replace("cloudinary://", "")
        auth, cloud_name = clean_url.split("@")
        api_key, api_secret = auth.split(":")

        cloudinary.config(
            cloud_name=cloud_name,
            api_key=api_key,
            api_secret=api_secret,
            secure=True
        )
    except Exception as e:
        print(f"❌ Error parseando CLOUDINARY_URL: {e}", file=sys.stderr)
        sys.exit(1)

configurar_cloudinary()

# --- 2. LÓGICA DE LA IMAGEN ---
def load_image(source: str) -> Image.Image:
    if source.startswith(("http://", "https://")):
        req = urllib.request.Request(
            source,
            headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'}
        )
        with urllib.request.urlopen(req, timeout=30) as resp:
            return Image.open(io.BytesIO(resp.read())).convert("RGB")
    return Image.open(source).convert("RGB")

def remove_background(source: str, output_path=None, tolerance=15, white_threshold=230,
                      public_id=None):
    image = load_image(source)
    img = np.array(image)
    height, width = img.shape[:2]

    mask = np.zeros((height + 2, width + 2), np.uint8)
    lo = (tolerance, tolerance, tolerance)
    up = (tolerance, tolerance, tolerance)

    flags = (4 | cv2.FLOODFILL_MASK_ONLY
             | cv2.FLOODFILL_FIXED_RANGE | (255 << 8))

    dummy = img.copy()

    def try_fill(x, y):
        if int(img[y, x].min()) >= white_threshold:
            cv2.floodFill(dummy, mask, (x, y), 0, lo, up, flags)

    for x in range(width):
        try_fill(x, 0)
        try_fill(x, height - 1)
    for y in range(height):
        try_fill(0, y)
        try_fill(width - 1, y)

    alpha = np.where(mask[1:-1, 1:-1] == 255, 0, 255).astype(np.uint8)
    rgba = np.dstack((img, alpha))
    result_img = Image.fromarray(rgba)

    # PNG en memoria
    buffer = io.BytesIO()
    result_img.save(buffer, format="PNG")
    buffer.seek(0)

    # Guardado local (para tu backend)
    if output_path:
        os.makedirs(os.path.dirname(os.path.abspath(output_path)), exist_ok=True)
        with open(output_path, "wb") as f:
            f.write(buffer.getvalue())
        print(f"📁 Guardado local en: {output_path}", file=sys.stderr)

    # Subida a Cloudinary
    buffer.seek(0)
    result = cloudinary.uploader.upload(
        buffer,
        public_id=public_id,
        folder="Shop",
        resource_type="image",
        overwrite=True,
        format="png",
    )

    # Validación de seguridad
    secure_url = result.get("secure_url")
    if not secure_url:
        raise ValueError("Cloudinary no devolvió una URL válida.")

    return secure_url

# --- 3. INTERFAZ DE LÍNEA DE COMANDOS ---
def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("source")
    parser.add_argument("output_path", nargs="?", default=None)
    parser.add_argument("--tol", type=int, default=15)
    parser.add_argument("--wthr", type=int, default=230)
    parser.add_argument("--public-id", default=None)

    args = parser.parse_args()

    try:
        url = remove_background(
            args.source,
            args.output_path,
            args.tol,
            args.wthr,
            args.public_id
        )

        # ESTO ES VITAL: Imprimir la URL en stdout para que Java la lea
        print(url)

    except Exception as e:
        # Imprimimos el error real en stderr para poder depurar
        print(f"❌ Error en el script Python: {e}", file=sys.stderr)
        sys.exit(1)

if __name__ == "__main__":
    main()