#!/usr/bin/env python3
"""Genera todo el arte de EX2 Lite (iconos, ilustraciones, fondo) a SVG y lo convierte a PNG.
Uso: python3 tools/make_art.py   (requiere rsvg-convert)."""
import math, random, subprocess, pathlib, re

ROOT = pathlib.Path(__file__).resolve().parent.parent
RES = ROOT / "app" / "res"
ICON = ROOT / "icon"
NODPI = RES / "drawable-nodpi"
NODPI.mkdir(parents=True, exist_ok=True)

def png(svg_path, out, w, h=None):
    cmd = ["rsvg-convert", "-w", str(w)]
    if h: cmd += ["-h", str(h)]
    subprocess.run(cmd + [str(svg_path), "-o", str(out)], check=True)

# ------------------------------------------------------------------ zorro
frag = (ICON / "fox_head.svgfrag").read_text()
defs = """<defs>
  <linearGradient id="fur" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#FFA03A"/><stop offset="1" stop-color="#F26A00"/></linearGradient>
  <radialGradient id="bgrad" cx="50%" cy="38%" r="75%"><stop offset="0" stop-color="#2A2F3A"/><stop offset=".55" stop-color="#12151B"/><stop offset="1" stop-color="#07080A"/></radialGradient>
  <radialGradient id="glow" cx="50%" cy="52%" r="50%"><stop offset="0" stop-color="#FF8000" stop-opacity=".55"/><stop offset="1" stop-color="#FF8000" stop-opacity="0"/></radialGradient>
  <linearGradient id="ring" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#FFB066"/><stop offset=".5" stop-color="#FF8000" stop-opacity=".25"/><stop offset="1" stop-color="#FF8000" stop-opacity=".8"/></linearGradient>
</defs>"""
swoosh = '<path d="M120 470 Q256 458 392 470" stroke="#FF8000" stroke-width="5" fill="none" stroke-linecap="round" opacity=".9"/>'

ifrag = (ICON / "fox_icon.svgfrag").read_text()
idefs = """<defs>
  <linearGradient id="pap" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#FFB04A"/><stop offset=".55" stop-color="#FF8000"/><stop offset="1" stop-color="#E05A00"/></linearGradient>
  <radialGradient id="shine" cx="30%" cy="18%" r="70%"><stop offset="0" stop-color="#FFFFFF" stop-opacity=".45"/><stop offset="1" stop-color="#FFFFFF" stop-opacity="0"/></radialGradient>
</defs>"""

def icon_svg(rx):
    return f"""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="512" height="512">{idefs}
<rect width="512" height="512" rx="{rx}" fill="url(#pap)"/>
<rect width="512" height="512" rx="{rx}" fill="url(#shine)"/>
<g transform="translate(256 262) scale(.78) translate(-256 -262)">{ifrag}</g>
</svg>"""

(ICON / "fox.svg").write_text(icon_svg(112))
(ICON / "fox_round.svg").write_text(icon_svg(256))
(ICON / "fox_fg.svg").write_text(f"""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="512" height="512">{idefs}
<g transform="translate(256 256) scale(.8) translate(-256 -262)">{ifrag}</g></svg>""")
(ICON / "fox_bg.svg").write_text(f"""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="512" height="512">{idefs}
<rect width="512" height="512" fill="url(#pap)"/><rect width="512" height="512" fill="url(#shine)"/></svg>""")
(ICON / "fox_hero.svg").write_text(f"""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="512" height="512">{defs}
<circle cx="256" cy="262" r="250" fill="url(#glow)"/>
<g transform="translate(256 262) scale(.86) translate(-256 -262)">{frag}</g></svg>""")

for d, size in [("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)]:
    png(ICON / "fox.svg", RES / f"mipmap-{d}" / "ic_launcher.png", size)
    png(ICON / "fox_round.svg", RES / f"mipmap-{d}" / "ic_launcher_round.png", size)
png(ICON / "fox_fg.svg", NODPI / "ic_fg.png", 432)
png(ICON / "fox_bg.svg", NODPI / "ic_bg.png", 432)
png(ICON / "fox_hero.svg", NODPI / "fox_hero.png", 512)

# ------------------------------------------------------------------ ilustración: autopista al atardecer
def highway():
    W, H, HZ = 1000, 800, 470
    s = [f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {H}" width="{W}" height="{H}">']
    s.append(f'''<defs>
<linearGradient id="sky" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#070914"/><stop offset=".35" stop-color="#241033"/><stop offset=".62" stop-color="#8a2f2a"/><stop offset="1" stop-color="#ff9a2e"/></linearGradient>
<radialGradient id="sun" cx="50%" cy="50%" r="50%"><stop offset="0" stop-color="#fff0c8"/><stop offset=".45" stop-color="#ffc060"/><stop offset="1" stop-color="#ff7a10" stop-opacity="0"/></radialGradient>
<linearGradient id="ground" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#1a0d14"/><stop offset="1" stop-color="#050507"/></linearGradient>
<linearGradient id="road" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#2a1a22"/><stop offset=".25" stop-color="#14151b"/><stop offset="1" stop-color="#08090c"/></linearGradient>
<linearGradient id="edge" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#ffb060" stop-opacity="0"/><stop offset=".3" stop-color="#ff8a1f"/><stop offset="1" stop-color="#ff8000"/></linearGradient>
<filter id="blur6" x="-30%" y="-300%" width="160%" height="700%"><feGaussianBlur stdDeviation="6"/></filter><filter id="blur2"><feGaussianBlur stdDeviation="1.6"/></filter>
</defs>''')
    s.append(f'<rect width="{W}" height="{H}" fill="url(#sky)"/>')
    s.append(f'<circle cx="500" cy="{HZ-6}" r="230" fill="url(#sun)"/>')
    # estrellas
    rnd = random.Random(7)
    for _ in range(70):
        s.append(f'<circle cx="{rnd.uniform(0,W):.0f}" cy="{rnd.uniform(0,HZ*0.55):.0f}" r="{rnd.uniform(.6,1.6):.1f}" fill="#fff" opacity="{rnd.uniform(.2,.8):.2f}"/>')
    # montañas
    def mountains(seed, base, amp, color, op):
        r = random.Random(seed); x = -40; pts = [f"-40,{HZ}"]
        while x < W + 60:
            pts.append(f"{x:.0f},{base - r.uniform(0, amp):.0f}"); x += r.uniform(40, 110)
        pts.append(f"{W+60},{HZ}")
        return f'<polygon points="{" ".join(pts)}" fill="{color}" opacity="{op}"/>'
    s.append(mountains(3, HZ - 18, 90, "#3a1a2c", .85))
    s.append(mountains(11, HZ - 6, 55, "#1a0f1c", 1))
    s.append(f'<rect x="0" y="{HZ}" width="{W}" height="{H-HZ}" fill="url(#ground)"/>')
    # carretera en perspectiva
    s.append(f'<polygon points="470,{HZ} 530,{HZ} 1180,{H} -180,{H}" fill="url(#road)"/>')
    # bordes brillantes
    s.append(f'<polygon points="468,{HZ} 474,{HZ} -140,{H} -230,{H}" fill="url(#edge)" opacity=".9"/>')
    s.append(f'<polygon points="526,{HZ} 532,{HZ} 1230,{H} 1140,{H}" fill="url(#edge)" opacity=".9"/>')
    # rayas centrales
    t = 0.0
    k = 0
    while t < 1.0:
        f0 = t ** 2.1; f1 = min(1, (t + 0.07) ** 2.1)
        y0 = HZ + (H - HZ) * f0; y1 = HZ + (H - HZ) * f1
        w0 = 1.5 + 22 * f0; w1 = 1.5 + 22 * f1
        s.append(f'<polygon points="{500-w0/2:.1f},{y0:.1f} {500+w0/2:.1f},{y0:.1f} {500+w1/2:.1f},{y1:.1f} {500-w1/2:.1f},{y1:.1f}" fill="#ffd9a0" opacity="{0.35+0.6*f1:.2f}"/>')
        t += 0.12 * (1 + 0.9 * t); k += 1
    # líneas de velocidad
    rnd = random.Random(21)
    for _ in range(26):
        ang = rnd.uniform(-1.25, 1.25)
        L = rnd.uniform(160, 520); x1 = 500 + math.sin(ang) * 40; y1 = HZ + 6
        x2 = 500 + math.sin(ang) * L * 1.5; y2 = HZ + math.cos(abs(ang) * 0.6) * L * 0.55
        s.append(f'<line x1="{x1:.0f}" y1="{y1:.0f}" x2="{x2:.0f}" y2="{y2:.0f}" stroke="#ff9a2e" stroke-width="{rnd.uniform(1,3):.1f}" opacity="{rnd.uniform(.12,.4):.2f}" filter="url(#blur2)"/>')
    # brillo de horizonte
    s.append(f'<ellipse cx="500" cy="{HZ+2}" rx="200" ry="9" fill="#ffc070" opacity=".5" filter="url(#blur6)"/>')
    s.append('</svg>')
    return "\n".join(s)

# ------------------------------------------------------------------ ilustración: ciudad al anochecer
def city():
    W, H, HZ = 1000, 800, 520
    s = [f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {H}" width="{W}" height="{H}">']
    s.append('''<defs>
<linearGradient id="sky" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#060813"/><stop offset=".4" stop-color="#1b1432"/><stop offset=".7" stop-color="#6a2c48"/><stop offset="1" stop-color="#ff8f3a"/></linearGradient>
<radialGradient id="moon" cx="50%" cy="50%" r="50%"><stop offset="0" stop-color="#fff2d0"/><stop offset=".5" stop-color="#ffb060" stop-opacity=".55"/><stop offset="1" stop-color="#ff7a10" stop-opacity="0"/></radialGradient>
<linearGradient id="road" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#1c1119"/><stop offset="1" stop-color="#050507"/></linearGradient>
<filter id="blur4" x="-10%" y="-200%" width="120%" height="500%"><feGaussianBlur stdDeviation="4"/></filter><filter id="blur1" x="-5%" y="-200%" width="110%" height="500%"><feGaussianBlur stdDeviation="1.2"/></filter>
</defs>''')
    s.append(f'<rect width="{W}" height="{H}" fill="url(#sky)"/>')
    s.append(f'<circle cx="680" cy="330" r="210" fill="url(#moon)"/>')
    rnd = random.Random(5)
    for _ in range(60):
        s.append(f'<circle cx="{rnd.uniform(0,W):.0f}" cy="{rnd.uniform(0,300):.0f}" r="{rnd.uniform(.6,1.5):.1f}" fill="#fff" opacity="{rnd.uniform(.2,.7):.2f}"/>')
    def skyline(seed, base, minh, maxh, minw, maxw, color, windows):
        r = random.Random(seed); x = -20; out = []
        while x < W + 20:
            w = r.uniform(minw, maxw); h = r.uniform(minh, maxh)
            out.append(f'<rect x="{x:.0f}" y="{base-h:.0f}" width="{w:.0f}" height="{h+4:.0f}" fill="{color}"/>')
            if r.random() < 0.35:   # antena / remate
                out.append(f'<rect x="{x+w/2-2:.0f}" y="{base-h-r.uniform(14,38):.0f}" width="4" height="40" fill="{color}"/>')
            if windows:
                cols = int(w // 14); rows = int(h // 20)
                for cx in range(cols):
                    for ry in range(rows):
                        if r.random() < 0.28:
                            out.append(f'<rect x="{x+6+cx*14:.0f}" y="{base-h+8+ry*20:.0f}" width="6" height="9" fill="{r.choice(["#ffcf80","#ffb860","#fff0c0"])}" opacity="{r.uniform(.45,.95):.2f}"/>')
            x += w + r.uniform(0, 4)
        return "\n".join(out)
    s.append(skyline(2, HZ, 60, 210, 38, 80, "#3a2038", False))
    s.append(skyline(9, HZ + 12, 90, 300, 44, 96, "#1d1226", True))
    s.append(skyline(14, HZ + 40, 120, 340, 54, 110, "#0b0a10", True))
    s.append(f'<rect x="0" y="{HZ+40}" width="{W}" height="{H-HZ-40}" fill="url(#road)"/>')
    # estelas de luces en la calle
    rnd = random.Random(33)
    for i in range(16):
        y = HZ + 70 + i * 17 + rnd.uniform(-4, 4)
        x1 = rnd.uniform(-50, 600); L = rnd.uniform(160, 520)
        col = "#ff3b2e" if i % 3 else "#ffe6b0"
        sw = rnd.uniform(2.5, 6)
        s.append(f'<rect x="{x1:.0f}" y="{y:.0f}" width="{L:.0f}" height="{sw:.1f}" rx="{sw/2:.1f}" fill="{col}" opacity="{rnd.uniform(.35,.85):.2f}" filter="url(#blur1)"/>')
    for i in range(10):
        y = HZ + 78 + i * 24 + rnd.uniform(-4, 4)
        x1 = rnd.uniform(300, 1000); L = rnd.uniform(140, 420)
        sw = rnd.uniform(2.5, 5.5)
        s.append(f'<rect x="{x1-L:.0f}" y="{y:.0f}" width="{L:.0f}" height="{sw:.1f}" rx="{sw/2:.1f}" fill="#ff8a1f" opacity="{rnd.uniform(.4,.9):.2f}" filter="url(#blur1)"/>')
    s.append(f'<rect x="0" y="{HZ+36}" width="{W}" height="10" fill="#ff9a3a" opacity=".3" filter="url(#blur4)"/>')
    s.append('</svg>')
    return "\n".join(s)

# ------------------------------------------------------------------ fondo de inicio
def home_bg():
    W, H = 1600, 720
    s = [f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {H}" width="{W}" height="{H}">']
    s.append('''<defs>
<linearGradient id="b" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#11141b"/><stop offset=".6" stop-color="#090b0f"/><stop offset="1" stop-color="#050608"/></linearGradient>
<radialGradient id="g1" cx="12%" cy="108%" r="75%"><stop offset="0" stop-color="#ff7a00" stop-opacity=".55"/><stop offset=".5" stop-color="#ff7a00" stop-opacity=".12"/><stop offset="1" stop-color="#ff7a00" stop-opacity="0"/></radialGradient>
<radialGradient id="g2" cx="95%" cy="-10%" r="60%"><stop offset="0" stop-color="#4a6fa5" stop-opacity=".28"/><stop offset="1" stop-color="#4a6fa5" stop-opacity="0"/></radialGradient>
<linearGradient id="st" x1="0" y1="0" x2="1" y2="0"><stop offset="0" stop-color="#ff8a1f" stop-opacity="0"/><stop offset=".5" stop-color="#ff8a1f" stop-opacity=".5"/><stop offset="1" stop-color="#ff8a1f" stop-opacity="0"/></linearGradient>
<filter id="bl"><feGaussianBlur stdDeviation="1.2"/></filter>
</defs>''')
    s.append(f'<rect width="{W}" height="{H}" fill="url(#b)"/>')
    s.append(f'<rect width="{W}" height="{H}" fill="url(#g1)"/><rect width="{W}" height="{H}" fill="url(#g2)"/>')
    # franjas diagonales de velocidad
    rnd = random.Random(4)
    for i in range(14):
        y = rnd.uniform(60, H - 40); x = rnd.uniform(-200, W * 0.7); L = rnd.uniform(260, 760)
        s.append(f'<g transform="rotate(-14 {x:.0f} {y:.0f})"><rect x="{x:.0f}" y="{y:.0f}" width="{L:.0f}" height="{rnd.uniform(1.2,3.2):.1f}" fill="url(#st)" opacity="{rnd.uniform(.12,.5):.2f}" filter="url(#bl)"/></g>')
    # rejilla de carbono muy sutil
    s.append('<g opacity=".05" stroke="#fff" stroke-width="1">')
    for x in range(0, W, 28): s.append(f'<line x1="{x}" y1="0" x2="{x}" y2="{H}"/>')
    for y in range(0, H, 28): s.append(f'<line x1="0" y1="{y}" x2="{W}" y2="{y}"/>')
    s.append('</g></svg>')
    return "\n".join(s)

# ------------------------------------------------------------------ onda de sonido
def wave():
    W, H = 1000, 120
    s = [f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {H}" width="{W}" height="{H}">']
    s.append('''<defs><linearGradient id="w" x1="0" y1="0" x2="1" y2="0"><stop offset="0" stop-color="#ff8a1f" stop-opacity="0"/><stop offset=".25" stop-color="#ff9a2e"/><stop offset=".7" stop-color="#ff8000"/><stop offset="1" stop-color="#ff8000" stop-opacity="0"/></linearGradient></defs>''')
    n = 90; bw = 6; gap = (W - n * bw) / (n - 1)
    for i in range(n):
        x = i * (bw + gap)
        v = 0.5 + 0.28 * math.sin(i * 0.31) + 0.18 * math.sin(i * 0.83 + 1.2) + 0.1 * math.sin(i * 1.9)
        v *= (0.55 + 0.45 * math.sin(math.pi * i / (n - 1)))
        h = max(6, v * 104)
        s.append(f'<rect x="{x:.1f}" y="{(H-h)/2:.1f}" width="{bw}" height="{h:.1f}" rx="3" fill="url(#w)"/>')
    s.append('</svg>')
    return "\n".join(s)

for name, fn, w, h in [("card_highway", highway, 1000, 800), ("card_city", city, 1000, 800),
                       ("bg_home", home_bg, 1600, 720), ("wave", wave, 1000, 120)]:
    p = ICON / f"{name}.svg"
    p.write_text(fn())
    png(p, NODPI / f"{name}.png", w, h)
print("arte generado")
