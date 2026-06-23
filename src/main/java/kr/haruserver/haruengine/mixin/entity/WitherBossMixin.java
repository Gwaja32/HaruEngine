package kr.haruserver.haruengine.mixin.entity;

import kr.haruserver.haruengine.util.ConfigManager;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WitherBoss.class)
public abstract class WitherBossMixin {
    @ModifyArg(
            method = "aiStep",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/boss/wither/WitherBoss;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"),
            index = 0
    )
    private Vec3 limitWitherMovement(Vec3 movement) {
        WitherBoss wither = (WitherBoss) (Object) this;

        // 현재 높이가 제한을 넘었고, 상승 중(movement.y > 0)이라면
        if (wither.getY() > ConfigManager.data.maxWitherY && movement.y > 0) {
            // Y 속도만 0으로 고정하고 X, Z는 그대로 유지
            return new Vec3(movement.x, 0.0, movement.z);
        }
        return movement;
    }
}