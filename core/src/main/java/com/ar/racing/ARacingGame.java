package com.ar.racing;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.ModelBatch;
import com.badlogic.gdx.graphics.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.math.MathUtils;

public class ARacingGame extends ApplicationAdapter {

    private ModelBatch batch;
    private ModelBuilder builder;
    private Environment environment;
    private PerspectiveCamera camera;

    private Model roadModel;
    private Model groundModel;
    private final Model[] carModels = new Model[5];
    private ModelInstance road;
    private ModelInstance ground;
    private final ModelInstance[] cars = new ModelInstance[5];

    private final float[] positions = new float[5];
    private final float[] progress = new float[5];
    private final float[] botSpeed = {0, 18, 16, 20, 17};

    private float playerSpeed = 0;
    private int playerLane = 1;

    private boolean leftPressed;
    private boolean rightPressed;
    private boolean finished = false;

    private static final float ROAD_WIDTH = 12f;
    private static final float FINISH_DISTANCE = 1200f;

    private BitmapFont font;
    private SpriteBatch ui;

    @Override
    public void create() {
        batch = new ModelBatch();
        builder = new ModelBuilder();
        ui = new SpriteBatch();
        font = new BitmapFont();

        camera = new PerspectiveCamera(
            67,
            Gdx.graphics.getWidth(),
            Gdx.graphics.getHeight()
        );
        camera.near = 0.1f;
        camera.far = 2500f;

        environment = new Environment();
        environment.set(new ColorAttribute(
            ColorAttribute.AmbientLight,
            0.85f, 0.85f, 0.85f, 1f
        ));
        environment.add(new DirectionalLight().set(
            1f, 1f, 1f, -0.5f, -1f, -0.5f
        ));

        roadModel = box(ROAD_WIDTH, 0.2f, 1600f, Color.DARK_GRAY);
        road = new ModelInstance(roadModel);
        road.transform.setToTranslation(0f, 0f, -790f);

        groundModel = box(
            300f, 0.1f, 1800f,
            new Color(0.12f, 0.38f, 0.12f, 1f)
        );
        ground = new ModelInstance(groundModel);
        ground.transform.setToTranslation(0f, -0.2f, -790f);

        Color[] colors = {
            Color.RED, Color.BLUE, Color.YELLOW, Color.GREEN, Color.ORANGE
        };

        for (int i = 0; i < 5; i++) {
            carModels[i] = box(1.7f, 0.8f, 3.2f, colors[i]);
            cars[i] = new ModelInstance(carModels[i]);
        }

        positions[0] = 0f;
        positions[1] = -3f;
        positions[2] = 3f;
        positions[3] = -3f;
        positions[4] = 3f;

        progress[0] = 0f;
        progress[1] = -12f;
        progress[2] = -25f;
        progress[3] = -40f;
        progress[4] = -55f;

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int x, int y, int pointer, int button) {
                float width = Gdx.graphics.getWidth();
                if (x < width / 2f) {
                    leftPressed = true;
                } else {
                    rightPressed = true;
                }
                return true;
            }

            @Override
            public boolean touchUp(int x, int y, int pointer, int button) {
                leftPressed = false;
                rightPressed = false;
                return true;
            }
        });
    }

    private Model box(float width, float height, float depth, Color color) {
        return builder.createBox(
            width, height, depth,
            new Material(ColorAttribute.createDiffuse(color)),
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal
        );
    }

    @Override
    public void resize(int width, int height) {
        if (height <= 0) return;
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
        ui.getProjectionMatrix().setToOrtho2D(0f, 0f, width, height);
    }

    @Override
    public void render() {
        float delta = Math.min(Gdx.graphics.getDeltaTime(), 0.033f);
        updateGame(delta);

        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0.38f, 0.68f, 0.95f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        camera.position.set(positions[0], 7f, progress[0] + 13f);
        camera.lookAt(positions[0], 0f, progress[0] - 15f);
        camera.update();

        batch.begin(camera);
        batch.render(ground, environment);
        batch.render(road, environment);

        for (int i = 0; i < 5; i++) {
            cars[i].transform.setToTranslation(positions[i], 0.65f, -progress[i]);
            batch.render(cars[i], environment);
        }
        batch.end();

        drawHUD();
    }

    private void updateGame(float delta) {
        if (finished) return;

        // One lane change per screen tap.
        if (leftPressed) {
            playerLane = Math.max(0, playerLane - 1);
            leftPressed = false;
        }
        if (rightPressed) {
            playerLane = Math.min(2, playerLane + 1);
            rightPressed = false;
        }

        float targetX = (playerLane - 1) * 3.5f;
        positions[0] = MathUtils.lerp(
            positions[0], targetX, Math.min(1f, delta * 7f)
        );

        playerSpeed = MathUtils.lerp(playerSpeed, 24f, delta * 0.5f);
        progress[0] += playerSpeed * delta;

        for (int i = 1; i < 5; i++) {
            float variation = MathUtils.sin(progress[i] * 0.025f + i) * 1.5f;
            positions[i] = MathUtils.clamp(
                positions[i] + variation * delta, -4.5f, 4.5f
            );

            botSpeed[i] = 17f + i * 1.1f
                + MathUtils.sin(progress[i] * 0.03f) * 2f;
            progress[i] += botSpeed[i] * delta;
        }

        for (int i = 0; i < 5; i++) {
            if (progress[i] >= FINISH_DISTANCE) {
                finished = true;
                break;
            }
        }
    }

    private int getRank() {
        int rank = 1;
        for (int i = 1; i < 5; i++) {
            if (progress[i] > progress[0]) rank++;
        }
        return rank;
    }

    private void drawHUD() {
        ui.begin();

        font.getData().setScale(1.5f);
        font.setColor(Color.WHITE);

        font.draw(ui, "AR RACING", 20f, Gdx.graphics.getHeight() - 25f);
        font.draw(
            ui,
            "SPEED: " + (int)(playerSpeed * 6f) + " KM/H",
            20f, Gdx.graphics.getHeight() - 65f
        );
        font.draw(ui, "RANK: " + getRank() + "/5", 20f, Gdx.graphics.getHeight() - 105f);

        if (finished) {
            font.draw(
                ui,
                "RACE FINISHED!",
                Gdx.graphics.getWidth() / 2f - 100f,
                Gdx.graphics.getHeight() / 2f
            );
        }

        font.draw(ui, "LEFT", 30f, 60f);
        font.draw(ui, "RIGHT", Gdx.graphics.getWidth() - 110f, 60f);

        ui.end();
    }

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (ui != null) ui.dispose();
        if (font != null) font.dispose();

        if (roadModel != null) roadModel.dispose();
        if (groundModel != null) groundModel.dispose();

        for (Model model : carModels) {
            if (model != null) model.dispose();
        }
    }
}
