package kr.haruserver.haruengine.mixin.entity;

import kr.haruserver.haruengine.util.EntityFilter;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityWaterStateMixin {

    @Shadow public abstract boolean isInWater();

    /**
     * @author LeeGwangSu
     * @reason 가축 9종의 수중 상태 체크 빈도를 낮춰 블록 쿼리 랙(WaterState) 해결
     */
    @Inject(method = "updateFluidInteraction", at = @At("HEAD"), cancellable = true)
    private void haru$optimizedWaterState(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;

        // 1. [비트마스크 최적화] 가축 9종 등 최적화 대상인지 확인
        if (EntityFilter.isNoPhysicsTarget(self)) {
            // 2. [틱 조절] 매 틱 주변 블록을 전수조사하는 대신 5틱에 한 번만 수행
            if (self.tickCount % 5 != 0) {
                // 이전에 계산되어 필드에 저장된 수중 상태 값을 그대로 반환하여 연산을 스킵합니다.
                // isTouchingWater()는 단순히 내부 flag를 리턴하므로 연산 부하가 없습니다.
                cir.setReturnValue(this.isInWater());
            }
            // age % 5 == 0 인 틱에는 믹스인이 개입하지 않아 바닐라의 실제 수중 체크 로직이 실행됩니다.
        }
    }
}