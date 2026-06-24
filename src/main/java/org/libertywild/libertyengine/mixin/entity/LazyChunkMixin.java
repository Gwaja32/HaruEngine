package org.libertywild.libertyengine.mixin.entity;

import org.libertywild.libertyengine.api.ChunkMapAccessor;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.hurtingprojectile.Fireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.WitherSkull;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class LazyChunkMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void cleanStaticWitherSkulls(CallbackInfo ci) {
        ServerLevel level = (ServerLevel) (Object) this;

        // 성능 최적화: 20틱 주기
        if (level.getGameTime() % 20 != 0) return;

        ChunkMap chunkMap = level.getChunkSource().chunkMap;

        if (chunkMap instanceof ChunkMapAccessor) {
            var visibleMap = ((ChunkMapAccessor) chunkMap).getVisibleChunkMap();

            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof WitherSkull || entity instanceof Fireball
                        || entity instanceof LargeFireball || entity instanceof SmallFireball) {
                    ChunkPos pos = entity.chunkPosition();
                    ChunkHolder holder = visibleMap.get(pos.pack());

                    // holder가 존재하고, 물리 연산이 정지된 상태(레이지 청크)라면 제거
                    if (holder != null && isLazyChunk(holder)) {
                        entity.discard();
                    }
                }
            }
        }
    }

    @Unique
    public boolean isLazyChunk(ChunkHolder holder) {
        // 1. 청크가 아예 로드되지 않은 상태라면 제외
        LevelChunk chunk = holder.getTickingChunk(); // 혹은 getLatestChunk()
        if (chunk == null) return false;

        // 2. 현재 상태 확인
        FullChunkStatus status = holder.getFullStatus();

        // 3. 레이지 청크 조건:
        // 엔티티는 존재함 (chunk != null)
        // BUT 물리 틱 연산 단계(ENTITY_TICKING)에는 도달하지 않음
        return status.ordinal() < FullChunkStatus.ENTITY_TICKING.ordinal();
    }
}