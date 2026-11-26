package com.oculifelis.lararium.mixin;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Explicit development checks never open an audio device. Ordinary play is unchanged. */
@Mixin(SoundEngine.class)
public final class TestAudioGuard {
    @Inject(method="loadLibrary",at=@At("HEAD"),cancellable=true)
    private void lararium$testSilence(CallbackInfo ci){if(Boolean.getBoolean("lararium.devtest"))ci.cancel();}
    @Inject(method="shouldChangeDevice",at=@At("HEAD"),cancellable=true)
    private void lararium$testNoDevice(CallbackInfoReturnable<Boolean> cir){if(Boolean.getBoolean("lararium.devtest"))cir.setReturnValue(false);}
}
