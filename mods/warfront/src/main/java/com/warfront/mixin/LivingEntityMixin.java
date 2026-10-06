package com.warfront.mixin;

import com.warfront.flight.WingFlight;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    /** Keeps a winged player fall-flying without an elytra in the chest slot. */
    @Inject(method = "updateFallFlying", at = @At("HEAD"), cancellable = true)
    private void warfront$wingFlight(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Player player) || !player.isFallFlying()) return;
        if (!WingFlight.hasWings(player) || WingFlight.wearsWorkingElytra(player)) return;
        if (!player.level().isClientSide) {
            player.setSharedFlag(7, WingFlight.canStayAloft(player));
        }
        ci.cancel();
    }
}
