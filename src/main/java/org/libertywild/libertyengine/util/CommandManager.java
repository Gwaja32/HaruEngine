package org.libertywild.libertyengine.util;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.PermissionLevel;

public class CommandManager {
    public static void registerCommand() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("libertyengine")
                    .requires(source -> {
                        // 1. libertyengine.use 권한 체크 (Boolean)
                        boolean hasNode = source.checkPermission(Identifier.parse("libertyengine.use"), false);

                        // 2. 관리자 권한 체크 (PermissionLevel이 ADMINS(3) 이상인지 확인)
                        // 소유자(OWNERS)나 관리자(ADMINS)는 별도 노드 없이도 사용 가능하도록 설정
                        boolean isAuthorized = source.getPermissionContext().permissionLevel().isEqualOrHigherThan(PermissionLevel.ADMINS);

                        return hasNode || isAuthorized;
                    }) // 관리자 권한
                    // 통합 콘피그 리로드
                    .then(Commands.literal("reload")
                            .executes(context -> {
                                ConfigManager.reload();
                                context.getSource().sendSuccess(() -> Component.literal("[LibertyEngine] Config가 리로드 되었습니다."), false);
                                return 1;
                            })
                    )
            );
        });
    }
}
