from PIL import Image
import numpy as np
import cv2
import sys


def remove_background(input_path, output_path,
                      tolerance=15, white_threshold=230):
    image = Image.open(input_path).convert("RGB")
    img = np.array(image)
    height, width = img.shape[:2]

    mask = np.zeros((height + 2, width + 2), np.uint8)

    lo = (tolerance, tolerance, tolerance)
    up = (tolerance, tolerance, tolerance)

    # 4-conectividad + solo máscara + rango FIJO + relleno de máscara con 255
    flags = (4
             | cv2.FLOODFILL_MASK_ONLY
             | cv2.FLOODFILL_FIXED_RANGE
             | (255 << 8))

    dummy = img.copy()  # floodFill modifica su entrada; usamos copia

    def try_fill(x, y):
        # Sólo sembrar si el píxel es realmente claro (candidato a fondo)
        if int(img[y, x].min()) >= white_threshold:
            cv2.floodFill(dummy, mask, (x, y), 0, lo, up, flags)

    # Bordes (el floodFill ignora semillas ya marcadas en la máscara)
    for x in range(width):
        try_fill(x, 0)
        try_fill(x, height - 1)
    for y in range(height):
        try_fill(0, y)
        try_fill(width - 1, y)

    alpha = np.where(mask[1:-1, 1:-1] == 255, 0, 255).astype(np.uint8)
    rgba = np.dstack((img, alpha))
    Image.fromarray(rgba).save(output_path, "PNG")


if __name__ == "__main__":
    # Uso: python script.py entrada.jpg salida.png [tolerancia] [umbral_blanco]
    tol = int(sys.argv[3]) if len(sys.argv) > 3 else 15
    wthr = int(sys.argv[4]) if len(sys.argv) > 4 else 230
    remove_background(sys.argv[1], sys.argv[2], tol, wthr)