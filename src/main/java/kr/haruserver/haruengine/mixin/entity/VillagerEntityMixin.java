package kr.haruserver.haruengine.mixin.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.World;
import net.minecraft.util.math.GlobalPos;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.Optional;

@Mixin(VillagerEntity.class)
public abstract class VillagerEntityMixin extends MerchantEntity {

    @Shadow private long lastRestockTime;
    @Shadow private int restocksToday;
    @Shadow private long lastRestockCheckTime;
    @Shadow protected abstract void updateDemandBonus();
    @Shadow protected abstract void sendOffersToCustomer();

    @Unique private boolean lobotomy$needsRestockAfterClosing = false;
    @Unique private int lobotomy$aiActiveTicks = 0;
    @Unique private boolean lobotomy$isGossipProcessed = false; // 소문 처리 플래그

    public VillagerEntityMixin(EntityType<? extends MerchantEntity> entityType, World world) {
        super(entityType, world);
    }

    // [규칙 1 & 2] 공격받았을 때
    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        boolean damaged = super.damage(world, source, amount);
        if (damaged && source.getAttacker() instanceof PlayerEntity) {
            // 때렸을 때만 소문이 갱신되도록 플래그 초기화
            this.lobotomy$isGossipProcessed = false;
            this.lobotomy$aiActiveTicks = 40;
        }
        return damaged;
    }

    // [핵심] 거래창 열 때 소문 계산 제어
    @Inject(method = "prepareOffersFor", at = @At("HEAD"), cancellable = true)
    private void onPrepareOffersFor(PlayerEntity player, CallbackInfo ci) {
        // 이미 소문이 가격에 반영된 상태라면, 바닐라의 중복 계산 로직을 막음
        if (this.lobotomy$isGossipProcessed) {
            this.sendOffersToCustomer(); // 가격 갱신 없이 창만 표시
            ci.cancel();
            return;
        }
        // 처음 열 때는 바닐라 로직을 실행하게 두고, 플래그를 true로 변경
        this.lobotomy$isGossipProcessed = true;
    }

    @Inject(method = "afterUsing", at = @At("HEAD"))
    private void onAfterUsing(TradeOffer offer, CallbackInfo ci) {
        this.lobotomy$aiActiveTicks = 600;
    }

    @Override
    public void setCustomer(@Nullable PlayerEntity customer) {
        if (this.getCustomer() != null && customer == null) {
            if (this.lobotomy$hasAnySoldOutItem()) {
                this.lobotomy$needsRestockAfterClosing = true;
            }
        }
        super.setCustomer(customer);
    }

    @Inject(method = "updateDemandBonus", at = @At("HEAD"), cancellable = true)
    private void onUpdateDemandBonus(CallbackInfo ci) {
        if (this.lobotomy$needsRestockAfterClosing) {
            ci.cancel();
        }
    }

    @Inject(method = "restock", at = @At("TAIL"))
    private void afterRestock(CallbackInfo ci) {
        if (this.lobotomy$needsRestockAfterClosing) {
            for (TradeOffer offer : this.getOffers()) {
                try {
                    // 필드 캐싱 없이 리플렉션을 직접 쓸 경우 대비하여 Yarn/Intermediary 둘 다 대응
                    Field demandField;
                    try {
                        demandField = TradeOffer.class.getDeclaredField("demandBonus");
                    } catch (NoSuchFieldException e) {
                        demandField = TradeOffer.class.getDeclaredField("field_18668");
                    }
                    demandField.setAccessible(true);
                    demandField.setInt(offer, 0);
                } catch (Exception ignored) {}
            }
            this.lobotomy$needsRestockAfterClosing = false;
        }
    }

    @Unique
    private boolean lobotomy$hasAnySoldOutItem() {
        for (TradeOffer offer : this.getOffers()) {
            if (offer.isDisabled()) return true;
        }
        return false;
    }

    @Inject(method = "mobTick", at = @At("HEAD"), cancellable = true)
    private void handleMobTick(ServerWorld world, CallbackInfo ci) {
        if (this.lobotomy$aiActiveTicks > 0) this.lobotomy$aiActiveTicks--;
        this.lobotomy$originalDayResetLogic(world);

        VillagerEntity self = (VillagerEntity) (Object) this;
        boolean hasJob = !self.getVillagerData().profession().matchesKey(VillagerProfession.NONE);
        boolean isChunSik = this.hasCustomName() && "춘식이".equals(this.getCustomName().getString());

        // 춘식이거나 아기 주민이면 AI 제한을 건너뜀
        if (isChunSik) return;

        if (hasJob) {
            boolean hasExperience = self.getExperience() > 0;
            if (!hasExperience) {
                Optional<GlobalPos> jobSite = self.getBrain().getOptionalRegisteredMemory(MemoryModuleType.JOB_SITE);
                if (jobSite.isPresent()) {
                    GlobalPos pos = jobSite.get();
                    if (world.getRegistryKey() != pos.dimension() || !world.getPointOfInterestStorage().test(pos.pos(), (poiType) -> true)) {
                        self.getBrain().forget(MemoryModuleType.JOB_SITE);
                        return;
                    }
                } else {
                    return;
                }
            }

            if (this.hasCustomer() || this.lobotomy$aiActiveTicks > 0) return;

            ci.cancel();
        }
        else {
            int chunkX = this.getBlockPos().getX() >> 4;
            int chunkZ = this.getBlockPos().getZ() >> 4;

            if (!this.lobotomy$isPlayerInNearbyChunks(world, chunkX, chunkZ, 1)) {
                ci.cancel();
            }
        }
    }

    /**
     * [Spark/WorldThreader 최적화] 인접 1청크(3x3) 내 플레이어 존재 여부 확인
     * @param radius 1이면 상하좌우 및 대각선 1칸 포함 (총 3x3 영역)
     */
    @Unique
    private boolean lobotomy$isPlayerInNearbyChunks(ServerWorld world, int entityX, int entityZ, int radius) {
        // WorldThreader 환경에서 안전하게 플레이어 리스트 순회
        for (ServerPlayerEntity player : world.getPlayers()) {
            int playerX = player.getBlockX() >> 4;
            int playerZ = player.getBlockZ() >> 4;

            // 정수 절대값 계산으로 3x3 영역 판정 (매우 빠름)
            if (Math.abs(playerX - entityX) <= radius && Math.abs(playerZ - entityZ) <= radius) {
                return true;
            }
        }
        return false;
    }

    @Unique
    private void lobotomy$originalDayResetLogic(ServerWorld world) {
        long currentTime = world.getTime();
        long currentDay = world.getTimeOfDay() / 24000L;
        if (currentTime > this.lastRestockTime + 12000L || (this.lastRestockCheckTime > 0L && currentDay > this.lastRestockCheckTime)) {
            this.lastRestockCheckTime = currentDay;
            this.lastRestockTime = currentTime;
            this.lobotomy$clearDailyRestockCount();
            this.lobotomy$needsRestockAfterClosing = false;
            // 날짜가 지나면 소문이 자연 감소했을 수 있으므로 플래그 초기화
            this.lobotomy$isGossipProcessed = false;
        }
    }

    @Unique
    private void lobotomy$clearDailyRestockCount() {
        int availableSlots = 2 - this.restocksToday;
        if (availableSlots > 0) {
            for (TradeOffer tradeOffer : this.getOffers()) tradeOffer.resetUses();
        }
        for (int j = 0; j < availableSlots; ++j) this.updateDemandBonus();
        this.sendOffersToCustomer();
        this.restocksToday = 0;
    }
}
