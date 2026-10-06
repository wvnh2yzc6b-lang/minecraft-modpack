package com.warfront.client;

import net.minecraft.util.Mth;

/**
 * Procedural wing motion for winged players, built to read as a real animal rather than a metronome:
 * <ul>
 *   <li>The beat runs on an accumulated phase, so speeding up or slowing down never jumps the wings.</li>
 *   <li>The stroke is asymmetric: a quick power downstroke and a slower recovery upstroke, during
 *       which the outer wing half-folds to spill air, like a bat or a large bird.</li>
 *   <li>The outer wing lags the inner (follow-through), and the stroke traces a figure eight:
 *       forward and leading edge down on the downstroke, back on the upstroke.</li>
 *   <li>Effort follows the flight: hard beats on takeoff and climbs, a swept-back glide in dives.</li>
 *   <li>Opening unfolds shoulder then elbow; closing folds elbow then shoulder.</li>
 *   <li>Every blend is exponential easing scaled by real elapsed time, so it is smooth at any frame rate.</li>
 * </ul>
 * The preview renderer (tools) mirrors this file line for line.
 */
public final class WingAnimator {
    // Tucked: wrist folded up over the shoulder, membrane hanging flat down the back.
    static final float TUCK_X = 0.77F, TUCK_Y = 1.26F, TUCK_Z = 0.7F, TUCK_OUTER_Z = -2.95F;
    // Spread for flight: wide and slightly swept back.
    static final float OPEN_X = 0.1F, OPEN_Y = 0.2F, OPEN_Z = 0.35F;

    /** Per-player animation state. */
    public static final class State {
        float spread, amp, freq, phase, glide, cloak;
        float lastAge = Float.NaN;
    }

    /** The pose to apply, for the right wing; the left mirrors it. */
    public static final class Pose {
        public float x, y, z, outerZ, cloakX;
    }

    /** What the animator needs to know about the player this frame. */
    public record Input(boolean flying, boolean airborne, float limbSwingAmount, boolean sprinting,
                        float climb, float speed, int flyTicks, float ageInTicks) {}

    private WingAnimator() {}

    /** Exponential approach, frame-rate independent: rate is the fraction closed per tick. */
    static float approach(float current, float target, float rate, float dt) {
        return current + (target - current) * (1F - (float) Math.pow(1F - rate, dt));
    }

    public static void update(State s, Input in, Pose out) {
        float dt = Float.isNaN(s.lastAge) ? 0F : Mth.clamp(in.ageInTicks() - s.lastAge, 0F, 4F);
        s.lastAge = in.ageInTicks();

        // Targets for this moment of flight.
        float spreadTarget, ampTarget, freqTarget, glideTarget;
        if (in.flying()) {
            // Effort rises when climbing or slow, falls to a glide in a fast dive; takeoff beats hard.
            float effort = Mth.clamp(0.55F + in.climb() * 3F - Math.max(0F, in.speed() - 1.2F) * 0.6F, 0.1F, 1F);
            effort = Math.max(effort, 1F - in.flyTicks() / 25F);
            spreadTarget = 1F;
            ampTarget = effort;
            freqTarget = 0.24F + 0.2F * effort;
            glideTarget = 1F - effort;
        } else if (in.airborne()) {
            spreadTarget = 0.45F;   // half-open, quick balancing flaps
            ampTarget = 0.45F;
            freqTarget = 0.62F;
            glideTarget = 0F;
        } else {
            spreadTarget = 0F;
            ampTarget = 0F;
            freqTarget = 0.3F;
            glideTarget = 0F;
        }
        if (dt == 0F && s.freq == 0F) {   // first frame: start settled rather than snapping in
            s.spread = spreadTarget;
            s.amp = ampTarget;
            s.freq = freqTarget;
            s.glide = glideTarget;
        }
        s.spread = approach(s.spread, spreadTarget, spreadTarget > s.spread ? 0.22F : 0.14F, dt);
        s.amp = approach(s.amp, ampTarget, 0.1F, dt);
        s.freq = approach(s.freq, freqTarget, 0.08F, dt);
        s.glide = approach(s.glide, glideTarget, 0.06F, dt);
        s.phase = (s.phase + s.freq * dt) % (float) (Math.PI * 2);

        // Asymmetric stroke: the phase warp makes the downstroke ~38% of the cycle.
        float psi = s.phase + 0.4F * (1F - Mth.cos(s.phase));
        float lift = Mth.cos(psi);        // +1 wings up, -1 wings down
        float stroke = Mth.sin(psi);      // > 0 on the downstroke, < 0 on the upstroke
        float sp = s.spread;
        float a = s.amp * sp;

        // Shoulder opens first, elbow follows (and folds first when closing).
        float elbow = sp * sp * (3F - 2F * sp);
        elbow = elbow * elbow;

        out.x = Mth.lerp(sp, TUCK_X, OPEN_X) + a * 0.1F * stroke;
        out.y = Mth.lerp(sp, TUCK_Y, OPEN_Y) + a * 0.16F * stroke + s.glide * sp * 0.3F;
        out.z = Mth.lerp(sp, TUCK_Z, OPEN_Z) + a * 0.42F * lift - s.glide * sp * 0.08F;
        // Tip lags the arm on the downstroke, half-folds on the recovery stroke.
        float tip = stroke > 0F ? 0.28F * stroke : 0.85F * stroke;
        out.outerZ = Mth.lerp(elbow, TUCK_OUTER_Z, 0F) + a * tip;

        // Folded, the wings breathe and jostle with each step.
        float rest = 1F - sp;
        out.z += rest * (Mth.sin(in.ageInTicks() * 0.08F) * 0.03F + in.limbSwingAmount() * 0.06F
                * Mth.sin(in.ageInTicks() * 0.6F));

        // Cloak: streams back in flight, riding the wingbeat a beat late, with a fast flutter in the wind;
        // on foot it swings with the stride.
        float cloakTarget = in.flying()
                ? 0.18F + a * 0.07F * Mth.sin(psi - 1.1F) + Math.min(1F, in.speed()) * 0.03F * Mth.sin(in.ageInTicks() * 1.7F)
                : 0.1F + in.limbSwingAmount() * 0.7F + (in.sprinting() ? 0.3F : 0F) + Mth.sin(in.ageInTicks() * 0.05F) * 0.03F;
        s.cloak = dt == 0F && s.cloak == 0F ? cloakTarget : approach(s.cloak, cloakTarget, 0.35F, dt);
        out.cloakX = s.cloak;
    }
}
