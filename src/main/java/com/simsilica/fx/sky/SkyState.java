/*
 * $Id: SkyState.java 174 2014-07-01 08:22:41Z pspeed42 $
 * 
 * Copyright (c) 2014, Simsilica, LLC
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions 
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright 
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright 
 *    notice, this list of conditions and the following disclaimer in 
 *    the documentation and/or other materials provided with the 
 *    distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its 
 *    contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT 
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS 
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE 
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR 
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, 
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED 
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.simsilica.fx.sky;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.bounding.BoundingSphere;
import com.jme3.export.*;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.shape.Sphere;
import com.jme3.shader.VarType;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;
import com.simsilica.fx.LightingState;
import com.simsilica.fx.geom.TruncatedDome;
import com.simsilica.lemur.core.VersionedReference;
import com.simsilica.lemur.event.BaseAppState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * The type Sky state.
 *
 * @author Paul Speed
 */
public class SkyState extends BaseAppState implements Savable, Cloneable, JmeCloneable {

    public static final int EARTH_RADIUS = 6378100;

    public static final ColorRGBA SUN_COLOR = new ColorRGBA(1, 1, 0.9f, 1);
    public static final ColorRGBA SKY_COLOR = new ColorRGBA(0.5f, 0.5f, 1f, 1);
    public static final ColorRGBA GROUND_COLOR = new ColorRGBA(0.25f, 0.25f, 0.3f, 1);

    /**
     * The atmospheric parameters.
     */
    @NotNull
    protected AtmosphericParameters atmosphericParms;

    /**
     * The reference to light direction.
     */
    protected VersionedReference<Vector3f> lightDir;

    protected Vector3f temp1;

    /**
     * The root node.
     */
    protected Node rootNode;

    /**
     * The sky geometry.
     */
    protected Geometry skyGeometry;

    /**
     * The sun geometry.
     */
    protected Geometry sunGeometry;

    /**
     * The ground geometry.
     */
    protected Geometry groundGeometry;

    /**
     * The lighting color.
     */
    protected ColorRGBA lightingColor;

    /**
     * The ground color.
     */
    protected ColorRGBA groundColor;
    /**
     * The sun color.
     */
    protected ColorRGBA sunColor;

    /**
     * The sky color.
     */
    protected ColorRGBA flatColor;

    /**
     * The atmospheric material.
     */
    protected Material atmosphericMaterial;

    /**
     * The ground material.
     */
    protected Material groundMaterial;

    /**
     * The sun material.
     */
    protected Material sunMaterial;

    /**
     * The flat material.
     */
    protected Material flatMaterial;

    /**
     * The dome inner radius.
     */
    protected float domeInnerRadius;

    /**
     * The dome outer radius.
     */
    protected float domeOuterRadius;

    protected boolean showGround;
    protected boolean flatShaded;

    /**
     * Instantiates a new Sky state.
     */
    public SkyState() {
        this(null, false);
        this.temp1 = new Vector3f();
    }

    /**
     * Instantiates a new Sky state.
     *
     * @param groundColor the ground color
     */
    public SkyState(@Nullable final ColorRGBA groundColor) {
        this(groundColor, groundColor != null);
    }

    /**
     * Instantiates a new Sky state.
     *
     * @param groundColor    the ground color
     * @param showGroundDisc the show ground disc
     */
    public SkyState(@Nullable final ColorRGBA groundColor, boolean showGroundDisc) {
        this.lightingColor = new ColorRGBA(1, 1, 1, 1);
        this.groundColor = new ColorRGBA(GROUND_COLOR);
        this.sunColor = new ColorRGBA(SUN_COLOR);
        this.flatColor = new ColorRGBA(SKY_COLOR);
        this.showGround = showGroundDisc;

        if (groundColor != null) {
            this.groundColor.set(groundColor);
        }

        this.domeInnerRadius = 2000;
        this.domeOuterRadius = 2000 * 1.025f;
        this.atmosphericParms = new AtmosphericParameters();
        this.atmosphericParms.setSkyDomeRadius(domeOuterRadius);
        this.atmosphericParms.setPlanetRadius(SkyState.EARTH_RADIUS * 0.01f);
        this.showGround = true;
        this.flatShaded = true;

        final Sphere sunSphere = new Sphere(6, 12, 50);
        final TruncatedDome skyDome = new TruncatedDome(domeInnerRadius, domeOuterRadius, 100, 50, true);
        final TruncatedDome ground = new TruncatedDome(domeInnerRadius, domeOuterRadius, 100, 50, true);

        this.sunGeometry = new Geometry("Sun", sunSphere);
        this.skyGeometry = new Geometry("Sky", skyDome);
        this.skyGeometry.setModelBound(new BoundingSphere(Float.POSITIVE_INFINITY, Vector3f.ZERO));
        this.skyGeometry.setQueueBucket(Bucket.Sky);
        this.skyGeometry.setCullHint(CullHint.Never);
        this.groundGeometry = new Geometry("ground", ground);
        this.groundGeometry.rotate(FastMath.PI, 0, 0);
        this.groundGeometry.setQueueBucket(Bucket.Sky);
        this.groundGeometry.setCullHint(CullHint.Never);
    }

    /**
     * Gets atmospheric parameters.
     *
     * @return the atmospheric parameters.
     */
    public AtmosphericParameters getAtmosphericParameters() {
        return atmosphericParms;
    }

    /**
     * Sets sky parent.
     *
     * @param node the parent node.
     */
    public void setSkyParent(final Node node) {
        this.rootNode = node;
    }

    /**
     * Gets sky parent.
     *
     * @return the parent node.
     */
    public Node getSkyParent() {
        return rootNode;
    }

    /**
     * Sets flat shaded.
     *
     * @param flatShaded true if need to use flat shader.
     */
    public void setFlatShaded(final boolean flatShaded) {
        if (isFlatShaded() == flatShaded) return;
        this.flatShaded = flatShaded;
        resetMaterials();
    }

    /**
     * Is flat shaded boolean.
     *
     * @return true if need to use flat shader.
     */
    public boolean isFlatShaded() {
        return flatShaded;
    }

    /**
     * Sets show ground geometry.
     *
     * @param showGround true if need to show ground geometry.
     */
    public void setShowGroundGeometry(final boolean showGround) {
        if (isShowGroundGeometry() == showGround) return;
        this.showGround = showGround;
        resetGround();
    }

    /**
     * Is show ground geometry boolean.
     *
     * @return true if need to show ground geometry.
     */
    public boolean isShowGroundGeometry() {
        return showGround;
    }

    /**
     * Gets ground color.
     *
     * @return the ground color.
     */
    public ColorRGBA getGroundColor() {
        return groundColor;
    }

    /**
     * Sets ground color.
     *
     * @param groundColor the ground color.
     */
    public void setGroundColor(final ColorRGBA groundColor) {
        this.groundColor.set(groundColor);
        if (groundMaterial == null) return;
        groundMaterial.setParam("GroundColor", VarType.Vector4, groundColor);
    }

    /**
     * Gets sun color.
     *
     * @return the sun color.
     */
    public ColorRGBA getSunColor() {
        return sunColor;
    }

    /**
     * Sets sun color.
     *
     * @param sunColor the sun color.
     */
    public void setSunColor(final ColorRGBA sunColor) {
        this.sunColor.set(sunColor);
        if (sunMaterial == null) return;
        sunMaterial.setParam("Color", VarType.Vector4, sunColor);
    }

    /**
     * Gets flat color.
     *
     * @return the flat color.
     */
    public ColorRGBA getFlatColor() {
        return flatColor;
    }

    /**
     * Sets flat color.
     *
     * @param flatColor the flat color.
     */
    public void setFlatColor(final ColorRGBA flatColor) {
        this.flatColor.set(flatColor);
        if (flatMaterial == null) return;
        flatMaterial.setParam("Color", VarType.Vector4, flatColor);
    }

    /**
     * Reset materials.
     */
    protected void resetMaterials() {
        if (!isEnabled()) return;
        if (groundGeometry == null || flatMaterial == null || atmosphericMaterial == null) return;

        if (isFlatShaded()) {
            skyGeometry.setMaterial(flatMaterial);
            sunGeometry.setCullHint(CullHint.Inherit);
            groundMaterial.setBoolean("UseScattering", false);
        } else {
            skyGeometry.setMaterial(atmosphericMaterial);
            sunGeometry.setCullHint(CullHint.Never);
            groundMaterial.setBoolean("UseScattering", true);
        }
    }

    /**
     * Reset ground.
     */
    protected void resetGround() {
        if (groundGeometry == null || rootNode == null) return;
        if (!isEnabled()) return;
        if (isShowGroundGeometry()) {
            rootNode.attachChild(groundGeometry);
        } else {
            groundGeometry.removeFromParent();
        }
    }

    /**
     * Gets ground material.
     *
     * @return the ground material
     */
    @Nullable
    public Material getGroundMaterial() {
        return groundMaterial;
    }

    @Override
    protected void initialize(final Application app) {

        final AssetManager assetManager = app.getAssetManager();

        if (rootNode == null) {
            rootNode = ((SimpleApplication) app).getRootNode();
        }

        final LightingState lightingState = getState(LightingState.class);
        final VersionedReference<Vector3f> lightDirRef = lightingState.getLightDirRef();

        init(lightDirRef, assetManager);
    }

    /**
     * External initialize this app state.
     *
     * @param lightDir     the light direction reference.
     * @param assetManager the asset manager.
     */
    public void init(@NotNull final VersionedReference<Vector3f> lightDir, @NotNull final AssetManager assetManager) {
        this.lightDir = lightDir;

        final Vector3f lightDirection = lightDir.get();

        if (sunMaterial == null) {
            sunMaterial = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            sunGeometry.setMaterial(sunMaterial);
        }

        if (flatMaterial == null) {
            flatMaterial = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        }

        if (atmosphericMaterial == null) {
            atmosphericMaterial = atmosphericParms.getSkyMaterial(assetManager);
        }

        atmosphericParms.setLightDirection(lightDirection);

        if (groundMaterial == null) {
            groundMaterial = new Material(assetManager, "MatDefs/GroundAtmospherics.j3md");
            groundMaterial.setColor("GroundColor", groundColor);
            groundMaterial.setBoolean("FollowCamera", true);
            groundMaterial.setBoolean("UseScattering", true);
            groundMaterial.setFloat("GroundScale", 10);
            groundGeometry.setMaterial(groundMaterial);
            atmosphericParms.applyGroundParameters(groundMaterial, true);
        }

        sunGeometry.move(lightDirection.mult(-900, temp1));

        atmosphericParms.calculateGroundColor(ColorRGBA.White, Vector3f.UNIT_X, 1f, 0, lightingColor);

        resetMaterials();
        resetGround();
    }

    @Override
    protected void cleanup(final Application app) {
        lightDir = null;
        rootNode = null;
    }

    @Override
    public void update(float tpf) {

        if (lightDir.update()) {
            final Vector3f direction = lightDir.get();
            sunGeometry.setLocalTranslation(direction.mult(-900, temp1));
            atmosphericParms.setLightDirection(direction);
            atmosphericParms.calculateGroundColor(ColorRGBA.White, Vector3f.UNIT_X, 1f, 0, lightingColor);
        }

        if (isFlatShaded()) {
            skyGeometry.setLocalTranslation(getApplication().getCamera().getLocation());
        }
    }

    @Override
    protected void enable() {
        if (rootNode == null) return;
        rootNode.attachChild(skyGeometry);
        if (isShowGroundGeometry()) {
            rootNode.attachChild(groundGeometry);
        }
        resetMaterials();
        resetGround();
    }

    @Override
    protected void disable() {
        if (rootNode == null) return;
        skyGeometry.removeFromParent();
        groundGeometry.removeFromParent();
    }

    @Override
    public Object jmeClone() {
        try {
            return super.clone();
        } catch (final CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void cloneFields(final Cloner cloner, final Object original) {
        temp1 = cloner.clone(temp1);
        skyGeometry = cloner.clone(skyGeometry);
        sunGeometry = cloner.clone(sunGeometry);
        groundGeometry = cloner.clone(groundGeometry);
        flatMaterial = cloner.clone(flatMaterial);
        atmosphericMaterial = cloner.clone(atmosphericMaterial);
        groundMaterial = cloner.clone(groundMaterial);
        sunMaterial = cloner.clone(sunMaterial);
        atmosphericParms = cloner.clone(atmosphericParms);
        lightingColor = cloner.clone(lightingColor);
        sunColor = cloner.clone(sunColor);
        flatColor = cloner.clone(flatColor);
        groundColor = cloner.clone(groundColor);
    }

    @Override
    public void write(final JmeExporter exporter) throws IOException {
        final OutputCapsule capsule = exporter.getCapsule(this);
        capsule.write(skyGeometry, "skyGeometry", null);
        capsule.write(sunGeometry, "sunGeometry", null);
        capsule.write(groundGeometry, "groundGeometry", null);
        capsule.write(flatMaterial, "flatMaterial", null);
        capsule.write(atmosphericMaterial, "atmosphericMaterial", null);
        capsule.write(groundMaterial, "groundMaterial", null);
        capsule.write(sunMaterial, "sunMaterial", null);
        capsule.write(atmosphericParms, "atmosphericParms", null);
        capsule.write(flatShaded, "flatShaded", false);
        capsule.write(showGround, "showGround", false);
        capsule.write(domeInnerRadius, "domeInnerRadius", 0);
        capsule.write(domeOuterRadius, "domeOuterRadius", 0);
        capsule.write(lightingColor, "lightingColor", null);
        capsule.write(flatColor, "flatColor", null);
        capsule.write(sunColor, "sunColor", null);
        capsule.write(groundColor, "groundColor", null);
    }

    @Override
    public void read(final JmeImporter importer) throws IOException {
        final InputCapsule capsule = importer.getCapsule(this);
        skyGeometry = (Geometry) capsule.readSavable("skyGeometry", null);
        sunGeometry = (Geometry) capsule.readSavable("sunGeometry", null);
        groundGeometry = (Geometry) capsule.readSavable("groundGeometry", null);
        lightingColor = (ColorRGBA) capsule.readSavable("lightingColor", null);
        flatMaterial = (Material) capsule.readSavable("flatMaterial", null);
        atmosphericMaterial = (Material) capsule.readSavable("atmosphericMaterial", null);
        groundMaterial = (Material) capsule.readSavable("groundMaterial", null);
        sunMaterial = (Material) capsule.readSavable("sunMaterial", null);
        atmosphericParms = (AtmosphericParameters) capsule.readSavable("atmosphericParms", null);
        flatShaded = capsule.readBoolean("flatShaded", false);
        showGround = capsule.readBoolean("showGround", false);
        domeInnerRadius = capsule.readFloat("domeInnerRadius", 0);
        domeOuterRadius = capsule.readFloat("domeOuterRadius", 0);
        flatColor = (ColorRGBA) capsule.readSavable("flatColor", null);
        sunColor = (ColorRGBA) capsule.readSavable("sunColor", null);
        groundColor = (ColorRGBA) capsule.readSavable("groundColor", null);

        if (groundMaterial != null) {
            groundMaterial.setColor("GroundColor", groundColor);
        }

        if (flatMaterial != null) {
            flatMaterial.setParam("Color", VarType.Vector4, flatColor);
        }

        if (sunMaterial != null) {
            sunMaterial.setParam("Color", VarType.Vector4, sunColor);
        }
    }
}
