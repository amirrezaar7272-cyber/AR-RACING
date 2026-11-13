package com.ar.racing;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import net.mgsx.gltf.scene3d.attributes.PBRColorAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRFloatAttribute;
import net.mgsx.gltf.scene3d.lights.DirectionalShadowLight;
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneAsset;
import net.mgsx.gltf.scene3d.scene.SceneManager;
import net.mgsx.gltf.scene3d.shaders.PBRDepthShaderProvider;
import net.mgsx.gltf.scene3d.shaders.PBRShaderConfig;
import net.mgsx.gltf.scene3d.shaders.PBRShaderProvider;
import net.mgsx.gltf.loaders.glb.GLBLoader;

public class ARacingGame extends ApplicationAdapter {
    private SceneManager renderer;
    private PerspectiveCamera camera;
    private DirectionalShadowLight shadowLight;
    private Scene track, player;
    private final Scene[] bots = new Scene[4];
    private SceneAsset trackAsset, playerAsset, yellowAsset, redAsset;
    private Model fallbackTrack, fallbackBlue, fallbackYellow, fallbackRed;
    private final Vector3 playerPosition = new Vector3();
    private final Vector3 cameraTarget = new Vector3(), cameraPosition = new Vector3();
    private float speed, playerRotation, botTime;
    private boolean accelerate, brake, left, right;
    private static final float MAX_SPEED=32f, ACCELERATION=12f, BRAKE_POWER=20f;
    private static final long ATTR=VertexAttributes.Usage.Position|VertexAttributes.Usage.Normal;

    @Override public void create() {
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        PBRShaderConfig config=PBRShaderProvider.createDefaultConfig();
        config.numDirectionalLights=1; config.numPointLights=0; config.numSpotLights=0;
        renderer=new SceneManager(new PBRShaderProvider(config),
                new PBRDepthShaderProvider(PBRShaderProvider.createDefaultDepthConfig()));
        renderer.setAmbientLight(0.45f);
        shadowLight=new DirectionalShadowLight(1024,1024,100f,100f,1f,180f);
        shadowLight.set(Color.WHITE,new Vector3(-1f,-4f,-2f),2.2f);
        renderer.environment.add(shadowLight);
        renderer.environment.set(new PBRFloatAttribute(PBRFloatAttribute.ShadowBias,1f/256f));

        camera=new PerspectiveCamera(67f,Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
        camera.near=0.1f; camera.far=250f;
        camera.position.set(0,8,-12); camera.lookAt(0,0,0); camera.up.set(Vector3.Y); camera.update();
        renderer.setCamera(camera);

        buildFallbackModels();
        loadTrack();
        loadPlayer();
        loadBots();
        updateBots(0);
    }

    private void buildFallbackModels() {
        ModelBuilder mb=new ModelBuilder();
        fallbackTrack=mb.createBox(100f,0.25f,70f,
                new Material(PBRColorAttribute.createBaseColorFactor(new Color(0.12f,0.14f,0.16f,1))),
                ATTR);
        fallbackBlue=makeCar(new Color(0.04f,0.25f,0.95f,1));
        fallbackYellow=makeCar(new Color(1f,0.72f,0.02f,1));
        fallbackRed=makeCar(new Color(0.85f,0.04f,0.03f,1));
    }

    private Model makeCar(Color color) {
        ModelBuilder mb=new ModelBuilder();
        mb.begin();
        Material body=new Material(PBRColorAttribute.createBaseColorFactor(color));
        mb.part("body",GL20.GL_TRIANGLES,ATTR,body).box(2f,0.8f,4f);
        Material glass=new Material(PBRColorAttribute.createBaseColorFactor(new Color(0.05f,0.08f,0.12f,1)));
        mb.part("cabin",GL20.GL_TRIANGLES,ATTR,glass).box(1.45f,0.65f,1.8f);
        return mb.end();
    }

    private void loadTrack() {
        if (Gdx.files.internal("models/track.glb").exists()) {
            try { trackAsset=new GLBLoader().load(Gdx.files.internal("models/track.glb")); track=new Scene(trackAsset.scene); }
            catch (Exception ignored) { track=new Scene(fallbackTrack); }
        } else track=new Scene(fallbackTrack);
        renderer.addScene(track);
    }
    private void loadPlayer() {
        if (Gdx.files.internal("models/blue_car.glb").exists()) {
            try { playerAsset=new GLBLoader().load(Gdx.files.internal("models/blue_car.glb")); player=new Scene(playerAsset.scene); }
            catch (Exception ignored) { player=new Scene(fallbackBlue); }
        } else player=new Scene(fallbackBlue);
        renderer.addScene(player);
    }
    private void loadBots() {
        for(int i=0;i<4;i++) {
            if(i<2 && Gdx.files.internal("models/yellow_car.glb").exists()) {
                try { if(yellowAsset==null) yellowAsset=new GLBLoader().load(Gdx.files.internal("models/yellow_car.glb")); bots[i]=new Scene(yellowAsset.scene); }
                catch(Exception ignored) { bots[i]=new Scene(fallbackYellow); }
            } else if(i>=2 && Gdx.files.internal("models/red_car.glb").exists()) {
                try { if(redAsset==null) redAsset=new GLBLoader().load(Gdx.files.internal("models/red_car.glb")); bots[i]=new Scene(redAsset.scene); }
                catch(Exception ignored) { bots[i]=new Scene(fallbackRed); }
            } else bots[i]=new Scene(i<2?fallbackYellow:fallbackRed);
            renderer.addScene(bots[i]);
        }
    }

    private void readTouchControls() {
        accelerate=brake=left=right=false;
        float w=Gdx.graphics.getWidth(), h=Gdx.graphics.getHeight();
        for(int p=0;p<5;p++) if(Gdx.input.isTouched(p)) {
            float x=Gdx.input.getX(p), y=h-Gdx.input.getY(p);
            if(y<h*0.48f) { if(x<w*0.5f) left=true; else right=true; }
            else { if(x<w*0.5f) brake=true; else accelerate=true; }
        }
    }

    private void updatePlayer(float dt) {
        readTouchControls();
        if(accelerate) speed+=ACCELERATION*dt; else speed-=2.5f*dt;
        if(brake) speed-=BRAKE_POWER*dt;
        speed=MathUtils.clamp(speed,0,MAX_SPEED);
        float steer=left?1f:(right?-1f:0f);
        playerRotation+=steer*dt*1.8f*(speed/MAX_SPEED);
        playerPosition.x+=MathUtils.sin(playerRotation)*speed*dt;
        playerPosition.z+=MathUtils.cos(playerRotation)*speed*dt;
        player.modelInstance.transform.setToTranslation(playerPosition);
        player.modelInstance.transform.rotate(Vector3.Y,playerRotation*MathUtils.radiansToDegrees);
    }
    private void updateBots(float dt) {
        botTime+=dt;
        for(int i=0;i<bots.length;i++) {
            float a=botTime*0.18f+i*MathUtils.PI/2f;
            bots[i].modelInstance.transform.idt().setToTranslation(
                    MathUtils.cos(a)*30f,0,MathUtils.sin(a)*18f);
            bots[i].modelInstance.transform.rotate(Vector3.Y,-a*MathUtils.radiansToDegrees);
        }
    }
    private void updateCamera(float dt) {
        float behind=11f, height=5f;
        cameraTarget.set(playerPosition.x,playerPosition.y+1f,playerPosition.z);
        cameraPosition.set(playerPosition.x-MathUtils.sin(playerRotation)*behind,
                playerPosition.y+height,playerPosition.z-MathUtils.cos(playerRotation)*behind);
        camera.position.lerp(cameraPosition,Math.min(1f,dt*5f));
        camera.lookAt(cameraTarget); camera.up.set(Vector3.Y); camera.update();
    }
    @Override public void render() {
        float dt=Math.min(Gdx.graphics.getDeltaTime(),1f/30f);
        updatePlayer(dt); updateBots(dt); updateCamera(dt);
        shadowLight.setCenter(playerPosition);
        Gdx.gl.glViewport(0,0,Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0.35f,0.55f,0.75f,1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT|GL20.GL_DEPTH_BUFFER_BIT);
        renderer.update(dt); renderer.render();
    }
    @Override public void resize(int w,int h) { if(renderer!=null){camera.viewportWidth=w;camera.viewportHeight=h;camera.update();renderer.updateViewport(w,h);} }
    @Override public void dispose() {
        if(renderer!=null) renderer.dispose();
        if(shadowLight!=null) shadowLight.dispose();
        if(trackAsset!=null) trackAsset.dispose();
        if(playerAsset!=null) playerAsset.dispose();
        if(yellowAsset!=null) yellowAsset.dispose();
        if(redAsset!=null) redAsset.dispose();
        if(fallbackTrack!=null) fallbackTrack.dispose();
        if(fallbackBlue!=null) fallbackBlue.dispose();
        if(fallbackYellow!=null) fallbackYellow.dispose();
        if(fallbackRed!=null) fallbackRed.dispose();
    }
}
