package com.grafkom.viewing3d;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.stb.STBImage.*;

/**
 * Memuat gambar PNG/JPG dengan STBImage lalu mengunggahnya ke GPU.
 * File dibaca dari classpath (mis. "/textures/brick.png"). Jika gagal, dipakai tekstur checker cadangan.
 */
public class Texture2D {

    private final int textureID;

    public Texture2D(String resourcePath) {
        textureID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureID);

        // Wrap & Filtering default: Bilinear + Mipmap
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        boolean ok = false;
        ByteBuffer raw = readResource(resourcePath);
        if (raw != null) {
            stbi_set_flip_vertically_on_load(true); // v=0 di bawah gambar, sesuai UV mesh
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer width = stack.mallocInt(1);
                IntBuffer height = stack.mallocInt(1);
                IntBuffer channels = stack.mallocInt(1);
                ByteBuffer data = stbi_load_from_memory(raw, width, height, channels, 4);
                if (data != null) {
                    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width.get(0), height.get(0), 0,
                            GL_RGBA, GL_UNSIGNED_BYTE, data);
                    stbi_image_free(data);
                    ok = true;
                } else {
                    System.err.println("Failed to load texture: " + resourcePath + " (" + stbi_failure_reason() + ")");
                }
            }
            MemoryUtil.memFree(raw);
        }
        if (!ok) {
            uploadFallbackChecker();
        }
        glGenerateMipmap(GL_TEXTURE_2D);
    }

    private static ByteBuffer readResource(String resourcePath) {
        try (InputStream in = Texture2D.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                System.err.println("Resource tekstur tidak ditemukan: " + resourcePath);
                return null;
            }
            byte[] bytes = in.readAllBytes();
            ByteBuffer buffer = MemoryUtil.memAlloc(bytes.length);
            buffer.put(bytes);
            buffer.flip();
            return buffer;
        } catch (IOException e) {
            System.err.println("Gagal membaca tekstur: " + resourcePath + " - " + e.getMessage());
            return null;
        }
    }

    private static void uploadFallbackChecker() {
        int size = 64;
        ByteBuffer pixels = MemoryUtil.memAlloc(size * size * 4);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                boolean light = (((x / 8) + (y / 8)) % 2) == 0;
                byte c = (byte) (light ? 230 : 60);
                pixels.put(c).put(c).put(c).put((byte) 255);
            }
        }
        pixels.flip();
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, size, size, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
        MemoryUtil.memFree(pixels);
    }

    /** true = Bilinear + Mipmapping (trilinear), false = Nearest Neighbor. */
    public void setFilterMode(boolean bilinearMipmapped) {
        glBindTexture(GL_TEXTURE_2D, textureID);
        if (bilinearMipmapped) {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        } else {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        }
    }

    public void bind(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, textureID);
    }

    public void dispose() {
        glDeleteTextures(textureID);
    }
}
