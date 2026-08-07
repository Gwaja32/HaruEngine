package org.libertywild.libertyengine.mixin.entity;

import org.libertywild.libertyengine.util.EntityFilter;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobEntityMixin {
    @Shadow @Final protected GoalSelector goalSelector;
    @Shadow @Final protected GoalSelector targetSelector;

    /**
     * @author LeeGwangSu
     * @reason AI 판단 주기를 조절하여 CPU 부하 경감 (병렬화보다 안전하고 빠름)
     */
    @Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
    private void optimizedAiTick(CallbackInfo ci) {
        Mob self = (Mob) (Object) this;

        boolean isChunSik = self.hasCustomName() && "춘식이".equals(self.getCustomName().getString());
        if (isChunSik) return;

        // [수정] 병렬 연산(ParallelTarget) 대상 중 가벼운 몹들(박쥐 등)은 AI를 매 틱 돌릴 필요가 없습니다.
        if (EntityFilter.isParallelTarget(self)) {
            // 3틱에 한 번만 AI 로직을 실행하도록 제한 (성능 약 66% 향상)
            if (self.tickCount % 3 != 0) {
                ci.cancel();
            }
        }

        // 주의: ParallelEngine.computeParallel 로직은 여기서 제거합니다.
        // 바닐라의 tickNewAi가 자동으로 실행되도록 두는 것이 데이터 안전성 면에서 가장 좋습니다.
    }
}