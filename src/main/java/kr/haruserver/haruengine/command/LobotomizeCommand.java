package kr.haruserver.haruengine.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.AABB;
import java.util.List;

public class LobotomizeCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("lobotomize")
                    .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                    .then(Commands.argument("range", IntegerArgumentType.integer(1, 500))
                            .executes(LobotomizeCommand::execute)));
        });
    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();

        if (player == null) {
            source.sendFailure(Component.literal("이 명령어는 플레이어만 사용할 수 있습니다."));
            return 0;
        }

        int range = IntegerArgumentType.getInteger(context, "range");

        AABB box = player.getBoundingBox().inflate(range);
        List<Villager> villagers = player.level().getEntitiesOfClass(Villager.class, box, villager -> true);

        int count = 0;
        Component customName = Component.literal("춘식이");

        for (Villager villager : villagers) {
            villager.setCustomName(customName);
            villager.setCustomNameVisible(true);
            count++;
        }

        // 성공 피드백 메시지
        int finalCount = count;
        source.sendSuccess(() -> Component.literal("[Haru] ")
                .withStyle(ChatFormatting.GOLD)
                .append(Component.literal(finalCount + "명의 주민의 이름이 '춘식이'로 변경되었습니다.")
                        .withStyle(ChatFormatting.WHITE)), false);

        return count;
    }
}
