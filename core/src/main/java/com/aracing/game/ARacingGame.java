package com.aracing.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;

import net.mgsx.gltf.loaders.glb.GLBLoader;
import net.mgsx.gltf.scene3d.attributes.PBRFloatAttribute;
import net.mgsx.gltf.scene3d.lights.DirectionalShadowLight;
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneAsset;
import net.mgsx.gltf.scene3d.scene.SceneManager;
import net.mgsx.gltf.scene3d.shaders.PBRShaderConfig;
import net.mgsx.gltf.scene3d.shaders.PBRShaderProvider;
import net.mgsx.gltf.scene3d.shaders.PBRDepthShaderProvider;

public class ARacingGame extends ApplicationAdapter {

    private SceneManager renderer;
    private PerspectiveCamera camera;

    private SceneAsset trackAsset;
    private SceneAsset playerAsset;
    private SceneAsset yellowAsset;
    private SceneAsset redAsset;

    private Scene track;
    private Scene player;
    private final Scene[] bots = new Scene[4];

    private DirectionalShadowLight shadowLight;

    private final Vector3 playerPosition = new Vector3();
    private final Vector3 cameraTarget = new Vector3();
    private final Vector3 cameraPosition = new Vector3();

    private float speed;
    private float steering;
    private float playerRotation;
    private float botTime;

    private static final float MAX_SPEED = 32f;
    private static final float ACCELERATION = 12f;
    private static final float BRAKE_POWER = 20f;
    private static final float BOT_RADIUS_X = 30f;
    private static final float BOT_RADIUS_Z = 18f;

    @Override
    public void create() {
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glEnable(GL20.GL_CULL_FACE);

        PBRShaderConfig colorConfig = PBRShaderProvider.createDefaultConfig();
        colorConfig.numDirectionalLights = 1;
        colorConfig.numPointLights = 0;
        colorConfig.numSpotLights = 0;

        com.badlogic.gdx.graphics.g3d.shaders.DepthShader.Config depthConfig =
                PBRShaderProvider.createDefaultDepthConfig();

        renderer = new SceneManager(
                new PBRShaderProvider(colorConfig),
                new PBRDepthShaderProvider(depthConfig)
        );
        renderer.setAmbientLight(0.35f);

        shadowLight = new DirectionalShadowLight(2048, 2048, 90f, 90f, 1f, 180f);
        shadowLight.set(Color.WHITE, new Vector3(-1f, -4f, -2f), 3.5f);
        renderer.environment.add(shadowLight);
        renderer.environment.set(new PBRFloatAttribute(
                PBRFloatAttribute.ShadowBias, 1f / 256f));

        camera = new PerspectiveCamera(67f,
                Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 0.1f;
        camera.far = 250f;
        camera.position.set(0f, 8f, -12f);
        camera.lookAt(0f, 0f, 0f);
        camera.up.set(Vector3.Y);
        camera.update();
        renderer.setCamera(camera);

        GLBLoader loader = new GLBLoader();
        trackAsset = loader.load(Gdx.files.internal("models/track.glb"));
        playerAsset = loader.load(Gdx.files.internal("models/blue_car.glb"));
        yellowAsset = loader.load(Gdx.files.internal("models/yellow_car.glb"));
        redAsset = loader.load(Gdx.files.internal("models/red_car.glb"));

        track = new Scene(trackAsset.scene);
        player = new Scene(playerAsset.scene);
        renderer.addScene(track);
        renderer.addScene(player);
        player.modelInstance.transform.setToTranslation(playerPosition);

        for (int i = 0; i < bots.length; i++) {
            SceneAsset asset = i < 2 ? yellowAsset : redAsset;
            bots[i] = new Scene(asset.scene);
            renderer.addScene(bots[i]);
        }
        updateBots(0f);
    }

    private void updatePlayer(float delta) {
        boolean accelerating = Gdx.input.isKeyPressed(Input.Keys.UP);
        boolean braking = Gdx.input.isKeyPressed(Input.Keys.DOWN);
        boolean left = Gdx.input.isKeyPressed(Input.Keys.LEFT);
        boolean right = Gdx.input.isKeyPressed(Input.Keys.RIGHT);

        if (accelerating) speed += ACCELERATION * delta;
        else speed -= 3f * delta;
        if (braking) speed -= BRAKE_POWER * delta;
        speed = MathUtils.clamp(speed, 0f, MAX_SPEED);

        steering = left ? 1f : (right ? -1f : 0f);
        playerRotation += steering * delta * 1.8f * (speed / MAX_SPEED);

        playerPosition.x += MathUtils.sin(playerRotation) * speed * delta;
        playerPosition.z += MathUtils.cos(playerRotation) * speed * delta;

        player.modelInstance.transform.setToTranslation(playerPosition);
        player.modelInstance.transform.rotate(
                Vector3.Y, playerRotation * MathUtils.radiansToDegrees);
    }

    private void updateBots(float delta) {
        botTime += delta;
        for (int i = 0; i < bots.length; i++) {
            float angle = botTime * 0.18f + i * MathUtils.PI / 2f;
            float x = MathUtils.cos(angle) * BOT_RADIUS_X;
            float z = MathUtils.sin(angle) * BOT_RADIUS_Z;

            bots[i].modelInstance.transform.idt();
            bots[i].modelInstance.transform.setToTranslation(x, 0f, z);
            bots[i].modelInstance.transform.rotate(
                    Vector3.Y, -angle * MathUtils.radiansToDegrees);
        }
    }

    private void updateCamera(float delta) {
        float behind = 11f;
        float height = 5f;
        float radians = playerRotation;

        cameraTarget.set(playerPosition.x, playerPosition.y + 1f, playerPosition.z);
        cameraPosition.set(
                playerPosition.x - MathUtils.sin(radians) * behind,
                playerPosition.y + height,
                playerPosition.z - MathUtils.cos(radians) * behind
        );

        camera.position.lerp(cameraPosition, Math.min(1f, delta * 5f));
        camera.lookAt(cameraTarget);
        camera.up.set(Vector3.Y);
        camera.update();
    }

    private void updateDynamicShadows() {
        shadowLight.setCenter(playerPosition.x, playerPosition.y, playerPosition.z);
    }

    @Override
    public void render() {
        float delta = Math.min(Gdx.graphics.getDeltaTime(), 1f / 30f);
        updatePlayer(delta);
        updateBots(delta);
        updateCamera(delta);
        updateDynamicShadows();

        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0.35f, 0.55f, 0.75f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        renderer.update(delta);
        renderer.render();
    }

    @Override
    public void resize(int width, int height) {
        if (renderer != null) renderer.updateViewport(width, height);
    }

    @Override
    public void dispose() {
        if (renderer != null) renderer.dispose();
        if (shadowLight != null) shadowLight.dispose();
        if (trackAsset != null) trackAsset.dispose();
        if (playerAsset != null) playerAsset.dispose();
        if (yellowAsset != null) yellowAsset.dispose();
        if (redAsset != null) redAsset.dispose();
    }
}
