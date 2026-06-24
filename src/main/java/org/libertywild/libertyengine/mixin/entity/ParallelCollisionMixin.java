package org.libertywild.libertyengine.mixin.entity;

import org.libertywild.libertyengine.util.EntityFilter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(Entity.class)
public abstract class ParallelCollisionMixin {

    @Invoker("collideWithShapes")
    public static Vec3 invokeVanillaAdjust(Vec3 movement, AABB entityBoundingBox, List<VoxelShape> collisions) {
        throw new AssertionError();
    }

    /**
     * @author LeeGwangSu
     * @reason 가축 9종이 다른 엔티티에 의해 밀리지 않도록 설정 (isPushable)
     */
    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    private void liberty$disablePushable(CallbackInfoReturnable<Boolean> cir) {
        // [최적화] 이제 비트마스크를 통해 CPU 사이클 소모 없이 즉시 판단합니다.
        if (EntityFilter.isNoPhysicsTarget((Entity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    /**
     * @author LeeGwangSu
     * @reason 가축 9종이 다른 엔티티와 충돌 판정을 갖지 않도록 설정 (isCollidable)
     */
    @Inject(method = "canBeCollidedWith", at = @At("HEAD"), cancellable = true)
    private void liberty$disableCollidable(CallbackInfoReturnable<Boolean> cir) {
        if (EntityFilter.isNoPhysicsTarget((Entity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    /**
     * @author LeeGwangSu
     * @reason [핵심 수정] 병렬 엔진을 제거하고 물리 연산 부하 자체를 관리
     */
    @Overwrite
    public static Vec3 collideBoundingBox(Entity entity, Vec3 movement, AABB entityBoundingBox, Level world, List<VoxelShape> collisions) {
        // 1. [병목 해결] 병렬 처리를 위해 CompletableFuture를 생성하던 모든 과정을 삭제했습니다.
        // 2. [물리 최적화] 만약 충돌 체크 대상(VoxelShape)이 너무 많으면(예: 128개 이상),
        // 이는 보통 복잡한 지형이나 겹친 엔티티 때문입니다.
        // 병렬화를 돌리는 대신, 바닐라 연산을 직접 수행하는 것이 현재는 더 빠릅니다.

        return invokeVanillaAdjust(movement, entityBoundingBox, collisions);
    }
}