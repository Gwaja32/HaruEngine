package kr.haruserver.haruengine.mixin.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.hurtingprojectile.WitherSkull;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.Map;
import java.util.WeakHashMap;

@Mixin(ServerLevel.class)
public abstract class WitherSkullMixin {
    // 각 위더 머리의 위치를 기억하기 위한 맵
    private static final Map<Entity, Vec3> LAST_POSITIONS = new WeakHashMap<>();

    @Inject(method = "tick", at = @At("TAIL"))
    private void cleanStaticWitherSkulls(CallbackInfo ci) {
        ServerLevel level = (ServerLevel) (Object) this;

        // 성능을 위해 20틱(1초)마다 실행
        if (level.getGameTime() % 20 == 0) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof WitherSkull projectile) {
                    Vec3 currentPos = projectile.position();
                    Vec3 lastPos = LAST_POSITIONS.get(projectile);

                    // 1초 전 좌표와 지금 좌표가 같으면 박제된 것으로 간주
                    if (lastPos != null && lastPos.equals(currentPos)) {
                        // 가장 확실한 죽음 처리
                        projectile.discard();
                        projectile.setRemoved(Entity.RemovalReason.DISCARDED);
                        level.removeBlockEntity(projectile.blockPosition());

                        LAST_POSITIONS.remove(projectile);
                    } else {
                        // 움직이고 있다면 좌표 갱신
                        LAST_POSITIONS.put(projectile, currentPos);
                    }
                }
            }
        }
    }
}