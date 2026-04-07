package kr.haruserver.haruengine.mixin.entity;

import kr.haruserver.haruengine.util.EntityFilter;
import kr.haruserver.haruengine.util.HaruEntityAccessor;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Entity.class)
public abstract class EntityHaruDataMixin implements HaruEntityAccessor {

    // -1은 아직 계산되지 않았음을 의미하는 'Sentinel Value'입니다.
    @Unique
    private byte haru$filterMask = -1;

    @Override
    public byte haru$getFilterMask() {
        // [수정] 한 번 계산되면 그 이후로는 즉시 변수 값만 리턴합니다.
        if (this.haru$filterMask == -1) {
            this.haru$filterMask = EntityFilter.calculateMask(((Entity)(Object)this).getType());
        }
        return this.haru$filterMask;
    }
}
