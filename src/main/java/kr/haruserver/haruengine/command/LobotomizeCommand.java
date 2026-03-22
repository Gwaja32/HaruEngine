package kr.haruserver.haruengine.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;

import java.util.List;

public class LobotomizeCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("lobotomize")
                    .requires(CommandManager.requirePermissionLevel(CommandManager.ADMINS_CHECK))
                    .then(CommandManager.argument("range", IntegerArgumentType.integer(1, 500))
                            .executes(LobotomizeCommand::execute)));
        });
    }

    private static int execute(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        if (player == null) {
            source.sendError(Text.literal("이 명령어는 플레이어만 사용할 수 있습니다."));
            return 0;
        }

        int range = IntegerArgumentType.getInteger(context, "range");

        Box box = player.getBoundingBox().expand(range);
        List<VillagerEntity> villagers = player.getEntityWorld().getEntitiesByClass(VillagerEntity.class, box, villager -> true);

        int count = 0;
        Text customName = Text.literal("춘식이");

        for (VillagerEntity villager : villagers) {
            villager.setCustomName(customName);
            villager.setCustomNameVisible(true);
            count++;
        }

        // 성공 피드백 메시지
        int finalCount = count;
        source.sendFeedback(() -> Text.literal("[Haru] ")
                .formatted(Formatting.GOLD)
                .append(Text.literal(finalCount + "명의 주민의 이름이 '춘식이'로 변경되었습니다.")
                        .formatted(Formatting.WHITE)), false);

        return count;
    }
}
