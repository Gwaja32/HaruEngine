package kr.haruserver.haruengine.mixin.entity;

import kr.haruserver.haruengine.util.EntityFilter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityCollisionMixin {

    /**
     * @author LeeGwangSu
     * @reason 근본적인 밀림 가능 여부 차단 (가장 먼저 실행됨)
     */
    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    private void haru$isPushable(CallbackInfoReturnable<Boolean> cir) {
        if (EntityFilter.isNoPhysicsTarget((Entity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    /**
     * @author LeeGwangSu
     * @reason 가축 9종이 다른 엔티티를 밀어내는 시도 차단 (AI 틱 등에서 호출)
     */
    @Inject(method = "pushAway", at = @At("HEAD"), cancellable = true)
    private void haru$stopPushingAway(Entity entity, CallbackInfo ci) {
        if (EntityFilter.isNoPhysicsTarget((Entity) (Object) this)) {
            ci.cancel();
        }
    }

    /**
     * @author LeeGwangSu
     * @reason 상대방이 밀려고 할 때 발생하는 연산 체인 절단
     */
    @Inject(method = "pushAwayFrom", at = @At("HEAD"), cancellable = true)
    private void haru$stopBeingPushed(Entity entity, CallbackInfo ci) {
        if (EntityFilter.isNoPhysicsTarget((Entity) (Object) this)) {
            ci.cancel();
        }
    }
}