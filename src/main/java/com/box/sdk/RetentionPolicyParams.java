package com.box.sdk;

import java.util.ArrayList;
import java.util.List;

/**
 * Optional parameters for creating an updating a Retention Policy.
 *
 * @see BoxRetentionPolicy
 */
public class RetentionPolicyParams {

    /**
     * @see #getCanOwnerExtendRetention()
     */
    private boolean canOwnerExtendRetention;

    /**
     * @see #getAreOwnersNotified()
     */
    private boolean areOwnersNotified;

    /**
     * @see #getDescription()
     */
    private String description;

    /**
     * @see #getCustomNotificationRecipients()
     */
    private List<BoxUser.Info> customNotificationRecipients;

    /**
     * Creates optional retention policy params with default values.
     */
    public RetentionPolicyParams() {
        this.canOwnerExtendRetention = false;
        this.areOwnersNotified = false;
        this.customNotificationRecipients = new ArrayList<>();
        this.description = "";
    }

    /**
     * @return the flag denoting whether the owner can extend the retention.
     */
    public boolean getCanOwnerExtendRetention() {
        return this.canOwnerExtendRetention;
    }

    /**
     * Set the flag denoting whether the owner can extend the retentiion.
     *
     * @param canOwnerExtendRetention The flag value.
     */
    public void setCanOwnerExtendRetention(boolean canOwnerExtendRetention) {
        this.canOwnerExtendRetention = canOwnerExtendRetention;
    }

    /**
     * @return the flag denoting whether owners and co-onwers are notified when the retention period is ending.
     */
    public boolean getAreOwnersNotified() {
        return this.areOwnersNotified;
    }

    /**
     * Set the flag denoting whether owners and co-owners are notified when the retention period is ending.
     *
     * @param areOwnersNotified The flag value.
     */
    public void setAreOwnersNotified(boolean areOwnersNotified) {
        this.areOwnersNotified = areOwnersNotified;
    }

    /**
     * @return The additional text description of the retention policy
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Set additional text description of the retention policy.
     *
     * @param description The additional text description of the retention policy.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the list of extra users to notify when the retention period is ending.
     */
    public List<BoxUser.Info> getCustomNotificationRecipients() {
        return this.customNotificationRecipients;
    }

    /**
     * Set the list of extra users to notify when the retention period is ending.
     *
     * @param customNotificationRecipients The list of users.
     */
    public void setCustomNotificationRecipients(List<BoxUser.Info> customNotificationRecipients) {
        this.customNotificationRecipients = customNotificationRecipients;
    }

    /**
     * Add a user by ID to the list of people to notify when the retention period is ending.
     *
     * @param userID The ID of the user to add to the list.
     */
    public void addCustomNotificationRecipient(String userID) {
        BoxUser user = new BoxUser(null, userID);
        this.customNotificationRecipients.add(user.new Info());

    }

    /**
     * Add a user to the list of people to notify when the retention period is ending.
     *
     * @param user The info of the user to add to the list
     */
    public void addCustomNotificationRecipient(BoxUser user) {
        this.customNotificationRecipients.add(user.new Info());
    }
}

