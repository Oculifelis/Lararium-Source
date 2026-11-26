package com.oculifelis.lararium.mixin;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Explicit development checks never capture the desktop cursor. Ordinary play is unchanged. */
@Mixin(MouseHandler.class)
public final class TestMouseGuard {
    @Inject(method="grabMouse",at=@At("HEAD"),cancellable=true)
    private void lararium$testCursor(CallbackInfo ci){if(Boolean.getBoolean("lararium.devtest"))ci.cancel();}
}
