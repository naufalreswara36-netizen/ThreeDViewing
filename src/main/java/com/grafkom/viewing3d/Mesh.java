package com.grafkom.viewing3d;

import static org.lwjgl.opengl.GL30.*;

/**
 * Mesh GPU dengan dua VAO: Flat (normal per-face) dan Smooth (normal per-vertex hasil averaging).
 * Layout vertex: location 0 = posisi, 1 = normal, 2 = UV (interleaved, stride 8 float).
 */
public class Mesh {

    private final int flatVao;
    private final int flatVbo;
    private final int smoothVao;
    private final int smoothVbo;
    private final int vertexCount;

    public Mesh(MeshData data) {
        vertexCount = data.vertexCount();
        int[] flat = createVao(data.buildFlat());
        int[] smooth = createVao(data.buildSmooth());
        flatVao = flat[0];
        flatVbo = flat[1];
        smoothVao = smooth[0];
        smoothVbo = smooth[1];
    }

    private static int[] createVao(float[] interleaved) {
        int vao = glGenVertexArrays();
        int vbo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, interleaved, GL_STATIC_DRAW);

        int stride = MeshData.FLOATS_PER_VERTEX * Float.BYTES;
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3L * Float.BYTES);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 2, GL_FLOAT, false, stride, 6L * Float.BYTES);
        glEnableVertexAttribArray(2);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
        return new int[]{vao, vbo};
    }

    public void draw(boolean smoothShading) {
        glBindVertexArray(smoothShading ? smoothVao : flatVao);
        glDrawArrays(GL_TRIANGLES, 0, vertexCount);
        glBindVertexArray(0);
    }

    public void dispose() {
        glDeleteBuffers(flatVbo);
        glDeleteBuffers(smoothVbo);
        glDeleteVertexArrays(flatVao);
        glDeleteVertexArrays(smoothVao);
    }
}
