package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * 3D Visual Realism Suite:
 *  1. Shading Model (Phong / Blinn-Phong)      -> tombol B
 *  2. Flat vs Smooth Shading                   -> tombol F
 *  3. Hidden Surface Removal (Z-Buffer, Cull)  -> tombol Z dan C
 *  4. Texture Mapping + Filtering              -> tombol T dan N
 *  5. Real-Time Shadow Mapping (2-pass, FBO)   -> tombol S
 */
public class VisualRealismApp {

    private static final String[] MESH_NAMES = {"Sphere", "Torus", "Rumah 3D"};
    private static final float GROUND_Y = -1.0f;

    // ----- window & state input
    private long window;
    private int fbWidth = 1280;
    private int fbHeight = 720;

    private boolean useBlinnPhong = true;
    private boolean smoothShading = true;
    private boolean depthTestOn = true;
    private boolean cullFaceOn = true;
    private boolean texturedOn = true;
    private boolean bilinearMipmap = true;
    private boolean shadowsOn = true;
    private boolean autoRotate = true;
    private boolean lightOrbit = false;
    private boolean vsyncOn = true;
    private int meshIndex = 0;
    private boolean titleDirty = true;

    // ----- kamera orbit (derajat)
    private float camYaw = 45f;
    private float camPitch = 28f;
    private float camDistance = 7.5f;
    private boolean dragging = false;
    private double lastMouseX;
    private double lastMouseY;

    // ----- animasi
    private float objectRotation = 0f;
    private float lightAngle = 0.67f;

    // ----- resource GPU
    private ShaderProgram sceneShader;
    private ShaderProgram depthShader;
    private ShadowMapFBO shadowFbo;
    private Texture2D objectTexture;
    private Texture2D floorTexture;
    private Mesh[] meshes;
    private Mesh groundMesh;

    // ----- FPS
    private int frameCount = 0;
    private double fpsTimer = 0.0;
    private int currentFps = 0;

    public void run() {
        try {
            init();
            loop();
        } finally {
            cleanup();
        }
    }

    // ================================================================== INIT

    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) {
            throw new IllegalStateException("Gagal menginisialisasi GLFW");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        glfwWindowHint(GLFW_SAMPLES, 4);

        window = glfwCreateWindow(fbWidth, fbHeight, "3D Visual Realism Suite", NULL, NULL);
        if (window == NULL) {
            throw new IllegalStateException("Gagal membuat window (GPU/driver harus mendukung OpenGL 3.3)");
        }

        glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> onKey(key, action));
        glfwSetMouseButtonCallback(window, (win, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                dragging = (action == GLFW_PRESS);
            }
        });
        glfwSetCursorPosCallback(window, (win, x, y) -> {
            if (dragging) {
                camYaw -= (float) (x - lastMouseX) * 0.4f;
                camPitch += (float) (y - lastMouseY) * 0.4f;
                camPitch = Math.max(3f, Math.min(85f, camPitch));
            }
            lastMouseX = x;
            lastMouseY = y;
        });
        glfwSetScrollCallback(window, (win, dx, dy) -> {
            camDistance -= (float) dy * 0.5f;
            camDistance = Math.max(2.5f, Math.min(20f, camDistance));
        });
        glfwSetFramebufferSizeCallback(window, (win, newW, newH) -> {
            fbWidth = newW;
            fbHeight = newH;
        });

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        GL.createCapabilities();

        int[] w = new int[1];
        int[] h = new int[1];
        glfwGetFramebufferSize(window, w, h);
        fbWidth = w[0];
        fbHeight = h[0];

        glEnable(GL_MULTISAMPLE);

        sceneShader = new ShaderProgram("/shaders/phong.vert", "/shaders/phong.frag");
        depthShader = new ShaderProgram("/shaders/depth.vert", "/shaders/depth.frag");
        sceneShader.use();
        sceneShader.setInt("textureSampler", 0);
        sceneShader.setInt("shadowMap", 1);
        sceneShader.setVec3("lightColor", 1.0f, 1.0f, 1.0f);
        sceneShader.setVec3("objectColor", 0.9f, 0.55f, 0.25f);

        shadowFbo = new ShadowMapFBO();
        objectTexture = new Texture2D("/textures/brick.png");
        floorTexture = new Texture2D("/textures/floor.png");

        meshes = new Mesh[]{
            new Mesh(MeshFactory.sphere(36, 24, 1.0f, 4f, 2f)),
            new Mesh(MeshFactory.torus(48, 24, 0.75f, 0.32f, 6f, 2f)),
            new Mesh(MeshFactory.house())
        };
        groundMesh = new Mesh(MeshFactory.ground(6.0f, GROUND_Y, 8f));

        printControls();
    }

    private void printControls() {
        System.out.println("=== 3D Visual Realism Suite - Kontrol ===");
        System.out.println("B      : Phong <-> Blinn-Phong");
        System.out.println("F      : Flat <-> Smooth Shading");
        System.out.println("Z      : Z-Buffer (Depth Test) ON/OFF");
        System.out.println("C      : Back-Face Culling ON/OFF");
        System.out.println("T      : Tekstur ON/OFF (pada objek)");
        System.out.println("N      : Filter tekstur Nearest <-> Bilinear+Mipmap");
        System.out.println("S      : Shadow Mapping ON/OFF");
        System.out.println("M      : Ganti model (Sphere / Torus / Rumah)");
        System.out.println("SPASI  : Putar objek ON/OFF   L : Cahaya mengorbit ON/OFF");
        System.out.println("V      : VSync ON/OFF (matikan untuk mengukur FPS)");
        System.out.println("Mouse kiri (drag) / panah : orbit kamera, scroll : zoom, R : reset kamera");
        System.out.println("ESC    : keluar");
    }

    // ================================================================== INPUT

    private void onKey(int key, int action) {
        if (action != GLFW_PRESS) {
            return;
        }
        switch (key) {
            case GLFW_KEY_ESCAPE:
                glfwSetWindowShouldClose(window, true);
                break;
            case GLFW_KEY_B:
                useBlinnPhong = !useBlinnPhong;
                break;
            case GLFW_KEY_F:
                smoothShading = !smoothShading;
                break;
            case GLFW_KEY_Z:
                depthTestOn = !depthTestOn;
                break;
            case GLFW_KEY_C:
                cullFaceOn = !cullFaceOn;
                break;
            case GLFW_KEY_T:
                texturedOn = !texturedOn;
                break;
            case GLFW_KEY_N:
                bilinearMipmap = !bilinearMipmap;
                objectTexture.setFilterMode(bilinearMipmap);
                floorTexture.setFilterMode(bilinearMipmap);
                break;
            case GLFW_KEY_S:
                shadowsOn = !shadowsOn;
                break;
            case GLFW_KEY_M:
                meshIndex = (meshIndex + 1) % meshes.length;
                break;
            case GLFW_KEY_SPACE:
                autoRotate = !autoRotate;
                break;
            case GLFW_KEY_L:
                lightOrbit = !lightOrbit;
                break;
            case GLFW_KEY_V:
                vsyncOn = !vsyncOn;
                glfwSwapInterval(vsyncOn ? 1 : 0);
                break;
            case GLFW_KEY_R:
                camYaw = 45f;
                camPitch = 28f;
                camDistance = 7.5f;
                break;
            default:
                return;
        }
        titleDirty = true;
    }

    private void pollContinuousInput(float dt) {
        float speed = 90f * dt;
        if (glfwGetKey(window, GLFW_KEY_LEFT) == GLFW_PRESS) {
            camYaw += speed;
        }
        if (glfwGetKey(window, GLFW_KEY_RIGHT) == GLFW_PRESS) {
            camYaw -= speed;
        }
        if (glfwGetKey(window, GLFW_KEY_UP) == GLFW_PRESS) {
            camPitch = Math.min(85f, camPitch + speed);
        }
        if (glfwGetKey(window, GLFW_KEY_DOWN) == GLFW_PRESS) {
            camPitch = Math.max(3f, camPitch - speed);
        }
    }

    // ================================================================== LOOP

    private void loop() {
        double last = glfwGetTime();
        fpsTimer = last;
        while (!glfwWindowShouldClose(window)) {
            double now = glfwGetTime();
            float dt = (float) (now - last);
            last = now;

            glfwPollEvents();
            pollContinuousInput(dt);

            if (autoRotate) {
                objectRotation += dt * 0.6f;
            }
            if (lightOrbit) {
                lightAngle += dt * 0.5f;
            }

            float lightX = (float) (6.5 * Math.cos(lightAngle));
            float lightY = 8.0f;
            float lightZ = (float) (6.5 * Math.sin(lightAngle));

            Matrix4f objectModel = new Matrix4f().translate(0f, 0.25f, 0f).rotateY(objectRotation);

            // Matriks ruang cahaya: Ortografik (directional) + lookAt dari posisi cahaya
            Matrix4f lightProjection = new Matrix4f().ortho(-10f, 10f, -10f, 10f, 1.0f, 30.0f);
            Matrix4f lightView = new Matrix4f().lookAt(lightX, lightY, lightZ, 0f, 0f, 0f, 0f, 1f, 0f);
            Matrix4f lightSpace = new Matrix4f(lightProjection).mul(lightView);

            renderShadowPass(objectModel, lightSpace);
            renderScenePass(objectModel, lightSpace, lightX, lightY, lightZ);

            glfwSwapBuffers(window);

            frameCount++;
            if (now - fpsTimer >= 0.5) {
                currentFps = (int) Math.round(frameCount / (now - fpsTimer));
                frameCount = 0;
                fpsTimer = now;
                titleDirty = true;
            }
            if (titleDirty) {
                updateTitle();
                titleDirty = false;
            }
        }
    }

    // ---------------------------------------------------------------- Render Pass 1 (Depth Pass)

    private void renderShadowPass(Matrix4f objectModel, Matrix4f lightSpace) {
        if (!shadowsOn) {
            return;
        }
        shadowFbo.bindForWriting();
        HSRManager.setupDepthTest(true);
        HSRManager.setupBackFaceCulling(false);

        depthShader.use();
        depthShader.setMat4("lightSpaceMatrix", lightSpace);
        depthShader.setMat4("model", objectModel);
        meshes[meshIndex].draw(smoothShading);   // lantai tidak menjadi pembayang

        shadowFbo.unbind(fbWidth, fbHeight);
    }

    // ---------------------------------------------------------------- Render Pass 2 (Lighting Pass)

    private void renderScenePass(Matrix4f objectModel, Matrix4f lightSpace,
                                 float lightX, float lightY, float lightZ) {
        if (fbWidth == 0 || fbHeight == 0) {
            return; // window diminimalkan
        }
        glViewport(0, 0, fbWidth, fbHeight);
        glClearColor(0.55f, 0.70f, 0.88f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // Hidden Surface Removal sesuai tombol Z dan C
        HSRManager.setupDepthTest(depthTestOn);
        HSRManager.setupBackFaceCulling(cullFaceOn);

        double yaw = Math.toRadians(camYaw);
        double pitch = Math.toRadians(camPitch);
        float eyeX = (float) (camDistance * Math.cos(pitch) * Math.sin(yaw));
        float eyeY = (float) (camDistance * Math.sin(pitch));
        float eyeZ = (float) (camDistance * Math.cos(pitch) * Math.cos(yaw));

        Matrix4f view = new Matrix4f().lookAt(eyeX, eyeY, eyeZ, 0f, 0f, 0f, 0f, 1f, 0f);
        float aspect = (float) fbWidth / (float) fbHeight;
        Matrix4f projection = new Matrix4f().perspective((float) Math.toRadians(45.0), aspect, 0.1f, 100.0f);

        sceneShader.use();
        sceneShader.setMat4("view", view);
        sceneShader.setMat4("projection", projection);
        sceneShader.setMat4("lightSpaceMatrix", lightSpace);
        sceneShader.setVec3("lightPos", lightX, lightY, lightZ);
        sceneShader.setVec3("viewPos", eyeX, eyeY, eyeZ);
        sceneShader.setBool("useBlinnPhong", useBlinnPhong);
        sceneShader.setBool("useShadow", shadowsOn);

        // Unit 1 = shadow map, unit 0 = tekstur (bind unit 0 terakhir)
        shadowFbo.bindDepthTexture(1);

        // Objek digambar lebih dulu, lantai sesudahnya: bila Z-Buffer dimatikan,
        // lantai akan menimpa objek sehingga efek HSR mudah diamati.
        sceneShader.setMat4("model", objectModel);
        sceneShader.setBool("useTexture", texturedOn);
        objectTexture.bind(0);
        meshes[meshIndex].draw(smoothShading);

        sceneShader.setMat4("model", new Matrix4f());
        sceneShader.setBool("useTexture", true);
        floorTexture.bind(0);
        groundMesh.draw(true);
    }

    // ================================================================== UTIL

    private void updateTitle() {
        String title = "3D Visual Realism Suite | Model: " + MESH_NAMES[meshIndex]
                + " | Shading: " + (useBlinnPhong ? "Blinn-Phong" : "Phong")
                + " | Normal: " + (smoothShading ? "Smooth" : "Flat")
                + " | Z-Buffer: " + (depthTestOn ? "ON" : "OFF")
                + " | Cull: " + (cullFaceOn ? "ON" : "OFF")
                + " | Tekstur: " + (texturedOn ? "ON" : "OFF")
                + " (" + (bilinearMipmap ? "Bilinear+Mipmap" : "Nearest") + ")"
                + " | Shadow: " + (shadowsOn ? "ON" : "OFF")
                + " | VSync: " + (vsyncOn ? "ON" : "OFF")
                + " | FPS: " + currentFps;
        glfwSetWindowTitle(window, title);
    }

    private void cleanup() {
        if (meshes != null) {
            for (Mesh m : meshes) {
                m.dispose();
            }
        }
        if (groundMesh != null) {
            groundMesh.dispose();
        }
        if (objectTexture != null) {
            objectTexture.dispose();
        }
        if (floorTexture != null) {
            floorTexture.dispose();
        }
        if (shadowFbo != null) {
            shadowFbo.dispose();
        }
        if (sceneShader != null) {
            sceneShader.dispose();
        }
        if (depthShader != null) {
            depthShader.dispose();
        }
        if (window != NULL) {
            glfwFreeCallbacks(window);
            glfwDestroyWindow(window);
        }
        glfwTerminate();
        GLFWErrorCallback cb = glfwSetErrorCallback(null);
        if (cb != null) {
            cb.free();
        }
    }
}
