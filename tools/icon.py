#!/usr/bin/env python3
"""Render the launcher icon: pearl mass with an S wiped out of it, red velvet showing through.

Same idea as the splash shader. Writes adaptive-icon layers and legacy icons into res/mipmap-*.
Requires: pip install numpy pillow scipy
"""
import pathlib
import numpy as np
from PIL import Image
from scipy.ndimage import gaussian_filter

RES = pathlib.Path(__file__).resolve().parent.parent / "app/src/main/res"
N = 864  # render size of the 108dp adaptive layer (x8), downsampled later
rng = np.random.default_rng(11)


def velvet(n):
    x = np.linspace(0, 1, n)[None, :].repeat(n, 0)
    y = np.linspace(0, 1, n)[:, None].repeat(n, 1)
    folds = 0.5 + 0.5 * np.sin(x * 2 * np.pi * 5.5 + 0.6 * np.sin(y * 3))
    k = folds ** 1.4
    r = 0.22 + 0.42 * k
    g = 0.015 + 0.03 * k
    b = 0.03 + 0.045 * k
    top = 1 - 0.45 * (1 - y) ** 3          # darker top
    vign = 1 - 0.35 * ((x - .5) ** 2 + (y - .5) ** 2) * 2
    img = np.stack([r, g, b], -1) * (top * vign)[..., None]
    return np.clip(img, 0, 1)


S_POINTS = [(0.665, 0.335), (0.60, 0.275), (0.49, 0.255), (0.385, 0.29), (0.345, 0.37), (0.39, 0.45),
            (0.50, 0.495), (0.61, 0.54), (0.655, 0.62), (0.615, 0.705), (0.51, 0.745), (0.40, 0.73), (0.335, 0.67)]


def s_path(n, steps=400):
    """Catmull-Rom spline through S_POINTS, in pixels."""
    p = np.array(S_POINTS) * n
    p = np.vstack([p[0], p, p[-1]])
    out = []
    for i in range(1, len(p) - 2):
        p0, p1, p2, p3 = p[i - 1], p[i], p[i + 1], p[i + 2]
        for t in np.linspace(0, 1, steps // (len(p) - 3), endpoint=False):
            t2, t3 = t * t, t * t * t
            out.append(0.5 * ((2 * p1) + (-p0 + p2) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t2 + (-p0 + 3 * p1 - 3 * p2 + p3) * t3))
    out.append(p[-2])
    return out


def mass_with_s(n):
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float32)
    lumps = gaussian_filter(rng.standard_normal((n, n)), n / 18)
    h = 1 + 0.9 * lumps / np.abs(lumps).max() * 0.12
    # Wipe along the S with a soft brush; push material into ridges.
    r = n * 0.058
    pts = s_path(n)
    removed = np.zeros_like(h)
    for x, y in pts:
        d = np.hypot(xx - x, yy - y) / r
        k = np.clip(1 - d ** 2, 0, None) ** 2 * 0.22
        take = h * k
        h -= take
        removed += take
    ridge = gaussian_filter(removed, n / 55)
    dist_mask = (gaussian_filter((removed > 0.02).astype(np.float32), n / 45))
    h += ridge * 0.9 * (1 - (removed > 0.05)) * (dist_mask > 0.05)
    h = gaussian_filter(h, n / 400)
    return np.clip(h, 0, None)


def shade(h):
    gy, gx = np.gradient(h)
    s = 60.0
    nx, ny, nz = -gx * s, -gy * s, np.ones_like(h)
    inv = 1 / np.sqrt(nx ** 2 + ny ** 2 + nz ** 2)
    nx, ny, nz = nx * inv, ny * inv, nz * inv
    L = np.array([-0.45, -0.62, 0.64]); L /= np.linalg.norm(L)
    H = L + np.array([0, 0, 1.0]); H /= np.linalg.norm(H)
    diff = np.clip(nx * L[0] + ny * L[1] + nz * L[2], 0, 1)
    spec = np.clip(nx * H[0] + ny * H[1] + nz * H[2], 0, 1) ** 36
    cav = (gaussian_filter(h, 3) - h) * 25
    sh = 0.5 + 0.55 * diff - cav
    col = np.stack([0.95 * sh + 0.8 * spec, 0.925 * sh + 0.8 * spec, 0.885 * sh + 0.82 * spec], -1)
    a = np.clip((h - 0.05) / 0.25, 0, 1)
    a = a * a * (3 - 2 * a)
    return np.clip(col, 0, 1), a


def save(img, path, size):
    path.parent.mkdir(parents=True, exist_ok=True)
    img.resize((size, size), Image.LANCZOS).save(path)


if __name__ == "__main__":
    bg = velvet(N)
    h = mass_with_s(N)
    col, a = shade(h)
    fg = np.concatenate([col, a[..., None]], -1)
    bg_img = Image.fromarray((bg * 255).astype(np.uint8), "RGB").convert("RGBA")
    fg_img = Image.fromarray((fg * 255).astype(np.uint8), "RGBA")
    full = Image.alpha_composite(bg_img, fg_img)

    densities = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}
    for d, k in densities.items():
        save(bg_img, RES / f"mipmap-{d}/ic_launcher_background.png", int(108 * k))
        save(fg_img, RES / f"mipmap-{d}/ic_launcher_foreground.png", int(108 * k))
        # Legacy square icon: crop the adaptive safe area (72 of 108dp).
        m = N * 18 // 108
        legacy = full.crop((m, m, N - m, N - m))
        save(legacy, RES / f"mipmap-{d}/ic_launcher.png", int(48 * k))
    (RES / "mipmap-anydpi-v26").mkdir(parents=True, exist_ok=True)
    (RES / "mipmap-anydpi-v26/ic_launcher.xml").write_text(
        '<?xml version="1.0" encoding="utf-8"?>\n'
        '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
        '    <background android:drawable="@mipmap/ic_launcher_background" />\n'
        '    <foreground android:drawable="@mipmap/ic_launcher_foreground" />\n'
        '</adaptive-icon>\n')
    # Preview: the icon as a launcher would mask it (circle).
    prev = full.resize((512, 512), Image.LANCZOS)
    mask = Image.new("L", (512, 512), 0)
    from PIL import ImageDraw
    ImageDraw.Draw(mask).ellipse((0, 0, 511, 511), fill=255)
    out = Image.new("RGBA", (512, 512), (0, 0, 0, 0)); out.paste(prev, (0, 0), mask)
    out.save(pathlib.Path(__file__).resolve().parent / "icon_preview.png")
    print("ok")
