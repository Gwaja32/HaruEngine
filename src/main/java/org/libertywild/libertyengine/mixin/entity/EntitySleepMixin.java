package org.libertywild.libertyengine.mixin.entity;

import org.libertywild.libertyengine.util.EntityFilter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class EntitySleepMixin {
    /**
     * @author LeeGwangSu
     * @reason 플레이어가 인접 1청크(3x3) 내에 없을 경우 12종 몹의 AI 비활성화
     */
    @Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
    private void skipAiIfNoPlayerInRange(CallbackInfo ci) {
        Mob entity = (Mob) (Object) this;

        boolean isChunSik = entity.hasCustomName() && "춘식이".equals(entity.getCustomName().getString());
        if (isChunSik) return;

        // 1. 비트마스크 기반 최적화 대상 확인 (가장 빠름)
        if (EntityFilter.isOptimizationTarget(entity)) {
            // 서버 월드인지 확인 (1.21.11 대응)
            if (!entity.level().isClientSide() && entity.level() instanceof ServerLevel serverWorld) {
                BlockPos pos = entity.blockPosition();
                int entityChunkX = pos.getX() >> 4;
                int entityChunkZ = pos.getZ() >> 4;

                // 2. 인접 1청크(3x3) 내 플레이어 존재 여부 확인
                // radius 1은 [상,하,좌,우,대각선]을 모두 포함하는 3x3 영역입니다.
                if (!this.liberty$isPlayerInNearbyChunks(serverWorld, entityChunkX, entityChunkZ, 1)) {
                    ci.cancel();
                }
            }
        }
    }
    /**
     * [최적화 루프] 플레이어 리스트를 순회하며 3x3 범위 내에 있는지 판별
     */
    @Unique
    private boolean liberty$isPlayerInNearbyChunks(ServerLevel world, int entityX, int entityZ, int radius) {
        // WorldThreader 환경에서도 안전한 리스트 순회 방식
        for (ServerPlayer player : world.players()) {
            int playerX = player.getBlockX() >> 4;
            int playerZ = player.getBlockZ() >> 4;

            // Math.abs 결과가 1 이하이면 인접한 8개의 청크 또는 자기 자신 청크에 있다는 뜻입니다.
            if (Math.abs(playerX - entityX) <= radius && Math.abs(playerZ - entityZ) <= radius) {
                return true;
            }
        }
        return false;
    }
}