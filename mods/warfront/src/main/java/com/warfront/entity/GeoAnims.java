package com.warfront.entity;

import com.warfront.army.UnitBody;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * Animations for units drawn with GeckoLib models (see client/geo). Each such body has idle, walk and attack clips
 * named animation.&lt;variant&gt;.&lt;clip&gt;; the attack is triggered from the server when a hit lands.
 */
public final class GeoAnims {
    private GeoAnims() {}

    public static final String CONTROLLER = "body";

    /** Bodies drawn with a GeckoLib model. */
    public static boolean usesGeo(UnitBody body) {
        return body == UnitBody.BONE_STALKER;
    }

    public static void register(SoldierEntity e, AnimatableManager.ControllerRegistrar controllers) {
        String id = e.getBody().variant(e.getRole());
        RawAnimation idle = RawAnimation.begin().thenLoop("animation." + id + ".idle");
        RawAnimation walk = RawAnimation.begin().thenLoop("animation." + id + ".walk");
        controllers.add(new AnimationController<>(e, CONTROLLER, 4, state -> {
            if (!usesGeo(e.getBody())) return PlayState.STOP;
            return state.setAndContinue(state.isMoving() ? walk : idle);
        }).triggerableAnim("attack", RawAnimation.begin().thenPlay("animation." + id + ".attack")));
    }

    /** A hit landed: play the attack clip on every client watching. */
    public static void attacked(SoldierEntity e) {
        if (!e.level().isClientSide && usesGeo(e.getBody())) e.triggerAnim(CONTROLLER, "attack");
    }
}
