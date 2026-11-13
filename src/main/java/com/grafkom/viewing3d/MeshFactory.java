package com.grafkom.viewing3d;

/**
 * Pabrik model 3D prosedural: Sphere, Torus, Rumah 3D, dan Ground Plane.
 * Semua mesh memiliki vertex normal dan koordinat tekstur UV (v=0 di bawah, v=1 di atas)
 * dengan urutan vertex CCW pada sisi luar (kompatibel dengan Back-Face Culling GL_CCW).
 */
public final class MeshFactory {

    private MeshFactory() {
    }

    /** UV Sphere. Kutub bawah v=0, kutub atas v=1. */
    public static MeshData sphere(int slices, int stacks, float radius, float uRepeat, float vRepeat) {
        float[][][] p = new float[stacks + 1][slices + 1][];
        float[][][] uv = new float[stacks + 1][slices + 1][];
        for (int i = 0; i <= stacks; i++) {
            float v = (float) i / stacks;
            double theta = -Math.PI / 2.0 + Math.PI * v;
            float y = (float) (radius * Math.sin(theta));
            float rr = (float) (radius * Math.cos(theta));
            for (int j = 0; j <= slices; j++) {
                float u = (float) j / slices;
                double lon = 2.0 * Math.PI * u;
                p[i][j] = new float[]{(float) (rr * Math.cos(lon)), y, (float) (rr * Math.sin(lon))};
                uv[i][j] = new float[]{u * uRepeat, v * vRepeat};
            }
        }
        return fromGrid(p, uv, stacks, slices);
    }

    /** Torus (donat) dengan sumbu simetri Y. */
    public static MeshData torus(int rings, int sides, float majorR, float minorR, float uRepeat, float vRepeat) {
        float[][][] p = new float[sides + 1][rings + 1][];
        float[][][] uv = new float[sides + 1][rings + 1][];
        for (int i = 0; i <= sides; i++) {
            float v = (float) i / sides;
            double beta = 2.0 * Math.PI * v;
            for (int j = 0; j <= rings; j++) {
                float u = (float) j / rings;
                double alpha = 2.0 * Math.PI * u;
                double ring = majorR + minorR * Math.cos(beta);
                p[i][j] = new float[]{
                    (float) (ring * Math.cos(alpha)),
                    (float) (minorR * Math.sin(beta)),
                    (float) (ring * Math.sin(alpha))};
                uv[i][j] = new float[]{u * uRepeat, v * vRepeat};
            }
        }
        return fromGrid(p, uv, sides, rings);
    }

    /** Rumah 3D: prisma segilima (dinding + atap pelana), cembung. */
    public static MeshData house() {
        MeshData m = new MeshData();
        float z = 0.8f;
        float[][] front = {{-1f, -0.8f, z}, {1f, -0.8f, z}, {1f, 0.2f, z}, {0f, 0.9f, z}, {-1f, 0.2f, z}};
        float[][] back = new float[5][];
        for (int i = 0; i < 5; i++) {
            back[i] = new float[]{front[i][0], front[i][1], -z};
        }
        float[] center = {0f, 0f, 0f};

        // Dua sisi gable (segilima) -> kipas segitiga
        for (int s = 0; s < 2; s++) {
            float[][] f = (s == 0) ? front : back;
            for (int i = 1; i <= 3; i++) {
                m.addTriangleOutward(f[0], f[i], f[i + 1],
                        gableUV(f[0]), gableUV(f[i]), gableUV(f[i + 1]), center);
            }
        }
        // Lima sisi persegi panjang: alas, dinding kanan, atap kanan, atap kiri, dinding kiri
        quad(m, front[0], front[1], back[1], back[0], center);
        quad(m, front[1], back[1], back[2], front[2], center);
        quad(m, front[2], back[2], back[3], front[3], center);
        quad(m, front[3], back[3], back[4], front[4], center);
        quad(m, front[4], back[4], back[0], front[0], center);
        return m;
    }

    /** Bidang lantai (normal +Y) pada ketinggian y, UV diulang 'tiles' kali. */
    public static MeshData ground(float halfSize, float y, float tiles) {
        MeshData m = new MeshData();
        float h = halfSize;
        float[] a = {-h, y, -h};
        float[] b = {-h, y, h};
        float[] c = {h, y, h};
        float[] d = {h, y, -h};
        float[] uvA = {0f, 0f};
        float[] uvB = {0f, tiles};
        float[] uvC = {tiles, tiles};
        float[] uvD = {tiles, 0f};
        m.addTriangle(a, b, c, uvA, uvB, uvC);
        m.addTriangle(a, c, d, uvA, uvC, uvD);
        return m;
    }

    // ------------------------------------------------------------------ helper

    private static MeshData fromGrid(float[][][] p, float[][][] uv, int rows, int cols) {
        MeshData m = new MeshData();
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                float[] p00 = p[i][j];
                float[] p01 = p[i][j + 1];
                float[] p10 = p[i + 1][j];
                float[] p11 = p[i + 1][j + 1];
                m.addTriangle(p00, p10, p01, uv[i][j], uv[i + 1][j], uv[i][j + 1]);
                m.addTriangle(p10, p11, p01, uv[i + 1][j], uv[i + 1][j + 1], uv[i][j + 1]);
            }
        }
        return m;
    }

    private static float[] gableUV(float[] p) {
        return new float[]{(p[0] + 1f) / 2f, (p[1] + 0.8f) / 1.7f};
    }

    private static void quad(MeshData m, float[] a, float[] b, float[] c, float[] d, float[] center) {
        float[] uvA = {0f, 0f};
        float[] uvB = {2f, 0f};
        float[] uvC = {2f, 2f};
        float[] uvD = {0f, 2f};
        m.addTriangleOutward(a, b, c, uvA, uvB, uvC, center);
        m.addTriangleOutward(a, c, d, uvA, uvC, uvD, center);
    }
}
