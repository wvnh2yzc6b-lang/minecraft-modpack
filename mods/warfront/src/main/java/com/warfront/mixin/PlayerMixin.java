package com.warfront.mixin;

import com.warfront.flight.WingFlight;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin {
    /** Winged players can start gliding without an elytra. */
    @Inject(method = "tryToStartFallFlying", at = @At("HEAD"), cancellable = true)
    private void warfront$wingTakeoff(CallbackInfoReturnable<Boolean> cir) {
        Player player = (Player) (Object) this;
        if (!WingFlight.wearsWorkingElytra(player) && WingFlight.canTakeOff(player)) {
            player.startFallFlying();
            cir.setReturnValue(true);
        }
    }
}
