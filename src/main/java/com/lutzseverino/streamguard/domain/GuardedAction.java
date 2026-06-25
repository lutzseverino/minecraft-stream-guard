package com.lutzseverino.streamguard.domain;

/**
 * World-affecting actions StreamGuard can protect.
 */
public enum GuardedAction {
    BLOCK_BREAK("block-break"),
    BLOCK_PLACE("block-place"),
    BLOCK_INTERACT("block-interact"),
    CONTAINER_OPEN("container-open"),
    ITEM_PICKUP("item-pickup"),
    ITEM_DROP("item-drop"),
    INVENTORY_CLICK("inventory-click"),
    CRAFTING("crafting"),
    VILLAGER_TRADING("villager-trading"),
    ENTITY_DAMAGE("entity-damage"),
    ENTITY_INTERACT("entity-interact"),
    BUCKETS("buckets"),
    FIRE("fire");

    private final String configKey;

    GuardedAction(String configKey) {
        this.configKey = configKey;
    }

    public String configKey() {
        return configKey;
    }
}
