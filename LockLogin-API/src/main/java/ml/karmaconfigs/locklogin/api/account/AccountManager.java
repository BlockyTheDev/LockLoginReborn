package ml.karmaconfigs.locklogin.api.account;

import java.io.Serializable;
import java.time.Instant;
import java.util.Set;

/**
 * LockLogin account manager
 * <p>
 * MUST HAVE AN EMPTY CONSTRUCTOR
 */
public abstract class AccountManager implements Serializable {

    /**
     * Check if the file exists
     *
     * @return if the file exists
     */
    public abstract boolean exists();

    /**
     * Tries to create the account
     *
     * @return if the account could be created
     */
    public abstract boolean create();

    /**
     * Tries to remove the account
     *
     * @return if the account could be removed
     */
    public abstract boolean remove();

    /**
     * Save the account id
     *
     * @param id the account id
     */
    public abstract void saveUUID(final AccountID id);

    /**
     * Save the account 2FA status
     *
     * @param status the account 2FA status
     */
    public abstract void set2FA(final boolean status);

    /**
     * Get the account id
     *
     * @return the account id
     */
    public abstract AccountID getUUID();

    /**
     * Get the account name
     *
     * @return the account name
     */
    public abstract String getName();

    /**
     * Save the account name
     *
     * @param name the account name
     */
    public abstract void setName(final String name);

    /**
     * Get the account password
     *
     * @return the account password
     */
    public abstract String getPassword();

    /**
     * Save the account password
     *
     * @param password the account password
     */
    public abstract void setPassword(final String password);

    /**
     * Get the account google auth token
     *
     * @return the account google auth
     * token
     */
    public abstract String getGAuth();

    /**
     * Save the account google auth token
     *
     * @param token the account google auth
     *              token
     */
    public abstract void setGAuth(final String token);

    /**
     * Get the account pin
     *
     * @return the account pin
     */
    public abstract String getPin();

    /**
     * Save the account pin
     *
     * @param pin the account pin
     */
    public abstract void setPin(final String pin);

    /**
     * Check if the account has 2FA
     *
     * @return if the account has 2FA
     */
    public abstract boolean has2FA();

    /**
     * Get the account creation time
     *
     * @return the account created time
     */
    public abstract Instant getCreationTime();

    /**
     * Get a list of accounts
     *
     * @return a list of all the
     * available accounts
     */
    public abstract Set<AccountManager> getAccounts();
}
