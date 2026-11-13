package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL30.*;

/** Membungkus Vertex + Fragment Shader GLSL (dibaca dari classpath: src/main/resources). */
public class ShaderProgram {

    private final int programId;
    private final Map<String, Integer> uniformCache = new HashMap<>();

    public ShaderProgram(String vertexResource, String fragmentResource) {
        int vs = compile(GL_VERTEX_SHADER, loadSource(vertexResource), vertexResource);
        int fs = compile(GL_FRAGMENT_SHADER, loadSource(fragmentResource), fragmentResource);
        programId = glCreateProgram();
        glAttachShader(programId, vs);
        glAttachShader(programId, fs);
        glLinkProgram(programId);
        if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE) {
            throw new IllegalStateException("Gagal link shader program: " + glGetProgramInfoLog(programId, 4096));
        }
        glDetachShader(programId, vs);
        glDetachShader(programId, fs);
        glDeleteShader(vs);
        glDeleteShader(fs);
    }

    private static String loadSource(String resourcePath) {
        try (InputStream in = ShaderProgram.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalStateException("File shader tidak ditemukan di classpath: " + resourcePath);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Gagal membaca shader: " + resourcePath, e);
        }
    }

    private static int compile(int type, String source, String name) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new IllegalStateException("Gagal compile shader " + name + ":\n" + glGetShaderInfoLog(shader, 4096));
        }
        return shader;
    }

    public void use() {
        glUseProgram(programId);
    }

    private int location(String name) {
        Integer loc = uniformCache.get(name);
        if (loc == null) {
            loc = glGetUniformLocation(programId, name);
            uniformCache.put(name, loc);
        }
        return loc;
    }

    public void setInt(String name, int value) {
        glUniform1i(location(name), value);
    }

    public void setBool(String name, boolean value) {
        glUniform1i(location(name), value ? 1 : 0);
    }

    public void setVec3(String name, float x, float y, float z) {
        glUniform3f(location(name), x, y, z);
    }

    public void setMat4(String name, Matrix4f matrix) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer fb = stack.mallocFloat(16);
            matrix.get(fb);
            glUniformMatrix4fv(location(name), false, fb);
        }
    }

    public void dispose() {
        glDeleteProgram(programId);
    }
}
