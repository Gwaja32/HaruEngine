package org.libertywild.libertyengine.mixin.entity;

import org.libertywild.libertyengine.util.EntityFilter;
import net.minecraft.world.entity.Entity;
import org.libertywild.libertyengine.util.LibertyEntityAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Entity.class)
public abstract class EntityLibertyDataMixin implements LibertyEntityAccessor {

    // -1은 아직 계산되지 않았음을 의미하는 'Sentinel Value'입니다.
    @Unique
    private byte liberty$filterMask = -1;

    @Override
    public byte liberty$getFilterMask() {
        // [수정] 한 번 계산되면 그 이후로는 즉시 변수 값만 리턴합니다.
        if (this.liberty$filterMask == -1) {
            this.liberty$filterMask = EntityFilter.calculateMask(((Entity)(Object)this).getType());
        }
        return this.liberty$filterMask;
    }
}
