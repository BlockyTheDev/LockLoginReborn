package eu.locklogin.api.common.session;

/*
 * GNU LESSER GENERAL PUBLIC LICENSE
 * Version 2.1, February 1999
 * <p>
 * Copyright (C) 1991, 1999 Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 * Everyone is permitted to copy and distribute verbatim copies
 * of this license document, but changing it is not allowed.
 * <p>
 * [This is the first released version of the Lesser GPL.  It also counts
 * as the successor of the GNU Library Public License, version 2, hence
 * the version number 2.1.]
 */

import eu.locklogin.api.account.AccountID;
import eu.locklogin.api.account.AccountManager;
import eu.locklogin.api.util.enums.Manager;
import eu.locklogin.api.util.platform.CurrentPlatform;
import ml.karmaconfigs.api.common.karmafile.KarmaFile;
import ml.karmaconfigs.api.common.utils.file.FileUtilities;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * LockLogin persistent session data
 */
public final class PersistentSessionData {

    private final static File folder = new File(FileUtilities.getProjectFolder("plugins") + File.separator + "LockLogin", "data");
    private final static File file = new File(folder, "sessions.lldb");

    private final static KarmaFile sessions = new KarmaFile(file);

    private final AccountID id;

    /**
     * Initialize the persistent session data
     *
     * @param uuid the account id
     */
    public PersistentSessionData(final AccountID uuid) {
        id = uuid;
    }

    /**
     * Get a list of persistent accounts
     *
     * @return a list of persistent accounts
     */
    public static Set<AccountManager> getPersistentAccounts() {
        Set<AccountManager> accounts = new LinkedHashSet<>();
        AccountManager tmp_manager = CurrentPlatform.getAccountManager(Manager.CUSTOM, null);

        if (tmp_manager != null) {
            Set<AccountManager> tmp_accounts = tmp_manager.getAccounts();
            List<String> ids = sessions.getStringList("PERSISTENT");
            for (AccountManager manager : tmp_accounts) {
                if (manager != null) {
                    if (ids.contains(manager.getUUID().getId()))
                        accounts.add(manager);
                }
            }
        }

        return accounts;
    }

    /**
     * Toggle the user persistent session
     * status
     *
     * @return the user persistent session
     * status
     */
    public boolean toggleSession() {
        List<String> ids = sessions.getStringList("PERSISTENT");

        boolean result;
        if (ids.contains(id.getId())) {
            ids.remove(id.getId());
            result = false;
        } else {
            ids.add(id.getId());
            result = true;
        }

        sessions.set("PERSISTENT", ids);
        return result;
    }

    /**
     * Toggle the account panic mode
     *
     * @return the account panic mode
     */
    public boolean togglePanic() {
        List<String> ids = sessions.getStringList("PANIC");

        boolean result;
        if (ids.contains(id.getId())) {
            ids.remove(id.getId());
            result = false;
        } else {
            ids.add(id.getId());
            result = true;
        }

        sessions.set("PANIC", ids);
        return result;
    }

    /**
     * Get if the user account is persistent or not
     *
     * @return if the user account is persistent
     */
    public boolean isPersistent() {
        List<String> ids = sessions.getStringList("PERSISTENT");
        return ids.contains(id.getId());
    }

    /**
     * Get if the user account is panicking
     *
     * @return if the user account is panicking
     */
    public boolean isPanicking() {
        List<String> ids = sessions.getStringList("PANIC");
        return ids.contains(id.getId());
    }
}
