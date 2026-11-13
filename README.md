# 3D Visual Realism Suite — Grafika Komputer Minggu 11

Proyek Java 17+ (Maven) dengan LWJGL 3.3.3, JOML, STBImage, dan GLSL 330 core.
Memenuhi spesifikasi tugas mandiri Minggu 11 (Rendering Faces pada Visual Realism).

## Cara menjalankan di NetBeans
1. Ekstrak ZIP ini.
2. NetBeans: **File > Open Project…** lalu pilih folder `ThreeDViewing` (ikon Maven).
3. Pastikan JDK 17 atau lebih baru: klik kanan proyek > **Properties > Build > Compile** (atau Tools > Java Platforms).
4. Klik kanan proyek > **Clean and Build** (butuh internet pertama kali untuk mengunduh LWJGL + JOML ke `~/.m2`).
5. Klik kanan proyek > **Run** (F6). Main class: `com.grafkom.viewing3d.Main` (sudah diset di `pom.xml`).

Catatan:
- GPU/driver harus mendukung OpenGL 3.3 atau lebih baru.
- macOS: tambahkan `-XstartOnFirstThread` pada VM options (Properties > Actions > Run project > Set Properties > `exec.args`: `-XstartOnFirstThread -classpath %classpath ${exec.mainClass}`).

## Kontrol
| Tombol | Fungsi | Indikator |
|---|---|---|
| **B** | Phong <-> Blinn-Phong (per-pixel, Fragment Shader) | 1 |
| **F** | Flat Shading (normal per-face) <-> Smooth Shading (vertex normal averaging) | 2 |
| **Z** | Z-Buffer / Depth Test ON/OFF | 3 |
| **C** | Back-Face Culling ON/OFF | 3 |
| **T** | Tekstur ON/OFF pada objek | 4 |
| **N** | Filter tekstur Nearest <-> Bilinear + Mipmap | 4 |
| **S** | Shadow Mapping ON/OFF | 5 |
| **M** | Ganti model: Sphere / Torus / Rumah 3D | Mesh |
| SPASI | Putar objek ON/OFF (bayangan ikut bergerak) | |
| **L** | Cahaya mengorbit ON/OFF | |
| **V** | VSync ON/OFF (matikan agar FPS tidak dibatasi, untuk bab pembahasan) | |
| Mouse kiri (drag) / panah | Orbit kamera | |
| Scroll / **R** | Zoom / reset kamera | |
| ESC | Keluar | |

Status semua toggle dan FPS tampil di judul jendela.

## Tips mengamati tiap indikator (untuk screenshot laporan)
1. **Shading Model**: tekan B berkali-kali pada Sphere (tekstur dimatikan dengan T agar kilau terlihat jelas).
2. **Flat vs Smooth**: tekan F pada Sphere/Torus; pada Flat tampak segi-segi (Mach band).
3. **HSR**: tekan Z (OFF). Lantai digambar setelah objek, sehingga menimpa objek bila Z-Buffer mati. Dengan Z OFF dan C OFF, sisi belakang ikut menimpa sisi depan. Dengan Z OFF dan C ON, Sphere/Torus tetap tampak benar.
4. **Tekstur**: tekan N, lalu zoom out/orbit rendah ke lantai (checker) untuk melihat aliasing Nearest vs Mipmap.
5. **Bayangan**: tekan S; putar objek (SPASI) atau orbitkan cahaya (L).

## Struktur proyek
```
ThreeDViewing/
├── pom.xml
└── src/main/
    ├── java/com/grafkom/viewing3d/
    │   ├── Main.java                 titik masuk
    │   ├── VisualRealismApp.java     window GLFW, input, loop 2-pass render
    │   ├── ShaderProgram.java        compile/link GLSL, uniform
    │   ├── Mesh.java / MeshData.java / MeshFactory.java   mesh + vertex normal + UV (flat & smooth)
    │   ├── HSRManager.java           Z-Buffer & Back-Face Culling
    │   ├── Texture2D.java            STBImage loader, Nearest/Bilinear+Mipmap
    │   └── ShadowMapFBO.java         Depth FBO 2048x2048
    └── resources/
        ├── shaders/ phong.vert, phong.frag, depth.vert, depth.frag
        └── textures/ brick.png, floor.png
```

## Penyesuaian terhadap kode contoh di modul
- `Texture2D`: memakai `GL13.glActiveTexture` (pada modul hanya `GL11` yang diimpor, sehingga tidak akan ter-compile) dan membaca gambar dari classpath via `stbi_load_from_memory`.
- `ShadowMapFBO`: format depth `GL_DEPTH_COMPONENT32F` + pengecekan kelengkapan FBO.
- `phong.frag`: ditambah shadow (PCF 3x3, bias dinamis `max(0.005*(1-N.L), 0.0005)`, shadow factor 0.2), dan normal dibalik untuk sisi belakang.
