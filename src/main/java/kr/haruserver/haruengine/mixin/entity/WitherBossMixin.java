package kr.haruserver.haruengine.mixin.entity;

import kr.haruserver.haruengine.util.ConfigManager;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(WitherBoss.class)
public abstract class WitherBossMixin {

    @Redirect(
            method = "aiStep",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/boss/wither/WitherBoss;getY()D")
    )
    private double redirectGetY(WitherBoss instance) {
        double currentY = instance.getY();
        // 높이 제한 로직 적용
        if (currentY > ConfigManager.data.maxWitherY) {
            // Y좌표를 강제로 최대치로 고정하여, 위더가 더 위로 올라가려는 로직을 무력화
            return ConfigManager.data.maxWitherY;
        }
        return currentY;
    }
}