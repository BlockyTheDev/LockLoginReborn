package ml.karmaconfigs.locklogin.plugin.bukkit.command.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface SystemCommand {

    String command() default "";

    boolean bungeecord() default false;

    /**
     * Get the plugin command manager
     */
    class manager {

        /**
         * Get the declared command of the
         * class
         *
         * @param clazz the command class
         * @return the command of the class
         */
        public static String getDeclaredCommand(final Class<?> clazz) {
            if (clazz.isAnnotationPresent(SystemCommand.class)) {
                SystemCommand cmd = clazz.getAnnotation(SystemCommand.class);

                try {
                    return cmd.command();
                } catch (Throwable ignored) {
                }
            }

            return "";
        }

        /**
         * Get the declared bungee status of the
         * class
         *
         * @param clazz the command class
         * @return the bungee status of the class
         */
        public static boolean getBungeeStatus(final Class<?> clazz) {
            if (clazz.isAnnotationPresent(SystemCommand.class)) {
                SystemCommand cmd = clazz.getAnnotation(SystemCommand.class);

                try {
                    return cmd.bungeecord();
                } catch (Throwable ignored) {
                }
            }

            return false;
        }
    }
}
