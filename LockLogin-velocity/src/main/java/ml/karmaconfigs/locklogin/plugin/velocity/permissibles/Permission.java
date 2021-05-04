package ml.karmaconfigs.locklogin.plugin.velocity.permissibles;

import com.velocitypowered.api.proxy.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public final class Permission {

    private final Map<String, Boolean> nodes_child = new HashMap<>();
    private final Map<String, Boolean> nodes_parent = new HashMap<>();
    private String node;

    /**
     * Initialize the plugin permission
     *
     * @param name the permission name
     */
    public Permission(final String name) {
        node = name;
    }

    /**
     * Add parent permission to the permission
     *
     * @param subNode     the parent permission
     * @param inheritance if the user has the parent permission, automatically
     *                    add this one
     */
    public final void addNode(final @NotNull String subNode, final boolean inheritance, final boolean child) {
        if (child) {
            nodes_child.put(subNode, inheritance);
        } else {
            nodes_parent.put(subNode, inheritance);
        }
    }

    /**
     * Get the permission node
     *
     * @return the permission node name
     */
    public final String getName() {
        return node;
    }

    /**
     * Get if the specified permission is permissible
     *
     * @param player the player to check permission with
     * @param node   the permission to check
     * @return if the specified permission applies to this
     * one or vice-verse
     */
    public final boolean isPermissible(final Player player, final Permission node) {
        if (player.hasPermission(node.node))
            return true;

        for (String permission : nodes_child.keySet()) {
            if (player.hasPermission(permission)) {
                return true;
            }
        }

        return false;
    }
}
