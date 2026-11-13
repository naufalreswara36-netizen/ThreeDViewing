package com.grafkom.viewing3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Menampung geometri segitiga mentah (posisi + UV) lalu membangun dua versi
 * buffer interleaved [px,py,pz, nx,ny,nz, u,v]:
 *  - Flat   : satu normal per face (per-face normal)
 *  - Smooth : normal vertex = rata-rata normal semua face yang berbagi posisi vertex
 *             N_vertex = (sum N_face_i) / ||sum N_face_i||
 * Kelas ini murni Java (tanpa OpenGL) sehingga mudah diuji.
 */
public class MeshData {

    public static final int FLOATS_PER_VERTEX = 8;

    private final List<float[]> positions = new ArrayList<>(); // 9 float per segitiga
    private final List<float[]> texCoords = new ArrayList<>(); // 6 float per segitiga

    /** Menambah segitiga dengan urutan vertex CCW (tampak depan). Segitiga degenerate diabaikan. */
    public void addTriangle(float[] a, float[] b, float[] c, float[] uvA, float[] uvB, float[] uvC) {
        float[] n = cross(sub(b, a), sub(c, a));
        if (length(n) < 1e-8f) {
            return;
        }
        positions.add(new float[]{a[0], a[1], a[2], b[0], b[1], b[2], c[0], c[1], c[2]});
        texCoords.add(new float[]{uvA[0], uvA[1], uvB[0], uvB[1], uvC[0], uvC[1]});
    }

    /**
     * Menambah segitiga untuk mesh cembung: urutan vertex otomatis dibalik bila
     * normalnya mengarah ke dalam (menuju titik interior 'center').
     */
    public void addTriangleOutward(float[] a, float[] b, float[] c,
                                   float[] uvA, float[] uvB, float[] uvC, float[] center) {
        float[] n = cross(sub(b, a), sub(c, a));
        float[] centroid = {(a[0] + b[0] + c[0]) / 3f, (a[1] + b[1] + c[1]) / 3f, (a[2] + b[2] + c[2]) / 3f};
        if (dot(n, sub(centroid, center)) >= 0f) {
            addTriangle(a, b, c, uvA, uvB, uvC);
        } else {
            addTriangle(a, c, b, uvA, uvC, uvB);
        }
    }

    public int triangleCount() {
        return positions.size();
    }

    public int vertexCount() {
        return positions.size() * 3;
    }

    /** Flat Shading: normal face yang sama untuk ketiga vertex tiap segitiga. */
    public float[] buildFlat() {
        float[] out = new float[vertexCount() * FLOATS_PER_VERTEX];
        int k = 0;
        for (int t = 0; t < positions.size(); t++) {
            float[] p = positions.get(t);
            float[] uv = texCoords.get(t);
            float[] n = faceNormal(p);
            for (int v = 0; v < 3; v++) {
                out[k++] = p[v * 3];
                out[k++] = p[v * 3 + 1];
                out[k++] = p[v * 3 + 2];
                out[k++] = n[0];
                out[k++] = n[1];
                out[k++] = n[2];
                out[k++] = uv[v * 2];
                out[k++] = uv[v * 2 + 1];
            }
        }
        return out;
    }

    /** Smooth Shading: Vertex Normal Averaging (vertex dengan posisi sama di-weld). */
    public float[] buildSmooth() {
        Map<String, float[]> sums = new HashMap<>();
        for (float[] p : positions) {
            float[] n = faceNormal(p);
            for (int v = 0; v < 3; v++) {
                String key = key(p[v * 3], p[v * 3 + 1], p[v * 3 + 2]);
                float[] s = sums.get(key);
                if (s == null) {
                    s = new float[3];
                    sums.put(key, s);
                }
                s[0] += n[0];
                s[1] += n[1];
                s[2] += n[2];
            }
        }
        float[] out = new float[vertexCount() * FLOATS_PER_VERTEX];
        int k = 0;
        for (int t = 0; t < positions.size(); t++) {
            float[] p = positions.get(t);
            float[] uv = texCoords.get(t);
            float[] fn = faceNormal(p);
            for (int v = 0; v < 3; v++) {
                float[] s = sums.get(key(p[v * 3], p[v * 3 + 1], p[v * 3 + 2]));
                float len = length(s);
                float[] n = (len < 1e-8f) ? fn : new float[]{s[0] / len, s[1] / len, s[2] / len};
                out[k++] = p[v * 3];
                out[k++] = p[v * 3 + 1];
                out[k++] = p[v * 3 + 2];
                out[k++] = n[0];
                out[k++] = n[1];
                out[k++] = n[2];
                out[k++] = uv[v * 2];
                out[k++] = uv[v * 2 + 1];
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ util vektor

    private static String key(float x, float y, float z) {
        return Math.round(x * 10000f) + "," + Math.round(y * 10000f) + "," + Math.round(z * 10000f);
    }

    private static float[] faceNormal(float[] p) {
        float[] a = {p[0], p[1], p[2]};
        float[] b = {p[3], p[4], p[5]};
        float[] c = {p[6], p[7], p[8]};
        float[] n = cross(sub(b, a), sub(c, a));
        float len = length(n);
        return new float[]{n[0] / len, n[1] / len, n[2] / len};
    }

    private static float[] sub(float[] a, float[] b) {
        return new float[]{a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    private static float[] cross(float[] u, float[] v) {
        return new float[]{
            u[1] * v[2] - u[2] * v[1],
            u[2] * v[0] - u[0] * v[2],
            u[0] * v[1] - u[1] * v[0]};
    }

    private static float dot(float[] a, float[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static float length(float[] a) {
        return (float) Math.sqrt(dot(a, a));
    }
}
